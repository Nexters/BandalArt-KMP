# Android와 iOS 위젯 동기화 모델 비교

조사 기준일: 2026-08-12

이 문서는 Android Jetpack Glance/AppWidget과 iOS WidgetKit의 데이터 전달 및 화면 갱신 모델을 공식 Android·Apple 자료만으로 비교한다. 결론부터 말하면 **Android가 항상 iOS보다 늦다고 단정할 수는 없지만, Android 쪽이 동기화 결함을 만들거나 기기별 차이를 겪을 수 있는 실행 경로는 더 많다.** 두 플랫폼 모두 앱 UI처럼 동일 프로세스의 상태를 같은 프레임에 다시 그리는 구조가 아니므로, 갱신 API 호출과 홈 화면 표시 완료는 구분해야 한다.

## 핵심 결론

- Android의 `GlanceAppWidget.update()`와 `updateAll()`은 위젯 화면을 직접 그리는 API가 아니라 Glance 콘텐츠를 다시 계산해 `RemoteViews`로 변환하고 host에 보내는 **갱신 요청 경로**다. Glance의 `provideGlance()`는 해당 요청에 반응해 `CoroutineWorker`로 실행된다. [Android: Manage and update GlanceAppWidget](https://developer.android.com/develop/ui/compose/glance/glance-app-widget), [Android: `GlanceAppWidget` API](https://developer.android.com/reference/kotlin/androidx/glance/appwidget/GlanceAppWidget)
- Android 공식 문서는 앱이 깨어 있을 때의 사용자 상호작용에는 즉시 갱신 경로를 사용할 수 있다고 설명한다. 다만 `updateAll()` 완료가 launcher의 다음 프레임 표시 완료까지 뜻한다고 명시하지는 않는다. `RemoteViews`가 다른 프로세스에 표시되고 별도 host가 이를 inflate하므로, **API 호출 직후 같은 프레임 표시를 보장한다고 해석해서는 안 된다.** [Android: Manage and update GlanceAppWidget](https://developer.android.com/develop/ui/compose/glance/glance-app-widget), [Android: `RemoteViews`](https://developer.android.com/reference/android/widget/RemoteViews), [Android: Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- iOS의 `WidgetCenter.reloadTimelines(ofKind:)`와 `reloadAllTimelines()`도 앱이 WidgetKit에 새 timeline을 요청하는 API다. WidgetKit이 timeline provider를 호출하고 시스템이 별도 프로세스에서 view 표현을 렌더링한다. [Apple: `WidgetCenter`](https://developer.apple.com/documentation/widgetkit/widgetcenter), [Apple: Keeping a widget up to date](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date), [Apple: Adding interactivity to widgets and Live Activities](https://developer.apple.com/documentation/widgetkit/adding-interactivity-to-widgets-and-live-activities)
- iOS는 갱신 예산을 동적으로 관리하며, 자주 보는 위젯도 일반적으로 하루 40~70회의 refresh를 배정받는다. WidgetKit은 여러 위젯의 reload를 병합할 수 있어 지정한 정확한 시각과 실제 reload 시각이 달라질 수 있다. 앱이 foreground인 동안의 reload는 예산에 포함되지 않지만, 이 예외가 같은 프레임 반영을 보장하는 것은 아니다. [Apple: Keeping a widget up to date](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date)
- Android는 home screen replacement와 사용자 정의 `AppWidgetHost`를 공식 지원하고, host가 `AppWidgetHostView`를 상속해 자체 구현할 수 있다. Android 12 이상에서 전달되는 크기 목록조차 host 구현에 따라 달라질 수 있다. 따라서 제조사 launcher와 타사 launcher라는 추가 변수가 존재한다. 다만 **launcher 차이를 stale 데이터의 원인으로 확정하려면 같은 빌드와 데이터로 launcher A/B 검증이 필요하다.** [Android: Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host), [AOSP: Android Compatibility Definition, Launcher](https://source.android.com/docs/compatibility/17/android-17-cdd#381_launcher_home_screen)
- iOS 역시 앱과 widget extension이 별도 실행 단위이며 직접 통신하지 않는다. 앱과 extension은 App Group의 공유 컨테이너에 상태를 저장하고, 읽기·쓰기 충돌을 방지하도록 접근을 동기화해야 한다. 즉 iOS도 동시성 문제가 사라지는 것은 아니며, Android보다 host 구현이 중앙화되어 있어 변수의 종류가 더 적은 것에 가깝다. [Apple: Developing a WidgetKit strategy](https://developer.apple.com/documentation/widgetkit/developing-a-widgetkit-strategy), [Apple: App Extension Programming Guide: Handling Common Scenarios](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/ExtensionScenarios.html)

## 갱신 경로 비교

| 단계 | Android Glance/AppWidget | iOS WidgetKit |
| --- | --- | --- |
| 데이터 원본 | 앱의 Room, DataStore 등 영속 저장소를 위젯이 다시 읽는다. Glance 문서는 app widget을 수동적 UI로 두고 앱이 data layer를 관리하라고 안내한다. [공식 문서](https://developer.android.com/develop/ui/compose/glance/glance-app-widget#manage-glanceappwidget-state) | 앱과 widget extension이 App Group의 파일·데이터베이스 등 공유 컨테이너를 읽는다. [공식 문서](https://developer.apple.com/documentation/widgetkit/developing-a-widgetkit-strategy#Store-shared-data-in-a-group-container) |
| 갱신 계기 | 데이터 변경 후 앱이 `update()` 또는 `updateAll()`을 호출한다. 변경 사실을 알려 갱신하는 책임은 앱에 있다. [공식 문서](https://developer.android.com/develop/ui/compose/glance/glance-app-widget#update-glanceappwidget) | 앱이 `reloadTimelines(ofKind:)` 또는 `reloadAllTimelines()`로 timeline 재생성을 요청한다. [공식 문서](https://developer.apple.com/documentation/widgetkit/widgetcenter) |
| UI 생성 | `provideGlance()`가 background `CoroutineWorker`에서 실행되고, Glance가 Composable을 `RemoteViews`로 변환한다. [공식 API](https://developer.android.com/reference/kotlin/androidx/glance/appwidget/GlanceAppWidget#provideGlance(android.content.Context,androidx.glance.GlanceId)) | WidgetKit이 timeline provider에서 entry를 받고, entry 기반 SwiftUI view 표현을 archive한다. 위젯 코드는 앱과 독립된 extension process에서 실행된다. [공식 문서](https://developer.apple.com/documentation/widgetkit/adding-interactivity-to-widgets-and-live-activities#Understand-the-role-of-app-intents) |
| 최종 표시 주체 | `RemoteViews`는 다른 프로세스에 표시할 view hierarchy이며, launcher 등의 `AppWidgetHost`가 `AppWidgetHostView`에 inflate해 표시한다. [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews), [AppWidgetHost](https://developer.android.com/develop/ui/views/appwidgets/host) | WidgetKit이 별도 프로세스에서 view를 렌더링한다. widget extension은 화면에 보여도 계속 실행 중이지 않다. [공식 문서](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date) |
| 앱이 깨어 있을 때 | 공식 문서는 foreground 상호작용 직후 `update()`를 호출하는 즉시 갱신 경로를 지원한다. [공식 문서](https://developer.android.com/develop/ui/compose/glance/glance-app-widget#when-to-update-widgets) | 앱 foreground 중 reload는 일일 예산에서 제외된다. 그래도 WidgetKit이 timeline을 다시 요청하고 렌더링하는 구조는 유지된다. [공식 문서](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date#Plan-reloads-within-a-budget) |
| 앱이 깨어 있지 않을 때 | `updatePeriodMillis`는 30분 미만을 지원하지 않으며, WorkManager periodic work도 전력 제약을 받는다. background broadcast는 시스템 과부하 시 호출이 지연될 수 있다. [공식 문서](https://developer.android.com/develop/ui/views/appwidgets/advanced#determine-how-often-to-update-a-widget) | timeline과 system budget에 의존한다. 자주 보는 위젯도 보통 하루 40~70 refresh이며, WidgetKit이 reload를 병합하거나 잘 보지 않는 위젯의 빈도를 낮출 수 있다. [공식 문서](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date#Plan-reloads-within-a-budget) |

## Android에서 결함 여지가 더 많아지는 이유

### 1. 갱신 작업의 중복과 실행 중 세션을 직접 고려해야 한다

`update()`와 `updateAll()` 요청은 `provideGlance()`를 background `CoroutineWorker`로 실행한다. 그런데 공식 API 문서에 따르면 `provideGlance()`가 이미 실행 중이면 새 `update()` 또는 `updateAll()` 호출이 이를 재시작하지 않는다. 대신 초기 데이터를 `provideContent()` 전에 읽고, 활성 composition 동안에는 관찰 가능한 데이터 소스를 구독해야 한다. 앱의 다른 위치에서 데이터를 바꿀 때는 현재 worker가 없을 수 있으므로 다시 `update()`를 호출해야 한다. [Android: `GlanceAppWidget.provideGlance`](https://developer.android.com/reference/kotlin/androidx/glance/appwidget/GlanceAppWidget#provideGlance(android.content.Context,androidx.glance.GlanceId))

이 모델에서는 다음 구현 실수가 실제 stale 화면으로 이어질 수 있다.

- 데이터를 저장하기 전에 `updateAll()`을 요청해 worker가 이전 snapshot을 읽는다.
- 여러 Flow를 따로 관찰해 반다라트 ID와 세부 목표 ID가 서로 다른 시점의 조합으로 만들어진다.
- 앞선 갱신이 늦게 끝나 최신 `RemoteViews` 뒤에 이전 `RemoteViews`를 전달한다.
- 실행 중 composition이 데이터 변경을 관찰하지 않는데, 새 `updateAll()`이 기존 `provideGlance()`를 재시작할 것이라고 가정한다.

앞의 첫 번째와 세 번째 문제는 Android가 자동으로 해결해 주는 순서 보장이 아니라 앱 구현에서 저장 완료, 최신 snapshot 생성, 갱신 요청을 직렬화해 해결해야 하는 영역이다. 두 번째 문제도 최근 반다라트와 최근 세부 목표를 한 snapshot으로 읽어야 하는 앱 데이터 모델 문제다.

### 2. `RemoteViews`가 launcher host까지 전달되는 단계가 추가된다

Glance는 최종 UI가 아니라 `RemoteViews`를 만들어 host에 보낸다. `RemoteViews` 자체가 다른 프로세스에서 표시하기 위한 `Parcelable` view description이며, launcher는 이를 `AppWidgetHostView`에 inflate한다. [Android: Manage and update GlanceAppWidget](https://developer.android.com/develop/ui/compose/glance/glance-app-widget#update-glanceappwidget), [Android: `RemoteViews`](https://developer.android.com/reference/android/widget/RemoteViews), [Android: Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)

따라서 아래 시점은 서로 다르다.

1. Room/DataStore 저장 완료
2. `updateAll()` 요청 시작 및 반환
3. `provideGlance()`가 읽은 snapshot 확정
4. `RemoteViews` 생성 및 AppWidget service 전달
5. launcher host가 새 `RemoteViews`를 적용하고 화면에 표시

`updateAll()` 반환만 로그로 확인하고 5번까지 성공했다고 판단하면 launcher 전달 문제와 이전 snapshot 생성 문제를 구분할 수 없다. 진단 로그는 최소한 `저장된 revision → provideGlance가 읽은 revision → 요청 완료`를 같은 correlation ID로 남겨야 한다.

AOSP 구현도 이 경계가 비동기임을 보여 준다. system service는 widget update callback을 handler에 예약하고, `AppWidgetHost`의 callback도 받은 update를 host handler에 넣어 listener와 `AppWidgetHostView`에 전달한다. `AppWidgetHostView`는 전달받은 `RemoteViews`를 재사용하거나 새로 inflate/apply한다. 이는 API 요청 접수와 launcher 화면의 실제 frame 변경이 같은 사건이 아님을 뒷받침한다. [AOSP: `AppWidgetServiceImpl`](https://android.googlesource.com/platform/frameworks/base/+/master/services/appwidget/java/com/android/server/appwidget/AppWidgetServiceImpl.java), [AOSP: `AppWidgetHost`](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/appwidget/AppWidgetHost.java), [AOSP: `AppWidgetHostView`](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/appwidget/AppWidgetHostView.java)

### 3. launcher 구현이 하나로 고정되어 있지 않다

Android는 제조사 기본 launcher뿐 아니라 타사 home screen replacement를 지원한다. 공식 host 문서는 앱이 직접 `AppWidgetHost`를 구현하고 `AppWidgetHostView`를 상속할 수 있다고 명시하며, widget 크기 정보와 host별 상태의 관리도 host 책임으로 둔다. [Android: Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host), [AOSP: Android Compatibility Definition](https://source.android.com/docs/compatibility/17/android-17-cdd#381_launcher_home_screen)

이 공식 구조에서 도출할 수 있는 **추론**은 다음과 같다.

- 같은 provider라도 launcher의 process lifecycle, `AppWidgetHostView` 적용 시점, resizing 및 화면 복귀 처리 차이로 사용자에게 보이는 시점이 달라질 가능성이 있다.
- Android OS 버전만 기록해서는 재현 조건이 충분하지 않으며, 제조사·launcher package·launcher version도 수집해야 한다.
- 다만 Room/DataStore에 이전 값이 저장됐거나 `provideGlance()`가 이전 값을 읽은 문제를 OEM launcher 탓으로 돌릴 수는 없다. launcher 원인은 provider가 최신 `RemoteViews`를 생성·전달했다는 증거가 있을 때만 후보로 올린다.

### 4. background 실행 정책이 여러 갈래다

Android widget은 foreground activity 직접 갱신, widget interaction, broadcast, `updatePeriodMillis`, WorkManager 등 여러 계기로 갱신할 수 있다. background broadcast는 기본적으로 background process에서 실행되므로 시스템이 과부하일 때 receiver 호출이 지연될 수 있고, periodic update에는 최소 주기와 전력 제약이 있다. [Android: Create an advanced widget](https://developer.android.com/develop/ui/views/appwidgets/advanced#determine-how-often-to-update-a-widget)

경로가 여러 개면 동일한 데이터 변경이 database observer와 `ON_STOP`에서 거의 동시에 들어오는 식의 중복 요청도 생긴다. 각 계기마다 자체 refresh 로직을 두기보다 하나의 refresh runner에 모으고, 최신 snapshot이 마지막에 적용되도록 순서를 통제해야 한다.

## iOS가 상대적으로 예측 가능해 보이는 이유와 남는 한계

### 중앙화된 WidgetKit renderer

iOS widget extension도 앱과 별도 프로세스에서 실행되지만, timeline 요청·budget·view archive·최종 rendering을 WidgetKit이 통제한다. Android가 home screen replacement와 custom `AppWidgetHost`를 공개적으로 지원하는 것과 달리, Apple 문서의 widget 표시 경로는 WidgetKit이라는 시스템 renderer로 제시된다. 이 공식 구조의 차이에서 **host 구현 차원 재현 변수가 Android보다 적다고 추론할 수 있다.** [Apple: Keeping a widget up to date](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date), [Apple: Adding interactivity to widgets and Live Activities](https://developer.apple.com/documentation/widgetkit/adding-interactivity-to-widgets-and-live-activities), [Android: Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)

그러나 이는 iOS가 실시간이라는 뜻이 아니다. 앱이 `reloadTimelines`를 호출해도 WidgetKit이 새 timeline을 요청하고 view를 렌더링해야 하며, WidgetKit은 배터리 보호를 위해 reload budget을 적용하고 여러 widget reload를 병합할 수 있다. timeline entry는 대략 5분 이상 간격을 두도록 안내하며, 사용자가 거의 보지 않는 홈 화면의 widget은 refresh 빈도가 더 낮아질 수 있다. [Apple: Keeping a widget up to date](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date#Plan-reloads-within-a-budget)

### App Group 공유 저장소도 동기화가 필요하다

Apple은 containing app과 app extension이 직접 통신하지 않으며, 보통 extension이 실행될 때 containing app은 실행 중이지 않다고 설명한다. 양쪽이 App Group shared container를 사용할 수 있지만, 데이터 손상을 피하려면 접근을 동기화해야 한다. SQLite, Core Data 또는 POSIX lock 등을 사용할 수 있다. [Apple: Understand How an App Extension Works](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/ExtensionOverview.html), [Apple: Handling Common Scenarios](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/ExtensionScenarios.html#//apple_ref/doc/uid/TP40014214-CH4-SW11)

따라서 iOS에서도 다음 순서를 지켜야 한다.

1. App Group의 선택 ID와 공유 database 변경을 commit한다.
2. commit이 끝난 후 `reloadTimelines(ofKind:)`를 호출한다.
3. timeline provider는 메모리 cache가 아닌 공유 저장소의 완료된 snapshot을 읽는다.

reload를 먼저 호출하고 저장을 나중에 끝내면 WidgetKit provider가 이전 snapshot을 읽을 수 있다. 이는 Android의 `updateAll()` 이전 저장 완료 원칙과 동일하다.

## “플랫폼 한계”와 “앱 결함” 판정 기준

| 관찰 결과 | 우선 판정 | 근거 및 다음 확인 |
| --- | --- | --- |
| Android에서 홈 진입 직후 이전 값이 보였다가 짧은 시간 안에 최신 값으로 바뀜 | 비동기 `RemoteViews` 생성·host 적용 지연 가능 | 최신 snapshot을 읽었다는 로그와 실제 표시 시각을 함께 측정한다. |
| Android에서 앱을 다시 열고 닫을 때까지 계속 이전 값 | 앱 결함 가능성이 높음 | 변경 observer 누락, 저장보다 빠른 refresh, 실행 중 `provideGlance()`의 데이터 미관찰을 확인한다. 단순 periodic 제한으로 설명하지 않는다. |
| Android의 특정 launcher에서만 stale이고 다른 launcher에서는 같은 시나리오가 정상 | host/launcher 후보 | 동일 기기 또는 동일 빌드에서 launcher A/B 후 `dumpsys appwidget`과 correlation 로그를 비교한다. |
| iOS에서 foreground 앱 변경 후 reload가 약간 늦음 | WidgetKit scheduling 가능 | foreground reload는 예산 제외지만 timeline 요청과 system render는 비동기다. |
| iOS에서 provider가 계속 이전 App Group 값을 읽음 | 앱 결함 가능성이 높음 | shared database commit 순서, container 경로, transaction 및 lock을 확인한다. |
| 앱이 종료된 동안 외부 데이터만 변경되고 widget이 그대로임 | 별도 background 동기화 계기 필요 | Android는 broadcast/WorkManager/FCM, iOS는 timeline/background processing/WidgetKit push 등 제품 요구에 맞는 계기를 설계한다. |

Android 공식 문서의 “앱이 깨어 있으면 즉시 갱신할 수 있다”는 표현은 foreground 변경을 15분 또는 30분 periodic work까지 기다리라는 뜻이 아니다. BandalArt에서 앱 안의 로컬 변경 후 홈으로 돌아왔는데 widget이 계속 이전 상태라면 먼저 구현 결함으로 추적한다. 반대로 100ms 단위의 동일 프레임 반영을 성공 기준으로 두는 것도 두 플랫폼의 프로세스 분리 모델과 맞지 않는다.

## BandalArt 진단 및 기록 권장안

### 공통 revision을 남긴다

데이터 저장 transaction마다 단조 증가하는 `snapshotRevision` 또는 변경 시각을 만든다. 아래 경계에서 동일 값을 기록하면 “최신 데이터를 못 읽음”과 “최신 화면을 host가 아직 표시하지 않음”을 구분할 수 있다.

- 앱 저장 완료: 반다라트 ID, 세부 목표 ID, 달성률, revision
- Android `provideGlance()` snapshot 읽기: 같은 값과 revision
- Android `updateAll()` 요청 시작·종료: refresh reason과 correlation ID
- iOS App Group commit 완료: 같은 값과 revision
- iOS timeline provider entry 생성: 읽은 값과 revision

사용자 데이터 본문은 로그에 남기지 않고 ID, 달성률, revision만 기록한다.

### Android 실기기 조사표에 launcher를 포함한다

- 앱 `versionName`, `versionCode`, 배포 `headSha`
- 기기 제조사·모델, Android 버전
- launcher package와 version
- widget instance 수와 크기
- 재현 동작과 저장 완료부터 실제 화면 변경까지 걸린 시간
- `adb shell dumpsys appwidget`의 provider/host 연결 상태

Android에는 최소한 Pixel Launcher와 실제 증상 기기의 제조사 launcher를 포함한다. 특정 launcher에서만 발생할 때 타사 launcher까지 비교한다. iOS는 OS 버전, 기기 모델, widget family, Low Power Mode 여부와 앱 foreground/background 상태를 기록한다.

### 성공 기준을 시간 구간으로 정의한다

플랫폼 문서는 foreground update의 동일 프레임 완료 시간을 보장하지 않으므로 “바로”를 테스트 용어로 사용하지 않는다. 실기기 측정 후 제품 기준을 수치화하되, 우선 다음을 별도로 기록한다.

- 저장 완료 시각
- refresh 요청 시각
- provider가 최신 revision을 읽은 시각
- 홈 화면에 최신 값이 처음 표시된 시각

최신 revision을 provider가 읽지 못했으면 앱 동기화 결함이다. provider가 최신 revision을 읽고 새 UI를 전달했는데 특정 launcher만 늦으면 host 차이를 조사한다. 이 구분 없이 `updateAll()` 호출 여부만으로 플랫폼 한계라고 결론 내리지 않는다.

## 공식 자료

### Android

- [Manage and update GlanceAppWidget](https://developer.android.com/develop/ui/compose/glance/glance-app-widget)
- [`GlanceAppWidget` API reference](https://developer.android.com/reference/kotlin/androidx/glance/appwidget/GlanceAppWidget)
- [`RemoteViews` API reference](https://developer.android.com/reference/android/widget/RemoteViews)
- [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- [Create an advanced widget](https://developer.android.com/develop/ui/views/appwidgets/advanced)
- [Android Compatibility Definition: Launcher](https://source.android.com/docs/compatibility/17/android-17-cdd#381_launcher_home_screen)
- [AOSP `AppWidgetServiceImpl`](https://android.googlesource.com/platform/frameworks/base/+/master/services/appwidget/java/com/android/server/appwidget/AppWidgetServiceImpl.java)
- [AOSP `AppWidgetHost`](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/appwidget/AppWidgetHost.java)
- [AOSP `AppWidgetHostView`](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/appwidget/AppWidgetHostView.java)

### Apple

- [Keeping a widget up to date](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date)
- [`WidgetCenter`](https://developer.apple.com/documentation/widgetkit/widgetcenter)
- [Developing a WidgetKit strategy](https://developer.apple.com/documentation/widgetkit/developing-a-widgetkit-strategy)
- [Adding interactivity to widgets and Live Activities](https://developer.apple.com/documentation/widgetkit/adding-interactivity-to-widgets-and-live-activities)
- [Configuring app groups](https://developer.apple.com/documentation/xcode/configuring-app-groups)
- [App Extension Programming Guide: Understand How an App Extension Works](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/ExtensionOverview.html)
- [App Extension Programming Guide: Handling Common Scenarios](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/ExtensionScenarios.html)
