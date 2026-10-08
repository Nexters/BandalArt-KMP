# 마감일 알림 정책 가이드

- 기준 commit: `53664a60` (Android 2.5.2, iOS 1.4.2)
- 관련 이슈: [#211 목표 마감일 기반 로컬 알림 도입](https://github.com/Nexters/BandalArt-KMP/issues/211)
- 도입 근거: [마감일 기반 로컬 알림 조사](LOCAL_DEADLINE_NOTIFICATION_RESEARCH.md), [구현 전략](LOCAL_DEADLINE_NOTIFICATION_STRATEGY.md)

이 문서는 현재 코드가 실제로 적용하는 마감일 알림 규칙을 정리한다. 알림 동작을 바꾸는 PR은 이 문서를 함께 갱신한다.

## 요약

| 항목 | 현재 정책 | 근거 코드 |
|---|---|---|
| 알림 시각 | 마감일 당일 **오전 9시 고정**. 사용자가 바꿀 수 없다 | `DeadlineReminderPlanner`, `AndroidDeadlineReminderScheduler`, `DeadlineReminderWorker`, `IosDeadlineReminderScheduler` |
| 시간대 | **기기 현지 시간대**. 서울 고정이 아니다 | `TimeZone.currentSystemDefault()`, `NSTimeZone.localTimeZone()` |
| 대상 | 미완료이고 제목과 마감일이 있는 모든 셀(메인·서브·태스크) | `DeadlineReminderPlanner.plan` |
| 묶음 단위 | 같은 반다라트 + 같은 마감일 = 알림 1개 | `DeadlineReminderBatch.id` |
| 예약 상한 | 가까운 날짜순 **32개 묶음**까지. 나머지는 예약하지 않는다 | `MAX_SCHEDULED_DEADLINE_REMINDER_BATCH_COUNT` |
| 켜기 조건 | 앱 설정 ON + OS 알림 권한 허용 | `DefaultDeadlineReminderReconciler` |
| 예약 방식 | 원본(Room·설정·권한)에서 전체 예약을 다시 계산해 교체 | `reconcileAll()`, `replaceAll()` |
| 알림 탭 | 해당 반다라트를 선택한 홈으로 이동. 셀 편집창은 열지 않는다 | `DeadlineNotificationLaunchTarget` |

## 1. 알림 대상

`DeadlineReminderPlanner`가 Room의 모든 셀(`getAllCells()`)을 후보로 읽고 아래 조건을 모두 만족하는 셀만 남긴다.

1. `isCompleted == false`
2. 앞뒤 공백을 제거한 제목이 비어 있지 않다.
3. `dueDate`가 `LocalDateTime` 형식으로 파싱된다. 파싱에 실패하면 조용히 제외한다.
4. 마감일 오전 9시가 현재 시각(기기 시간대)보다 뒤에 있다. 오늘 9시가 이미 지났으면 오늘 마감 셀도 제외한다.

메인·서브·태스크 셀을 구분하지 않는다. 마감일이 있는 서브목표와 그 아래 태스크가 같은 날 마감이면 같은 알림에 함께 집계된다.

## 2. 알림 시각과 시간대

- 알림 시각은 마감일 당일 `09:00`이며 코드 상수다. 설정 화면이나 저장소 값으로 바꿀 수 없다.
- 마감일은 시간대 정보가 없는 `LocalDateTime` 문자열로 저장되고, 알림 계산에서는 날짜만 사용한다.
- 9시는 알림을 계산하는 시점의 기기 현지 시간대로 해석한다.
  - 공통 planner: `DeadlineReminderTimeZoneProvider(TimeZone::currentSystemDefault)`
  - Android: `LocalDateTime(dueDate, 09:00).toInstant(TimeZone.currentSystemDefault())`로 절대 시각을 계산해 WorkManager 지연 시간으로 쓴다.
  - iOS: `NSTimeZone.localTimeZone()`(자동 갱신 프록시)을 지정한 `NSDateComponents`로 `UNCalendarNotificationTrigger`를 만든다.
- 시간대가 바뀌면 다시 예약한다.
  - Android: `DeadlineReminderTimeChangeReceiver`가 `TIME_SET`, `TIMEZONE_CHANGED`를 받아 `DeadlineReminderReconcileWorker`로 전체 재조정한다.
  - iOS: `NSSystemTimeZoneDidChange`, `applicationSignificantTimeChange`에서 재조정한다.
- 결과적으로 서울에서 예약한 뒤 뉴욕으로 이동하면 뉴욕 기준 마감일 오전 9시에 알림이 온다.

## 3. 묶음과 상한

- 묶음 ID: `deadline.v1.board.{bandalartId}.date.{yyyy-MM-dd}`
- 같은 반다라트·같은 날짜의 셀은 셀 ID 순으로 정렬해 알림 1개로 합친다.
- 묶음은 마감일, 반다라트 ID 순으로 정렬하고 앞에서 32개만 예약한다. 넘치는 개수는 `overflowCount`로 상태에만 남고 사용자에게 따로 알리지 않는다.
- 32개 상한은 iOS pending 알림 수에 공식 문서로 공개되지 않은 상한이 있다는 점을 고려해 보수적으로 잡은 값이다. 도입 시 결정 근거는 [조사 문서](LOCAL_DEADLINE_NOTIFICATION_RESEARCH.md)에 있다.

## 4. 켜기·끄기와 권한

알림은 **앱 설정 ON**과 **OS 권한 허용**이 모두 만족할 때만 예약된다. 둘은 별도 상태다.

| 권한 상태 | Android | iOS | 예약 |
|---|---|---|---|
| `GRANTED` | 권한 허용 + 앱 알림 ON + 채널 중요도가 `NONE`이 아님 | `authorized` + 배너 허용 | 예약 |
| `QUIET` | 해당 없음 | `provisional`, `ephemeral`, 또는 `authorized`지만 배너 꺼짐 | 예약 |
| `REQUESTABLE` | Android 13+에서 아직 거부 이력이 없음 | `notDetermined` | 전부 취소 |
| `BLOCKED` | 거부 이력 있음, 앱 알림 OFF, 채널 OFF | `denied` | 전부 취소 |

- 권한은 사용자가 설정에서 알림을 켤 때만 요청한다. 앱 시작 시 먼저 묻지 않는다.
- `BLOCKED`면 OS 설정 화면으로 이동시킨다. Android는 앱 알림 설정, iOS는 앱 알림 설정 URL을 연다.
- 앱 설정 OFF면 예약과 이미 표시된 마감 알림을 모두 지운다.
- Android Worker는 발송 직전에 설정 ON과 `GRANTED`를 다시 확인한다. iOS는 OS가 직접 발송하므로 발송 시점 재확인이 없다.

## 5. 예약 재조정 시점

예약은 변경된 셀만 고치지 않는다. `reconcileAll()`이 원본을 다시 읽어 원하는 전체 예약을 계산하고, 기존 마감 알림 예약을 모두 지운 뒤 다시 만든다. 동시 실행은 `Mutex`로 직렬화한다.

| 시점 | 위치 |
|---|---|
| 반다라트·셀 생성, 수정, 삭제, 완료 토글, 완료 초기화 | `DefaultBandalartRepository` |
| 알림 설정 ON/OFF, 권한 요청 결과, 홈 foreground 복귀 | `HomePresenter` |
| 클라우드 백업 복원 후 | `CloudBackupPresenter` |
| Android 사용자 앱 실행 | `MainActivity.onCreate` → application scope |
| iOS 앱 실행, active 진입, 시간대·유의미한 시간 변경 | `iosApp.swift` → `DeadlineReminderLifecycleBridge` |
| Android 시간·시간대 변경 | `DeadlineReminderTimeChangeReceiver` |
| iOS 위젯 런타임 갱신 | `IosWidgetRuntimeBridge` |

## 6. 플랫폼별 발송

### Android

- 묶음마다 `OneTimeWorkRequest`를 `ExistingWorkPolicy.REPLACE`로 예약한다. 태그는 `deadline.v1`과 묶음 ID다.
- WorkManager는 정확한 시각을 보장하지 않는다. 9시 이후 시스템이 허용하는 시점에 실행될 수 있다. exact alarm은 쓰지 않는다.
- Worker는 실행 시점에 다시 검사하고, 조건이 맞지 않으면 알림 없이 성공 처리한다.
  - 설정 ON, `GRANTED`
  - 오늘이 마감일이고 현재 9시 이후
  - 해당 반다라트의 그 날짜 미완료 셀이 남아 있음
- 채널 `deadline_reminder_v2`, 중요도 `IMPORTANCE_HIGH`

### iOS

- 묶음마다 `UNCalendarNotificationTrigger` 1회성 요청을 만든다. 식별자는 묶음 ID다.
- OS가 정해진 시각에 직접 발송하므로 발송 직전의 완료 여부를 다시 확인하지 않는다. 대신 셀이 바뀔 때마다 재조정해 예약을 최신으로 유지한다.
- 기본 알림음을 사용한다.

## 7. 알림 내용

| 경우 | 제목 | 본문 (ko) |
|---|---|---|
| 셀 1개 | 오늘 마감인 목표가 있어요 | `{제목}`을(를) 완료할 시간이에요. |
| 셀 여러 개 | 오늘 마감인 목표가 있어요 | `{개수}`개의 목표가 오늘 마감이에요. |
| 테스트 | 마감일 알림 테스트 | 이 알림이 보이면 알림 설정이 정상이에요. |

문구는 Android `composeApp/src/androidMain/res/values*/strings.xml`과 iOS `iosApp/iosApp/*.lproj/Localizable.strings`의 `deadline_reminder_*` 키에 있다(ko/en/ja).

## 8. 알림 탭

- Android: `ACTION_OPEN_DEADLINE`, data URI `bandalart://deadline/{batchId}`, extra `deadline_bandalart_id`로 앱을 연다.
- iOS: `userInfo["deadline_bandalart_id"]`를 `DeadlineNotificationLaunchBridge`에 기록한다.
- 두 플랫폼 모두 `BufferedDeadlineNotificationLaunchTarget`에 반다라트 ID를 보관한다. Splash를 지나 홈이 준비되면 해당 반다라트를 선택하고 `acknowledge`로 비운다.

## 9. 테스트 알림

설정의 테스트 버튼은 즉시(iOS 1초 뒤) 알림 1개를 보낸다. 마감 알림 예약에는 영향을 주지 않고, 다음 재조정 때 함께 지워진다.

## 10. 현재 제약과 후속 과제

| 제약 | 영향 | 후속 |
|---|---|---|
| 알림 시각 9시 고정 | 아침 9시가 맞지 않는 사용자가 바꿀 수 없다 | 알림 시간 선택 기능 |
| 9시 값이 4곳에 중복 | 시각을 바꾸려면 planner, Android scheduler, Worker, iOS 상수를 함께 고쳐야 한다 | 설정값 하나로 통합 |
| 마감일에 시간대 정보 없음 | 마감일 자체는 어느 나라에서든 같은 날짜로 취급된다. 이동 후에는 새 현지 날짜 기준으로 알림이 온다 | 시간대 정책을 제품 관점에서 확정 |
| Android 발송 지연 가능 | Doze 등으로 9시 정각보다 늦을 수 있다 | 정확도 요구 시 exact alarm 검토 |
| 32개 초과 묶음 미예약 | 먼 날짜 알림은 가까운 묶음이 지나갈 때까지 예약되지 않는다 | 재조정 시점마다 다시 채워지므로 현재는 유지 |
| iOS 발송 직전 재확인 없음 | 앱이 재조정하지 못한 채 완료된 셀이 알림에 남을 수 있다 | 셀 변경 시 재조정으로 대부분 방지 |
