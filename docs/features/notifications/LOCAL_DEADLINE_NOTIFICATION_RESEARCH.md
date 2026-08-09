# 마감일 기반 로컬 알림 조사

- 조사일: 2026-08-09
- 관련 이슈: [#211 목표 마감일 기반 로컬 알림 도입](https://github.com/Nexters/BandalArt-KMP/issues/211)
- 기준 commit: `0058885c`
- 범위: 서버·FCM 없이 Android/iOS에서 마감일 로컬 알림을 제공하기 위한 공식 API 제약과 현재 저장소 접점 확인

이 문서는 구현 전략의 입력 자료다. 구체적인 인터페이스, 모듈 배치, PR 분할과 출시 순서는 후속 전략 문서에서 확정한다.

## 결론

서버나 push infrastructure 없이 두 플랫폼 모두 구현할 수 있다. Android는 “오전 9시 이후 시스템이 허용하는 시점”의 지연을 수용한다면 WorkManager one-time unique work가 맞고, iOS는 `UNCalendarNotificationTrigger` one-shot request가 맞다. exact alarm, FCM, APNs 등록과 Push Notifications capability는 v1에 필요하지 않다.

다만 플랫폼 scheduler보다 먼저 해결할 저장소 문제가 있다.

1. main cell의 due date 제거가 현재 DAO patch 의미 때문에 실제 DB에 반영되지 않는다.
2. task update가 부모 sub/main의 완료 상태를 자동 변경하므로 변경한 셀만 예약·취소해서는 stale 알림을 막을 수 없다.
3. 앱 ON/OFF preference와 OS permission/channel authorization은 별도 상태여야 한다.
4. Android warm `singleTask`와 iOS cold-start response를 Splash 이후 Home까지 전달하는 buffered selection 경계가 없다.
5. 같은 날 다건 UX와 iOS의 공개되지 않은 pending request 상한 때문에 per-cell 무제한 예약 여부를 제품·구조 결정으로 남겨야 한다.

따라서 #211은 단순 notification API 연결이 아니라 DB source-of-truth 기반 reconcile, 권한 상태, lifecycle navigation, 실제 기기 검증을 함께 다루는 L 크기 작업이라는 기존 백로그 판단이 타당하다.

## 1. 현재 제품 요구

이슈 #211은 사용자가 설정에서 기능을 직접 켠 경우에만 OS 권한을 요청하고, 미완료이며 제목과 마감일이 유효한 셀을 기기 현지 시각 오전 9시에 알리는 v1을 정의한다. 날짜 변경·제거, 완료·완료 취소, 셀·반다라트 삭제, 설정 OFF/ON 뒤에도 예약이 중복되거나 오래 남지 않아야 한다. 알림 탭은 셀 편집창까지 열지 않고 해당 반다라트를 선택한 홈으로 이동하는 수준이 v1 범위다.

## 2. 저장소에서 확인한 사실

### 2.1 데이터와 날짜 표현

- `BandalartCellDBEntity`는 `bandalartId`, `title`, `dueDate`, `isCompleted`, `parentId`를 이미 저장한다. 알림 자체를 위한 Room 컬럼은 없다: `core/database/src/commonMain/kotlin/com/nexters/bandalart/core/database/entity/BandalartCellDBEntity.kt`.
- `BandalartCellEntity`도 같은 값을 공통 domain에 노출한다. 셀 ID는 Room 자동 생성 `Long`이고 플랫폼 예약 식별자의 원천으로 사용할 수 있다: `core/domain/src/commonMain/kotlin/com/nexters/bandalart/core/domain/entity/BandalartCellEntity.kt`.
- 날짜 피커는 선택 날짜를 `yyyy-MM-ddT00:00` 형태의 현지 날짜·시간 문자열로 만든다. DB 값은 instant나 UTC timestamp가 아니다: `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/ui/bandalart/BandalartDatePicker.kt`.
- 현재 Android `minSdk`는 28, `targetSdk`는 36이고 iOS deployment target은 16.6이다: `gradle/libs.versions.toml`, `iosApp/iosApp.xcodeproj/project.pbxproj`.

따라서 기존 `dueDate`를 마감일이라는 달력 날짜로 해석해 기기의 현재 time zone에서 오전 9시를 계산해야 한다. 문자열을 UTC instant로 간주하면 날짜가 바뀔 수 있다.

### 2.2 저장·완료·삭제 경계

- Home의 셀 저장은 main/sub/task 세 repository 메서드로 나뉜다. 빠른 완료도 task update 메서드를 재사용하고, 셀·반다라트 삭제도 repository를 통한다: `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/presenter/HomePresenter.kt`.
- 실제 Room write는 `DefaultBandalartRepository`에 모여 있다: `core/data/src/commonMain/kotlin/com/nexters/bandalart/core/data/repository/DefaultBandalartRepository.kt`.
- main cell update는 `updateDto.dueDate ?: current.dueDate`로 병합한다. UI가 날짜 제거를 `null`로 전달해도 기존 main due date가 유지되므로, #211의 “due date 제거 시 예약 취소”를 구현하기 전에 nullable patch 의미를 분리해야 한다: `core/database/src/commonMain/kotlin/com/nexters/bandalart/core/database/BandalartDao.kt`의 `updateMainCellWithDto`.
- task update 뒤 DAO가 부모 sub cell과 main cell의 완료 상태를 자동으로 다시 계산한다. 한 task 변경이 여러 셀의 알림 적격성을 바꿀 수 있다: `core/database/src/commonMain/kotlin/com/nexters/bandalart/core/database/BandalartDao.kt`의 `updateCompletionStatus`.
- sub cell 삭제는 물리 삭제가 아니라 해당 sub cell과 자식 task cell을 모두 초기화하고, task cell 삭제도 셀을 초기화한다. main cell 삭제만 FK cascade로 반다라트 전체를 제거한다: 같은 파일의 `deleteCellOrReset`, `resetSubCellWithChildren`, `resetTaskCell`.
- DAO에는 반다라트별 전체 셀 조회는 있지만, 전체 DB에서 알림 적격 셀만 조회하거나 ID가 없을 때 nullable하게 조회하는 API는 없다.
- main due date는 `bandalarts`와 main row인 `bandalart_cells`에 중복 저장된다. reminder source가 두 테이블을 모두 읽으면 main 목표를 중복 예약할 수 있다.

이 구조에서는 화면 이벤트마다 서로 다른 예약 명령을 직접 조합할 경우 부모 자동 완료와 하위 셀 일괄 초기화를 놓칠 위험이 있다. DB write 성공 뒤 최소한 해당 반다라트의 최종 상태를 다시 읽는 동기화 경계가 필요하며, 플랫폼 예약 실패가 이미 성공한 Room write를 되돌려서는 안 된다.

### 2.3 설정과 권한 UI 접점

- `SettingsRepository`는 현재 theme와 최근 이모지만 노출하고, 값은 기존 `bandalart.preferences_pb` DataStore에 저장한다: `core/domain/src/commonMain/kotlin/com/nexters/bandalart/core/domain/repository/SettingsRepository.kt`, `core/datastore/src/commonMain/kotlin/com/nexters/bandalart/core/datastore/BandalartDataStore.kt`.
- 설정 bottom sheet는 Home의 단일 modal 상태이며 화면 설정과 앱 정보 섹션을 가진다. 알림 toggle을 넣을 직접 UI 접점은 `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/ui/settings/SettingsBottomSheet.kt`다.
- 설정의 영속값은 repository `Flow`를 Home Presenter가 수집하고 event로 갱신하는 기존 패턴이 있다: `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/presenter/HomePresenter.kt`.
- 알림 사용 의사와 OS authorization은 같은 상태가 아니다. DataStore toggle이 ON이어도 사용자가 시스템 설정에서 권한을 끌 수 있고, OS 권한을 다시 얻지 못한 상태에서 toggle만 ON으로 저장할 수도 있다. UI는 두 상태를 구분해야 한다.

### 2.4 앱 시작, 플랫폼 binding과 알림 탭

- Metro `PlatformBindings`가 Android/iOS 구현을 공통 `AppGraph`에 주입하는 기존 경계다: `composeApp/src/commonMain/kotlin/com/nexters/bandalart/di/metro/AppGraph.kt`.
- Android graph는 `BandalartApplication.onCreate`에서 한 번 만들어진다. 앱 프로세스가 살아 있지 않아도 실행되는 WorkManager Worker가 application graph와 Room에 접근할 수 있는 접점이다: `androidApp/src/main/kotlin/com/nexters/bandalart/BandalartApplication.kt`.
- iOS graph는 `MainViewController`에서 생성되고 Swift `AppDelegate`는 현재 Firebase 초기화만 한다: `composeApp/src/iosMain/kotlin/com/nexters/bandalart/MainViewController.kt`, `iosApp/iosApp/iosApp.swift`.
- Android `MainActivity`는 `singleTask`지만 notification intent 처리나 `onNewIntent`가 없다. iOS `AppDelegate`도 `UNUserNotificationCenterDelegate`를 연결하지 않는다.
- 공통 navigation은 Splash를 root로 시작하고 Home은 DataStore의 `recentBandalartId`로 선택 항목을 복원한다. 외부 URI나 pending navigation request를 처리하는 공통 deep-link 계층은 현재 없다: `composeApp/src/commonMain/kotlin/com/nexters/bandalart/BandalartApp.kt`, `feature/splash/src/commonMain/kotlin/com/nexters/bandalart/feature/splash/presenter/SplashPresenter.kt`, `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/presenter/HomePresenter.kt`.

따라서 알림 탭 처리는 앱 종료·Splash·onboarding·이미 열린 Home의 네 상태를 모두 고려해야 한다. 단순히 launch intent를 만드는 것만으로는 이미 실행 중인 `singleTask` Activity나 iOS foreground/background response에서 올바른 반다라트를 선택할 수 없다.

### 2.5 의존성과 테스트 기반

- AndroidX WorkManager 의존성은 아직 version catalog와 모듈에 없다. `androidApp`에는 Android 13 notification permission과 notification channel 구현도 없다.
- 공통 날짜 계산에 `kotlinx-datetime` 0.6.1을 사용할 수 있다. 현재 저장·표시는 `yyyy-MM-dd'T'HH:mm` 형태와 별도의 expect/actual `LocalDateTime` wrapper를 사용하므로 저장 형식은 유지하고 reminder projection에서만 `LocalDate`로 정규화하는 편이 범위가 작다: `gradle/libs.versions.toml`, `core/common/src/commonMain/kotlin/com/nexters/bandalart/core/common/extension/String.kt`.
- repository, DataStore, DAO, Home Presenter와 Metro graph에 JUnit 5/MockK/Circuit/Robolectric 기반 `androidHostTest`가 이미 있다.
- `androidDeviceTest`와 iOS test target은 아직 없다. 프로젝트 테스트 가이드도 실제 permission·notification처럼 host fake가 의미 없는 검증만 device test로 올리도록 규정한다: `docs/architecture/kmp/KMP_TESTING_GUIDE.md`.
- 다국어 문자열은 Compose resources의 한국어 기본값, 영어, 일본어 세 묶음으로 관리한다: `core/designsystem/src/commonMain/composeResources/values*/strings.xml`.

## 3. Android 공식 API 조사

### 3.1 WorkManager와 시간 정확도

- WorkManager는 앱 재시작과 기기 재부팅 뒤에도 유지돼야 하는 deferrable persistent work용 API다. 반면 exact alarm은 Doze를 깨우므로 알람 시계나 캘린더처럼 정밀성이 사용자 핵심 기능인 경우에만 사용하도록 Android가 제한한다: [Persistent work](https://developer.android.com/develop/background-work/background-tasks/persistent), [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms).
- one-time work의 `initialDelay`는 작업이 실행될 수 있는 **최소 지연**이다. 실제 실행 시각은 시스템 최적화와 Doze 등의 영향을 받으므로 오전 9시 정각을 보장하지 않는다: [Define work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- unique work는 논리 작업 이름으로 중복 enqueue를 막는다. one-time work를 `ExistingWorkPolicy.REPLACE`로 enqueue하면 같은 이름의 기존 작업을 취소하고 새 작업으로 교체할 수 있고, name/tag/id로 취소·관찰할 수 있다: [Manage work](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work).
- 조사 시점의 stable WorkManager는 2.11.2이고 minSdk 23, compileSdk 33 이상을 요구한다. 이 저장소의 minSdk 28/compileSdk 37과 호환된다: [WorkManager release notes](https://developer.android.com/jetpack/androidx/releases/work).

따라서 v1이 “마감일 오전 중 알림”의 지연을 허용한다면 WorkManager one-time unique work가 공식 API 용도에 맞는다. “오전 9시 정각”이 SLA라면 WorkManager로 보장할 수 없으며, 그 요구가 실제로 생길 때 AlarmManager와 특별 권한의 비용을 다시 평가해야 한다. v1에는 `SCHEDULE_EXACT_ALARM` 또는 `USE_EXACT_ALARM`을 추가할 근거가 없다.

### 3.2 권한과 notification channel

- targetSdk 36인 이 앱은 Android 13(API 33) 이상에서 `POST_NOTIFICATIONS` runtime permission 적용 대상이다. 신규 설치의 알림은 기본 OFF이며, target 33 이상 앱은 권한 prompt를 요청할 시점을 직접 정할 수 있다: [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission).
- Android 8(API 26) 이상에서는 모든 알림에 channel이 필요하다. channel 생성은 반복해도 안전하지만 생성 뒤 앱이 importance와 동작을 바꿀 수 없고 사용자가 시스템 설정에서 변경할 수 있다: [Create and manage notification channels](https://developer.android.com/develop/ui/compose/notifications/channels).
- 앱의 알림 ON preference, `POST_NOTIFICATIONS` 허용 여부, `deadline_reminder` channel 차단 여부는 서로 다른 상태다. 지원 범위 전체가 API 28 이상이므로 모든 기기에서 channel 상태도 확인해야 한다.
- Android 공식 permission 문서는 ADB로 허용·거절·신규 설치 상태를 재현하는 명령도 제공한다. 자동화된 policy test와 별개로 실제 prompt 회귀 검증에 사용할 수 있다: [Notification runtime permission testing](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

### 3.3 알림 grouping과 탭 navigation

- Android의 grouped notification은 각 child에 같은 group key를 지정하는 것만으로 끝나지 않는다. 별도의 summary notification에 `setGroupSummary(true)`를 지정해야 한다: [Create a group of notifications](https://developer.android.com/develop/ui/views/notifications/group).
- notification 탭이 Activity를 열 때 explicit `PendingIntent`와 올바른 back stack을 구성해야 한다. 공식 예시는 mutable input이 필요하지 않은 일반 탭 intent에 `FLAG_IMMUTABLE`을 사용하고, 기존 extras를 갱신할 때 `FLAG_UPDATE_CURRENT`를 함께 사용한다: [Start an Activity from a notification](https://developer.android.com/develop/ui/views/notifications/navigation).
- 현재 `MainActivity`가 `singleTask`이므로 cold start의 `onCreate`와 이미 실행 중인 앱의 `onNewIntent`를 모두 처리해야 한다. v1은 앱 외부 URI를 받을 필요가 없으므로 public deep-link intent filter를 추가하지 않고 explicit app intent로 구현할 수 있다.

### 3.4 재부팅, 시간대와 최종 실행 검증

- WorkManager가 persistent work를 재부팅 뒤 복구하므로 v1에서 별도 `BOOT_COMPLETED` receiver와 permission은 필요하지 않다: [Persistent work](https://developer.android.com/develop/background-work/background-tasks/persistent).
- 기존 initial delay는 time zone이나 사용자가 수동으로 바꾼 시계에서 “새 현지 날짜 오전 9시” 의미를 저절로 다시 계산하는 계약이 아니다. `TIME_SET`과 `TIMEZONE_CHANGED`는 manifest-declared implicit broadcast 제한의 예외이지만 Android는 불필요한 listener를 피하도록 요구한다: [Implicit broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions).
- 이 기능에서 receiver가 필요하다면 DB를 직접 오래 읽는 대신 unique reconcile work만 enqueue해야 한다. 실제 DB 비교와 예약 교체는 background work가 맡아야 한다.
- Worker 실행 시에는 예약 당시 input만 신뢰하지 않고 현재 DB에서 toggle, 셀 존재, 제목, 완료 상태, due date를 다시 확인해야 오래된 알림을 막을 수 있다. 권한 거절이나 stale work는 영구 retry할 일시 장애가 아니므로 정상 종료시키는 편이 무한 재시도를 피한다.

### 3.5 테스트와 진단

- WorkManager integration test는 `TestDriver.setInitialDelayMet`으로 initial delay 충족을 가상화할 수 있고 Worker 구현은 별도 worker test API로 단위 검증할 수 있다: [Integration testing](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing), [Test Worker implementations](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/worker-impl).
- 예약 상태는 `adb shell dumpsys jobscheduler`와 WorkManager diagnostics로 확인할 수 있다: [Debug WorkManager](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/debug).
- Doze와 App Standby는 공식 ADB 절차로 강제 진입·해제할 수 있다. initial delay의 실제 지연 허용 수준은 에뮬레이터/기기에서 별도로 검증해야 한다: [Test with Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby).

### 3.6 현재 저장소에 미치는 영향

- suspend DB 검증을 수행하는 `CoroutineWorker`를 위해 WorkManager `work-runtime-ktx`와 `work-testing` 의존성을 version catalog와 Android 대상 모듈에 새로 추가해야 한다. 현재 저장소에는 WorkManager가 없다.
- scheduler/Worker/notification publisher는 Android SDK를 참조하지만 eligibility, stable ID, 날짜 정책과 desired-state 계산은 공통 테스트가 가능하다. 기존 `PlatformBindings`는 플랫폼 구현을 common graph에 전달할 수 있는 경계다.
- Worker는 `BandalartApplication.appGraph`를 통해 app-scoped Room/repository에 접근할 수 있다. 다만 domain repository에는 nullable cell lookup이나 active reminder projection이 없어 Worker 최종 검증과 전체 reconcile을 위한 읽기 계약이 추가로 필요하다.
- 알림 permission 요청처럼 화면 lifecycle이 필요한 동작은 `feature/home`의 기존 `FlexibleUpdateEffect` expect/actual 패턴과 `rememberLauncherForActivityResult`를 재사용할 수 있다. app-scoped scheduler/status 조회와 일회성 OS prompt effect는 분리 대상이다.
- notification intent의 `bandalartId`는 cold/warm start 모두에서 app-scoped pending selection으로 전달하고 Home이 목록을 읽은 뒤 소비해야 한다. 현재 Home Presenter는 Room 목록의 각 emission에서 `recentBandalartId`를 다시 읽지만 DataStore 변경 자체는 Room emission을 만들지 않으므로 warm app에서 값만 저장해서는 화면 선택이 즉시 바뀌지 않는다.
- 조사 단계에서는 cell별 unique work와 Android child/summary 조합도 검토했다. 그러나 task write의 parent 자동 완료, iOS pending capacity와 cross-board summary navigation이 복잡해 최종 전략은 `(bandalartId, dueDate)` batch를 가까운 순서로 제한해 예약하고 cross-board Android summary를 만들지 않는 방식으로 결정한다.

## 4. iOS 공식 API 조사

### 4.1 권한과 설정 상태

- Apple은 알림이 필요한 맥락에서 권한을 요청하고, 예약 전에 현재 notification settings를 확인하도록 안내한다. 최초 요청 뒤에는 같은 API를 다시 호출해도 시스템 prompt가 다시 나타나지 않는다: [Asking permission to use notifications](https://developer.apple.com/documentation/usernotifications/asking-permission-to-use-notifications).
- `requestAuthorization` completion은 background thread에서 호출될 수 있고, 반환 Boolean만 장기 상태로 저장하기보다 `getNotificationSettings`로 현재 authorization을 다시 확인해야 한다: [requestAuthorization(options:completionHandler:)](https://developer.apple.com/documentation/usernotifications/unusernotificationcenter/requestauthorization(options:completionhandler:)), [authorizationStatus](https://developer.apple.com/documentation/usernotifications/unnotificationsettings/authorizationstatus).
- `denied` 상태에서는 시스템이 앱의 로컬 알림 예약 시도를 무시한다. `authorized`, `provisional`, `denied`, `notDetermined`를 앱의 ON/OFF preference와 별도로 다뤄야 한다: [UNAuthorizationStatus](https://developer.apple.com/documentation/usernotifications/unnotificationsettings/authorizationstatus).
- iOS 16 이상에서는 `UIApplication.openNotificationSettingsURLString`으로 앱의 알림 설정 화면을 열 수 있다. 프로젝트의 deployment target이 16.6이므로 이 API를 직접 사용할 수 있다: [openNotificationSettingsURLString](https://developer.apple.com/documentation/uikit/uiapplication/opennotificationsettingsurlstring).
- 로컬 알림만 사용하는 경우 APNs 등록, Push Notifications capability와 새 Info.plist usage-description key는 필요하지 않다. v1의 사용자 가치에는 `.alert`와 `.sound`면 충분하고 badge, time-sensitive, critical, provisional authorization은 별도 제품 결정 없이는 범위에 넣지 않는다. Focus와 Scheduled Summary에 따라 시스템 표시가 지연될 수 있다는 점은 수용 기준에 반영해야 한다.

### 4.2 예약, 교체와 취소

- 로컬 알림은 `UNMutableNotificationContent`, trigger, `UNNotificationRequest`를 만들고 `UNUserNotificationCenter.add`로 등록한다. 조건이 바뀌면 기존 pending request를 제거해야 한다: [Scheduling a notification locally from your app](https://developer.apple.com/documentation/usernotifications/scheduling-a-notification-locally-from-your-app).
- `UNCalendarNotificationTrigger`는 `DateComponents`의 지정 필드가 일치하는 시점에 발화한다. one-shot은 `repeats = false`로 만들고, `nextTriggerDate`로 실제 다음 발화 시각을 검증할 수 있다: [UNCalendarNotificationTrigger](https://developer.apple.com/documentation/usernotifications/uncalendarnotificationtrigger), [DateComponents](https://developer.apple.com/documentation/foundation/datecomponents).
- 같은 identifier로 새 pending request를 추가하면 이전 request를 교체한다. `deadline.v1.<bandalartId>.<cellId>`처럼 앱 기능 namespace와 두 stable ID를 모두 포함하면 upsert가 가능하다: [UNNotificationRequest.identifier](https://developer.apple.com/documentation/usernotifications/unnotificationrequest/identifier).
- pending request 전체 조회, identifier별 pending 제거, 이미 Notification Center에 표시된 delivered notification 제거는 서로 다른 API다. 날짜 변경·완료·삭제 뒤 이미 표시된 stale 항목까지 없애려면 pending과 delivered를 구분해 정리해야 한다: [getPendingNotificationRequests](https://developer.apple.com/documentation/usernotifications/unusernotificationcenter/getpendingnotificationrequests(completionhandler:)), [removePendingNotificationRequests](https://developer.apple.com/documentation/usernotifications/unusernotificationcenter/removependingnotificationrequests(withidentifiers:)), [removeDeliveredNotifications](https://developer.apple.com/documentation/usernotifications/unusernotificationcenter/removedeliverednotifications(withidentifiers:)).
- 시스템은 한 앱의 pending request 수를 제한하지만 Apple의 현행 공개 API 문서와 SDK header는 이 기능이 의존할 수 있는 숫자를 명시하지 않는다. 셀 수가 계속 늘 수 있다면 모든 미래 마감일을 무기한 개별 예약하는 전제는 안전하지 않다.

### 4.3 foreground, 탭과 grouping

- 앱이 foreground일 때 표시 여부는 `UNUserNotificationCenterDelegate.willPresent`에서 결정하고, 사용자의 탭과 action은 `didReceive`에서 처리한다. 두 callback의 completion handler는 반드시 호출해야 한다: [Handling notifications and notification-related actions](https://developer.apple.com/documentation/usernotifications/handling-notifications-and-notification-related-actions).
- delegate는 앱 launch 완료 전에 notification center에 연결해야 종료 상태에서 탭한 response도 받을 수 있다. 현재 저장소에서는 `iosApp/iosApp/iosApp.swift`의 `AppDelegate.didFinishLaunching`이 그 접점이다.
- `threadIdentifier`가 같은 알림은 시스템 UI에서 시각적으로 그룹화할 수 있다. 그러나 이것만으로 제품이 원하는 “N개의 목표가 오늘 마감” 요약 문구나 정확한 count가 보장되지는 않는다: [UNMutableNotificationContent.threadIdentifier](https://developer.apple.com/documentation/usernotifications/unmutablenotificationcontent/threadidentifier).
- `categoryIdentifier`는 custom action/category 연결용이며 grouping 식별자가 아니다. v1에 action button이 없다면 category 등록은 탭 routing과 별개로 최소화할 수 있다.

### 4.4 시간대 변화

- `DateComponents`는 calendar와 time zone 문맥에서 평가된다. 현재 calendar/time zone 변화를 추적하는 Foundation API가 있지만, 여행 뒤 기존 `UNCalendarNotificationTrigger`가 새 지역의 오전 9시로 어떻게 재해석되는지는 공개 문서가 제품 정책 수준으로 충분히 명시하지 않는다: [Calendar.autoupdatingCurrent](https://developer.apple.com/documentation/foundation/calendar/autoupdatingcurrent), [TimeZone.autoupdatingCurrent](https://developer.apple.com/documentation/foundation/timezone/autoupdatingcurrent).
- UIKit은 날짜 변경, 통신사 시각 갱신, DST 변화 같은 중요한 시간 변경 notification을 제공한다. foreground 복귀 때의 재조정과 함께 실제 기기에서 여행·DST 시나리오를 검증해야 한다: [UIApplication.significantTimeChangeNotification](https://developer.apple.com/documentation/uikit/uiapplication/significanttimechangenotification).

### 4.5 현재 저장소에 미치는 영향

- Swift `AppDelegate`가 notification center delegate를 launch 완료 전에 등록하고 `willPresent`, `didReceive`를 처리할 수 있다. 플랫폼 scheduler 자체는 `iosMain` 구현으로 두고 Metro `PlatformBindings`에 주입할 수 있다.
- UserNotifications completion API를 suspend로 감쌀 수 있지만 callback thread에서 Compose/Circuit 상태를 직접 바꾸면 안 된다.
- notification `userInfo`의 primitive `bandalartId`는 Compose navigator가 준비되기 전에 도착할 수 있다. cold start response를 buffer한 뒤 Splash/onboarding을 지나 Home이 소비하는 bridge가 필요하다.
- 앱 toggle이 ON이고 OS 상태가 `notDetermined`일 때만 prompt를 요청하는 흐름, `denied`일 때 OS 설정 이동을 제공하는 흐름, foreground 복귀 때 settings를 다시 읽는 흐름이 분리돼야 한다.
- 오전 9시 calendar components를 만들기 전에 due date 문자열을 달력 날짜로 정규화하고, 이미 지난 날짜와 오늘 오전 9시 이후를 제외해야 한다. `nextTriggerDate`가 없거나 과거면 등록하지 않는 방어가 가능하다.
- pending 상한 숫자가 공개 계약이 아니므로 per-cell 무제한 예약, 가까운 일정만 rolling 예약, 날짜·반다라트 단위 집계 중 하나를 후속 전략에서 선택해야 한다. `threadIdentifier`만으로 집계 문구를 대체할 수 없다.
- Compose resources 문자열은 notification daemon이 표시 시점에 자동 해석하지 않는다. 예약 시점에 현재 언어 문구를 content에 넣거나 iOS host의 `Localizable.strings`와 `localizedUserNotificationString(forKey:arguments:)`를 사용해야 한다. 후자는 예약 뒤 시스템 언어가 바뀌어도 표시 시점 locale을 따를 수 있다.

## 5. 교차 플랫폼에서 확인해야 할 설계 질문

후속 전략에서 다음을 명시적으로 결정해야 한다.

1. DB가 source of truth일 때 `schedule/cancel` 명령형 API와 `reconcile` API의 책임을 어떻게 나눌지
2. 한 셀 변경 뒤 해당 셀만 동기화할지, 부모 자동 완료까지 포함해 해당 반다라트 전체를 동기화할지
3. 알림 ON 저장, OS 권한 요청, 권한 거절·영구 거절, 시스템 설정 이동을 어떤 상태 머신으로 표현할지
4. 앱 시작, foreground 복귀, 시간대 변경에서 reconcile을 각각 누가 호출할지
5. 같은 날 여러 셀을 개별 알림과 summary/group으로 어떻게 보여 줄지
6. 알림 탭의 `bandalartId`를 Splash/onboarding을 지나 Home Presenter까지 유실 없이 전달할 최소 경계
7. Android WorkManager의 지연 허용 범위가 제품 기대에 맞는지와 iOS pending request 수 상한을 어떻게 다룰지
8. 플랫폼 예약 상태 전체 열람이 불완전하거나 비용이 클 때 stale 예약을 무해하게 만드는 최종 실행 시 검증 방식

## 6. 현재까지 확인된 주요 위험

- **오래된 알림:** 저장 이후 플랫폼 예약 갱신이 실패하면 DB와 pending 예약이 달라진다. 다음 시작 시 reconcile과 발송 직전 유효성 확인이 필요하다.
- **main 날짜 제거 실패:** 현재 DAO의 main update는 `null`을 “기존 값 유지”로 사용해 사용자가 main due date를 제거할 수 없다. 이 상태에서 cancel 연동만 추가하면 DB source of truth가 이미 잘못된다.
- **부모 자동 완료 누락:** task 하나를 완료하면 DAO가 sub/main도 완료할 수 있다. 셀 단위 hook만으로는 부모 예약이 남는다.
- **삭제 범위 오판:** UI의 “삭제”가 셀 종류에 따라 cascade delete 또는 다수 셀 reset으로 달라진다.
- **시간대 의미 혼동:** `dueDate`는 현지 달력 날짜인데 instant처럼 변환하면 여행이나 DST 전환에서 다른 날짜 알림이 될 수 있다.
- **권한과 toggle 불일치:** 앱 preference와 시스템 authorization을 하나의 Boolean으로 취급하면 설정 UI가 실제 동작을 잘못 표시한다.
- **실행 중 탭 유실:** Android `singleTask`와 iOS delegate response를 공통 Home 선택으로 넘기는 경로가 현재 없다.
- **테스트 공백:** JVM fake로 정책과 idempotency는 검증할 수 있지만 실제 OS 권한 prompt, 종료 상태 delivery, Doze와 iOS notification response는 현재 CI가 자동 검증하지 않는다.

## 7. 출처

플랫폼 공식 문서 링크는 Android/iOS 조사 절의 주장 가까이에 둔다. 저장소 구조에 관한 근거는 위에 적은 기준 commit의 파일 경로다.
