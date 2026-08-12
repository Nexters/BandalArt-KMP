# Android 마감 알림 시작 복구

- 관련 이슈: [#211 목표 마감일 기반 로컬 알림 도입](https://github.com/Nexters/BandalArt-KMP/issues/211)
- 수정 PR: [#310 Android 시작 시 마감 알림 예약 복구](https://github.com/Nexters/BandalArt-KMP/pull/310)
- 적용 버전: Android 2.3.0

## 증상

사용자가 마감일 알림을 켜고 목표의 마감일을 저장해도, 이전 예약 실패나 앱 업데이트 등의 이유로 WorkManager 예약이 사라진 상태가 다음 실행에서 복구되지 않을 수 있었다.

Room의 목표·마감일과 DataStore의 알림 설정은 남아 있었다. 유실된 것은 원본 데이터가 아니라 이 원본에서 파생되는 WorkManager 예약 상태였다.

## 원인

Android 앱 시작 시 `BandalartApplication`은 알림 Worker가 사용할 dependency registry만 설치했다. 저장된 목표를 다시 읽고 플랫폼 예약을 만드는 `DeadlineReminderReconciler.reconcileAll()`은 보장된 사용자 콜드 스타트 경로에서 호출되지 않았다.

기존 Home의 `LifecycleEventEffect(ON_RESUME)`는 이후 foreground 복귀를 복구하지만, Splash를 지나 Home effect가 설치되기 전에 발생한 최초 resume event까지 재생하지는 않는다. 따라서 사용자가 앱을 한 번 열어도 다음 resume이나 목표 수정 전까지 누락된 예약이 그대로 남을 수 있었다.

## 해결

사용자가 여는 `MainActivity.onCreate()`가 새 activity launch인지 확인한 뒤 `BandalartApplication`에 복구를 요청한다. 실제 `reconcileAndroidDeadlineReminders()`는 activity 회전이나 종료에 취소되지 않도록 application scope에서 실행한다.

```text
사용자 앱 실행
  -> Application: graph 생성 + Worker dependency registry 설치
  -> MainActivity.onCreate: 사용자 launch 복구 요청
  -> Application scope: reconcileAll()
  -> Room 후보 + DataStore 설정 + OS 권한 조회
  -> 공통 planner가 유효한 가까운 batch 계산
  -> WorkManager 예약을 desired state로 교체
```

`DefaultDeadlineReminderReconciler`는 다음 순서로 동작한다.

1. 알림 설정이 OFF면 기능 namespace의 예약과 표시된 알림을 정리한다.
2. OS 권한이나 channel이 허용되지 않으면 예약을 정리하고 health에 권한 오류를 남긴다.
3. Room의 전체 셀 projection을 읽는다.
4. 완료·빈 제목·과거 마감일을 제외하고 가까운 32개 batch를 계산한다.
5. `AndroidDeadlineReminderScheduler.replaceAll()`로 WorkManager 상태를 다시 만든다.

reconcile 내부의 플랫폼·DB 예외는 scheduling health에 기록하고 삼킨다. 따라서 복구 실패가 앱 시작이나 이미 성공한 목표 저장을 실패시키지는 않는다.

## `Application.onCreate()`에서 실행하지 않는 이유

WorkManager도 앱 프로세스가 죽어 있으면 `Application.onCreate()`를 거쳐 `DeadlineReminderWorker`를 실행한다. 이 시점에 무조건 `reconcileAll()`을 호출하면 `replaceAll()`의 첫 단계인 `cancelAllWorkByTag()`가 지금 전달하려는 reminder work까지 취소할 수 있다.

특히 마감일 09:00 이후 planner는 같은 날 batch를 다시 예약하지 않으므로, 이 경쟁이 발생하면 해당 알림은 복구되지 않는다. 따라서 책임을 분리한다.

- `Application`: background Worker가 즉시 사용할 graph와 registry만 준비한다.
- `MainActivity`: 실제 사용자 콜드 스타트인지 판별하고 복구를 요청한다. configuration recreation에서는 중복 요청하지 않는다.
- application scope: Activity가 종료되어도 durable state의 플랫폼 예약 복구를 끝까지 수행한다.
- Home foreground effect: 앱이 background에서 돌아온 뒤 권한·시간·데이터 변경을 다시 맞춘다.
- 시간 변경 receiver: 시스템 시간 또는 시간대 변경을 unique reconcile work로 처리한다.

## 테스트

`AndroidDeadlineReminderInfrastructureTest.reconciliationEntryPointDelegatesToAppGraph`는 공용 Android reconcile 진입점이 app graph의 reconciler를 정확히 한 번 호출하는지 검증한다. `MainActivity`의 실제 wiring과 background Worker 단독 프로세스 시작은 수동 검증 항목으로 남긴다.

기존 테스트가 함께 보장하는 계약은 다음과 같다.

- planner가 과거 날짜, 빈 제목, 완료된 셀을 제외한다.
- scheduler가 feature namespace를 교체하고 고유 work를 예약한다.
- Worker가 발송 직전 Room과 설정·권한을 다시 확인한다.
- dependencies가 아직 설치되지 않은 Worker는 retry한다.
- reconcile 오류가 목표 저장 경계 밖에서 best effort로 처리된다.

수동 확인은 알림을 켠 상태에서 미래 마감일을 저장한 뒤 앱을 강제 종료하고 다시 실행하는 순서로 한다. 재실행 후 WorkManager 예약이 복구되는지 확인하고, 별도로 앱 프로세스가 없는 상태에서 마감 시각 Worker가 알림을 정상 표시하는지도 확인한다.

## 재발 방지 기준

- WorkManager가 시작할 수 있는 `Application.onCreate()`에서 feature-tag 전체 취소를 수행하지 않는다.
- 사용자 앱 시작과 background Worker 프로세스 시작을 같은 의미의 "cold start"로 취급하지 않는다.
- 예약 상태는 source of truth로 간주하지 않고 Room·DataStore에서 재생성 가능한 파생 상태로 유지한다.
- 시작 복구 테스트는 단순 registry 설치가 아니라 Android reconcile entry point가 app graph에 위임하는지 검증한다.
- Activity wiring을 바꿀 때는 사용자 launch에서만 application scope 복구를 요청하고 WorkManager 단독 프로세스 시작에서는 요청하지 않는지 확인한다.
