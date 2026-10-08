# 마감일 알림 시간 선택·시간대 대응 전략

- 작성일: 2026-10-08
- 관련 이슈: [#429 마감일 알림 시간 선택 및 시간대 대응](https://github.com/Nexters/BandalArt-KMP/issues/429)
- 기준 commit: `ae40d40e`
- 현재 정책: [마감일 알림 정책 가이드](DEADLINE_NOTIFICATION_POLICY_GUIDE.md)

## 결정

| 항목 | 결정 |
|---|---|
| 선택 단위 | 시·분 자유 선택 (00:00~23:59) |
| 기본값 | 09:00. 기존 사용자는 아무것도 하지 않아도 지금과 같다 |
| 저장 형식 | 자정 기준 분(`Int`, 0~1439). 09:00 = 540 |
| 시간대 | 기기 현지 시각 기준 유지. 마감일 저장 형식은 바꾸지 않는다 |
| UI 위치 | 설정 시트의 마감일 알림 스위치 아래. 스위치가 켜져 있을 때만 "알림 시간" 행 표시 |
| 선택 UI | 공통 확인 다이얼로그 안에 플랫폼별 피커. Android는 Material3 `TimePicker`, iOS는 compose-hig `CupertinoTimePicker`(휠) |
| 시각 표기 | 언어별 12시간제(오전 9:00 / 9:00 AM / 午前9:00). kotlinx-datetime이 로케일 포맷을 제공하지 않으므로 기존 날짜 표기처럼 언어별로 직접 만든다 |
| Android 정확도 | WorkManager 유지. exact alarm은 도입하지 않는다 |
| 백업 | 클라우드 백업에 알림 시각 포함. 스키마 버전은 올리지 않는다 |

## 현재 구조와 바꿀 곳

9시가 네 곳에 상수로 있다. 모두 저장된 값 하나를 쓰도록 바꾼다.

| 위치 | 현재 | 변경 |
|---|---|---|
| `DeadlineReminderPlanner` | `LocalTime(hour = 9, minute = 0)`로 지난 알림 제외 | 설정 시각을 입력으로 받아 계산하고 결과(`DeadlineReminderPlan`)에 시각을 담는다 |
| `AndroidDeadlineReminderScheduler` | `LocalTime(9, 0)`으로 지연 시간 계산 | plan의 시각 사용 |
| `DeadlineReminderWorker` | `now.hour < 9`면 발송 안 함 | 실행 시점에 설정 시각을 다시 읽어 비교 |
| `IosDeadlineReminderScheduler` | `DEADLINE_REMINDER_HOUR = 9L` | plan의 시·분으로 `NSDateComponents` 구성 |

## 단계

### 1. 설정 저장

- `BandalartDataStore`: `deadline_reminder_minute_of_day` int 키, 읽기 Flow(기본 540)와 setter. 범위 밖 값은 540으로 취급
- `SettingsRepository`: `deadlineReminderMinuteOfDay: Flow<Int>`, `setDeadlineReminderMinuteOfDay(Int)`
- 백업
  - `BackupPreferences`, `BandalartBackupPreferences`에 `deadlineReminderMinuteOfDay: Int = 540` 추가
  - 기존 백업에는 값이 없으므로 기본값으로 복원된다
  - 기존 앱은 `ignoreUnknownKeys`로 새 필드를 무시하므로 `CURRENT_SCHEMA_VERSION`은 2로 유지한다. 올리면 구버전 앱이 새 백업 복원을 거부한다

### 2. 도메인 계산

- 시각 표현은 `LocalTime`으로 통일하고 저장값 변환은 한 곳(`DeadlineReminderTime` 등)에서만 한다
- `DeadlineReminderPlanner.plan(candidates, isEnabled, reminderTime)`
  - "마감일 + 설정 시각"이 현재보다 뒤인 셀만 남긴다
  - `DeadlineReminderPlan`에 `reminderTime` 포함
- `DeadlineReminderScheduler.replaceAll(batches, reminderTime)`로 플랫폼에 시각 전달
- `DefaultDeadlineReminderReconciler`가 설정 시각을 읽어 planner와 scheduler에 넘긴다
- 묶음 ID(`deadline.v1.board.{id}.date.{date}`)는 유지한다. 시각이 바뀌면 재조정이 전체 예약을 교체하므로 ID에 시각을 넣을 필요가 없다

### 3. 플랫폼

- Android scheduler: `LocalDateTime(dueDate, reminderTime).toInstant(currentSystemDefault())`
- Android Worker: 오늘이 마감일이고 현재 시각이 설정 시각 이상일 때만 발송
- iOS scheduler: `hour`, `minute`를 설정 시각으로 채운다. 자동 갱신 시간대 프록시는 유지한다

### 4. UI

- `HomeScreen.Event.SetDeadlineReminderTime(minuteOfDay)` → 저장 후 `reconcileAll()`
- 설정 시트: 스위치 ON일 때 "알림 시간 · 오전 9:00" 행. 누르면 시간 선택 다이얼로그, 완료 시 이벤트 전송
- 다이얼로그 틀은 공통 `BandalartActionAlertDialog`, 피커만 `expect`/`actual`
  - Android: Material3 `TimePicker`
  - iOS: compose-hig `CupertinoTimePicker`. iOS 앱에서 Material 시계 다이얼 대신 iOS와 같은 휠 형태를 쓰기 위해서다
- compose-hig는 `1.0.2-alpha17`을 iOS source set에만 추가한다. 이후 버전은 Kotlin 2.4.20·Compose Multiplatform 1.11 이상을 요구해 현재 CMP 1.10.3과 맞지 않는다. CMP를 올릴 때 함께 올린다
- 알파 버전이므로 문제가 생기면 시스템 `UIDatePicker`(시간 모드, 휠)를 `UIKitView`로 감싸는 구현으로 바꾼다
- 알려진 제약: `1.0.2-alpha17`은 오전/오후 표기가 `"AM"`, `"PM"`으로 고정되어 있다(라이브러리 소스의 `TODO localize`). 한국어·일본어에서도 피커 안에는 AM/PM으로 보이고, 설정 행의 시각 표기는 언어별로 표시된다
- 기존 설명 문구의 "오전 9시"를 "설정한 시간"으로 바꾸고, 알림 시간 행·다이얼로그 문자열을 ko/en/ja로 추가

### 5. 문서

- [마감일 알림 정책 가이드](DEADLINE_NOTIFICATION_POLICY_GUIDE.md)의 "오전 9시 고정"을 "설정 시각(기본 09:00)"으로 갱신

## 시간대 정책

- 마감일은 시간대 없는 날짜로 취급한다. 서울에서 입력한 "10월 10일 마감"은 뉴욕에 있어도 10월 10일 마감이다
- 알림은 그 날짜의 **현재 기기 시간대 기준** 설정 시각에 보낸다
- 시간대가 바뀌면 기존 재조정 경로(Android `TIMEZONE_CHANGED`·`TIME_SET`, iOS `NSSystemTimeZoneDidChange`·`significantTimeChange`)가 다시 예약한다
- 서머타임
  - planner는 현지 날짜·시각(wall clock)으로 비교한다
  - 실제 발송 시점 변환은 플랫폼 규칙을 따른다. Android는 kotlinx-datetime `toInstant`, iOS는 `UNCalendarNotificationTrigger`가 처리한다
  - 건너뛰는 시각(예: 02:30이 없는 날)과 반복 시각의 실제 발송은 서머타임 지역 시간대로 실기기에서 확인한다

## Android exact alarm을 쓰지 않는 이유

- 분 단위로 고르면 몇 분 늦는 발송이 지금보다 눈에 띌 수 있다
- 그러나 `SCHEDULE_EXACT_ALARM`은 Android 14부터 기본 거부라 사용자가 별도 권한을 허용해야 하고, `USE_EXACT_ALARM`은 알람·캘린더 앱용이라 Play 정책 대상이 아니다
- 마감일 알림은 정각 보장이 필요한 기능이 아니므로 WorkManager를 유지하고, 실기기에서 지연 폭을 측정해 문제가 크면 별도 이슈로 다룬다

## 테스트

| 대상 | 확인할 것 |
|---|---|
| `DeadlineReminderPlannerTest` | 설정 시각 기준 제외 경계(정각 지남·1분 남음), 기본 09:00 |
| `DeadlineReminderTimeTest` | 분 단위 변환 왕복, 범위 밖 저장값은 09:00 |
| `DefaultDeadlineReminderReconcilerTest` | 설정 시각이 scheduler에 전달됨 |
| `AndroidDeadlineReminderSchedulerTest` | 바뀐 시그니처로 기존 예약 동작 유지 |
| `HomePresenterDeadlineReminderTest` | 시각 변경 이벤트 → 저장 + 재조정 |
| 백업 | 기존 백업에 값이 없으면 기본값으로 복원(직렬화 기본값) |
| iOS | Kotlin/Native compile, 시뮬레이터에서 시각 변경 후 pending request의 trigger 시각 확인 |

## 완료 조건

- 설정에서 시·분을 고를 수 있고, 기존 사용자는 09:00으로 유지된다
- 선택 시각이 Android 예약·Worker 발송 검사·iOS 예약에 동일하게 적용된다
- 시각을 바꾸면 즉시 전체 재예약된다
- 클라우드 백업·복원에 알림 시각이 포함되고 기존 백업 복원도 정상이다
- 시간대 변경과 서머타임 경계를 테스트로 고정한다
- 정책 가이드가 새 동작으로 갱신된다
