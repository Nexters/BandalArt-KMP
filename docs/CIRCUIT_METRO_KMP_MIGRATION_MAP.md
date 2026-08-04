# Circuit + Metro KMP 이식 맵

## 1. 문서 목적

이 문서는 이슈 #182의 2단계 실행 문서다. `main`과 `develop`을 병합하지 않고 `main`의 KMP 구현에 필요한 Circuit 동작만 기능 단위로 이식하기 위한 기준을 고정한다.

- 통합 기준: `main` `9a70451cd5792a3c4facf2dba166fc337988519d`
- Circuit 참조: `develop` `437df428b545e7b9353bd66a3033d3ea8944b6a2`
- 작업 브랜치: `refactor/circuit-metro-kmp`
- 대상 플랫폼: Android, iOS
- 최종 DI: Metro
- 과도기 DI: Koin + Metro

## 2. 현재 베이스라인

| 항목 | `main` | `develop` | 통합 판단 |
| --- | --- | --- | --- |
| 제품 구조 | KMP/CMP Android+iOS | Android 전용 | `main` 유지 |
| 화면 상태 | ViewModel + StateFlow/Channel | Circuit Presenter + UiState/Event | Presenter 동작을 KMP로 이식 |
| 내비게이션 | Compose Navigation | Circuit BackStack | Circuit으로 기능별 전환 |
| DI | Koin 4.1.0-Beta5 | Hilt 2.60.1 | 양쪽 모두 최종 제거, Metro로 수렴 |
| Kotlin | 2.1.20 | 2.3.21 | Metro Circuit codegen을 위해 2.3.21 이상으로 정렬 |
| AGP / Gradle | 8.8.2 / 8.10.2 | 9.3.0 / 9.5.0 | KMP AGP 9 모듈 분리 후 정렬 |
| SDK | compile/target 35/35 | compile/target 37/36 | Android app을 37/36으로 정렬 |
| Circuit | 없음 | 0.35.1 | 0.35.1 이상 호환 버전 도입 |
| 데이터 | KMP Room + DataStore | Android Room + DataStore + 원격 계층 | `main` 구현 유지 |
| 네트워크 | 제품 흐름에서 사용하지 않는 로컬 DB 앱 | Ktor/Retrofit, guest login 흔적 | 이식하지 않음 |
| 앱 버전 | Android 2.2.0, iOS project 1.0 | Android 2.2.6 (20206) | 실제 배포 기준을 확인해 별도 정렬 |

### 구조상 선행 조건

현재 `composeApp`은 `org.jetbrains.kotlin.multiplatform`과 `com.android.application` 역할을 한 모듈에서 수행한다. Kotlin 공식 문서에 따르면 AGP 9부터 이 조합은 호환되지 않는다. 따라서 Metro 부트스트랩 전 다음 구조가 필요하다.

```text
androidApp                 Android application, manifest, signing, MainActivity/Application
    └── composeApp         KMP shared UI, iOS framework, common/Android/iOS actual 구현
            ├── core/*
            └── feature/*
iosApp                     Xcode entry point → composeApp framework
```

Android의 `MainActivity`, `BandalartApplication`, manifest, signing, build type, baseline profile 연결은 `androidApp`으로 이동한다. `expect/actual` 구현과 Android용 DB/DataStore/AppVersion/ImageHandler 구현은 KMP source set에 남긴다.

## 3. 화면 및 상태 소유자 대응표

| 기능 | `main` 기준 | `develop` 참조 | 이식 결정 | 주의할 동작 |
| --- | --- | --- | --- | --- |
| Splash | `SplashViewModel`, `SplashScreen`, `SplashNavigation` | `SplashScreen : Screen`, `SplashPresenter`, Presenter 테스트 | `main` UI/리소스를 유지하고 공통 Circuit Screen/Presenter로 재작성 | `main`은 미완료 onboarding 시 빈 Bandalart를 만들지만 `develop`은 Home이 빈 목록을 만들므로 생성 책임을 한 곳으로 고정해야 함 |
| Onboarding | `OnboardingViewModel`, `OnBoardingScreen`, `OnBoardingNavigation` | `OnboardingScreen : Screen`, `OnboardingPresenter`, Presenter 테스트 | Presenter/Event 계약 이식 후 VM/Navigation/Koin binding 제거 | 완료 상태 저장 후 `resetRoot(HomeScreen)` 동작 보존 |
| Complete | `CompleteViewModel`, UI state/action/event, `CompleteNavigation` | `CompleteScreen` 인자, `CompletePresenter`, Presenter 테스트 | route 인자를 Circuit Screen으로 옮기고 side effect를 KMP 타입으로 재작성 | Android `android.net.Uri` 대신 현재 KMP URI/플랫폼 image handler 사용 |
| Home | `HomeViewModel`, UI state/action/event, `HomeScreen`, bottom sheet/dialog | `HomeScreen` 계약, `HomePresenter`, app-update 처리/테스트 | 가장 마지막에 읽기 → 편집 → modal → 공유/저장 순으로 분할 이식 | `rememberRetained` modal 상태, 선택 업데이트, capture/share/save와 로컬 데이터 회귀를 별도 검증 |

### Splash 동작 결정

최종 구조에서는 빈 Bandalart 생성 책임을 Home의 “목록이 비었을 때 생성” 흐름 하나로 통일한다. 이유는 다음과 같다.

- `develop` Presenter 테스트가 이 동작을 이미 기준으로 고정한다.
- Splash는 onboarding 분기와 Android 강제 업데이트 진입만 담당하는 편이 책임이 명확하다.
- onboarding 완료 여부와 상관없이 Home이 데이터 불변식을 보장한다.

이 결정은 5단계 Splash/Onboarding 이식 시 Home이 아직 ViewModel이어도 동작해야 한다. 기존 `HomeViewModel`이 빈 목록에서 생성하는지 회귀 테스트로 먼저 확인하고, 그렇지 않다면 Splash의 생성을 제거하지 않는다.

### 공통 코드로 그대로 옮길 수 없는 `develop` 타입

| `develop` 타입/API | 이유 | KMP 대안 |
| --- | --- | --- |
| `android.net.Uri` | Android 전용 | 현재 `com.eygraber.uri.Uri` 또는 문자열 계약 + `ImageHandlerProvider` |
| `java.util.Locale` | Native 공통 코드에서 사용 불가 | `core.common.Locale` |
| `@Parcelize` | Android 구현 세부 | Circuit KMP가 지원하는 screen 직렬화 방식과 각 플랫폼 상태 복원 검증 |
| `ActivityRetainedComponent` | Hilt Android scope | 제거. 공통 Presenter assisted factory + app graph multibinding |
| Play `AppUpdateManager` | Android 전용 | Android UI/플랫폼 effect에 격리, iOS Presenter 계약에 포함하지 않음 |
| Android resource/Lottie API | Android 전용 | `main`의 Compose Multiplatform resource/Compottie 구현 유지 |

## 4. 기능별 파일 분류

### 4.1 공통 보존

아래 `main` 파일/디렉터리는 기능 재구현 없이 유지한다.

- `core/domain/src/commonMain/**`: entity와 repository interface
- `core/data/src/commonMain/**`: mapper와 `Default*Repository`
- `core/database/src/commonMain/**`: Room database, DAO, entity, schema 구성
- `core/database/src/androidMain/**`, `src/iosMain/**`: DB factory actual 구현
- `core/datastore/src/commonMain/**`: DataStore wrapper와 factory 계약
- `core/datastore/src/androidMain/**`, `src/iosMain/**`: DataStore factory actual 구현
- `core/designsystem/src/commonMain/**`, `core/ui/src/commonMain/**`: CMP theme/component/resource
- 각 feature의 `ui/**`, `model/**`, `mapper/**`: KMP로 이미 이전된 UI와 모델
- `AppVersionProvider`, `ImageHandlerProvider`, locale 등 현재 플랫폼 구현

보존은 “수정 금지”가 아니다. Metro constructor/provider annotation과 Circuit state 연결에 필요한 최소 변경은 허용하되 동작과 저장 형식은 바꾸지 않는다.

### 4.2 `develop`에서 동작만 이식

- `feature/splash/**/SplashScreen.kt`, `presenter/SplashPresenter.kt`
- `feature/onboarding/**/OnboardingScreen.kt`, `presenter/OnboardingPresenter.kt`
- `feature/complete/**/CompleteScreen.kt`, `presenter/CompletePresenter.kt`
- `feature/home/**/HomeScreen.kt`, `presenter/HomePresenter.kt`
- 각 feature의 `src/test/**/presenter/*PresenterTest.kt`
- `HandleAppUpdateTest.kt`와 관련 선택 업데이트 상태 전이
- `app/**/CircuitModule.kt`의 factory set → Circuit 생성 방식
- `MainActivity.kt`의 Circuit BackStack/Content 구성

위 파일은 cherry-pick하거나 통째로 복사하지 않는다. 공통 source set에서 컴파일되도록 `main` 타입과 리소스를 사용해 재작성한다.

### 4.3 대체 후 제거

기능이 Circuit으로 전환된 같은 PR에서 아래 `main` 파일을 제거한다.

| 전환 기능 | 제거 대상 |
| --- | --- |
| Splash | `SplashViewModel.kt`, `di/SplashModule.kt`, `navigation/SplashNavigation.kt` |
| Onboarding | `OnBoardingViewModel.kt`, `di/OnboardingModule.kt`, `navigation/OnBoardingNavigation.kt` |
| Complete | `viewmodel/CompleteViewModel.kt`, `CompleteUiState/Action/Event.kt`, `di/CompleteModule.kt`, `navigation/CompleteNavigation.kt` |
| Home | `viewmodel/HomeViewModel.kt`, `HomeUiState/Action/Event.kt`, `di/HomeModule.kt`, `navigation/HomeNavigation.kt` |
| 전체 화면 전환 후 | `core/navigation/Route.kt`, `BandalartNavHost.kt`, Compose Navigation 의존성 |

ViewModel 테스트는 즉시 삭제하지 않고 동일 동작의 Presenter 테스트가 먼저 통과한 뒤 대체한다. ViewModel 테스트에만 있는 회귀 사례는 Presenter 테스트 또는 repository 테스트로 옮긴다.

### 4.4 이식하지 않음

- `develop`의 모든 `dagger.*`, `javax.inject.*`, `@HiltAndroidApp`, `@AndroidEntryPoint`, `@InstallIn` 선언
- `core/network/**`, guest login service/data source/token repository
- Android 전용 Room/DataStore module과 qualifier
- Retrofit/Ktor server binding과 server base URL 의존성
- `develop`의 Android resource 기반 UI 구현
- `NavStack` 전환과 신규 설정 화면/다크 모드

## 5. Repository 및 데이터 대응표

| 계약 | `main` 구현 | `main` Koin 소유권 | `develop` 참조 | 최종 Metro 소유권 |
| --- | --- | --- | --- | --- |
| `BandalartRepository` | `DefaultBandalartRepository` | `singleOf().bind()` | `BandalartRepositoryImpl` + Hilt binding | `AppScope` 단일 인스턴스, `main` 구현 사용 |
| `OnboardingRepository` | `DefaultOnboardingRepository` | `singleOf().bind()` | `OnboardingRepositoryImpl` + Hilt binding | `AppScope` 단일 인스턴스, `main` 구현 사용 |
| `InAppUpdateRepository` | `DefaultInAppUpdateRepository` | `singleOf().bind()` | `InAppUpdateRepositoryImpl` + Hilt binding | `AppScope` 단일 인스턴스, `main` 구현 사용 |
| `BandalartDatabase` | KMP Room factory + bundled driver | `single` | Android `Room.databaseBuilder` | `AppScope`, KMP factory/driver 유지 |
| `BandalartDao` | `database.bandalartDao` | `single` | Hilt `@Provides` | AppGraph 내부 app-scoped binding |
| `BandalartDataStore` | KMP factory로 생성한 Preferences | `single` | Android Context extension | `AppScope`, 기존 파일명/key 유지 |
| `InAppUpdateDataStore` | KMP factory로 생성한 Preferences | `single` | Android qualifier | `AppScope`, 기존 파일명/key 유지 |
| 플랫폼 provider | Android/iOS factory actual | `platformModule single` | Android Context 직접 주입 | AppGraph factory의 `PlatformBindings` 입력 |

`GuestLoginTokenEntity` 등 사용되지 않는 흔적은 이번 단계에서 삭제하지 않는다. 다만 Metro graph에 binding을 추가하지 않고, 실제 제거는 별도 정리 PR에서 참조 여부를 확인한 뒤 수행한다.

## 6. Koin → Metro 객체 소유권 이전 규칙

### 6.1 허용하는 공존 구조

```text
Android/iOS entry point
        ├── AppGraph (Metro가 생성 책임 보유)
        └── Koin
             └── metroBridgeModule(AppGraph accessor를 같은 인스턴스로 노출)
```

- Metro로 이전된 객체는 Metro만 생성한다.
- Koin의 bridge definition은 Metro accessor가 반환한 동일 인스턴스를 노출할 뿐 생성하지 않는다.
- Metro graph는 `getKoin()`, `KoinComponent`, 전역 service locator를 호출하지 않는다.
- 기존 Koin binding을 제거하고 bridge binding을 추가하는 변경은 같은 PR/커밋 단위로 수행한다.
- singleton 여부는 타입뿐 아니라 DB 파일 handle, Preferences DataStore instance까지 확인한다.

### 6.2 금지하는 공존 구조

- `single { DefaultBandalartRepository(...) }`와 Metro의 동일 repository binding 동시 존재
- Koin이 만든 DB/DAO를 Metro graph factory input으로 주입
- Metro가 만든 DB와 Koin이 만든 DAO를 섞는 부분 graph
- Android와 iOS 중 한 플랫폼만 Metro 소유권을 먼저 전환
- Presenter 내부에서 Koin을 직접 조회

### 6.3 이전 순서

의존성의 아래쪽부터 하나의 닫힌 subgraph로 옮긴다.

1. `PlatformBindings`: DB/DataStore factory, AppVersion, ImageHandler
2. Room: database → DAO
3. DataStore: Preferences instance → wrapper
4. Repository 구현과 interface binding
5. Circuit Presenter/UI factory multibinding과 `Circuit`
6. Android/iOS composition root
7. 남은 Koin module/plugin/runtime 제거

Room과 DataStore를 Metro가 소유한 뒤 repository를 옮긴다. 기존 ViewModel이 남아 있는 동안에는 repository accessor만 Koin bridge로 연결한다.

## 7. Metro component 및 scope 설계

### 7.1 최소 graph

`commonMain`에 하나의 app graph를 둔다.

```kotlin
@Scope
annotation class AppScope

@DependencyGraph(AppScope::class)
interface AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Includes platformBindings: PlatformBindings): AppGraph
    }
}

interface PlatformBindings {
    val databaseFactory: BandalartDatabaseFactory
    val dataStoreFactory: BandalartDataStoreFactory
    val appVersionProvider: AppVersionProvider
    val imageHandlerProvider: ImageHandlerProvider
}
```

실제 annotation/import 문법은 Metro 1.3.2 compile spike에서 확정한다. 이 코드는 책임 경계를 설명하기 위한 설계안이다.

### 7.2 플랫폼 생성 지점

| 플랫폼 | graph 생성 지점 | 입력 | 전달 방식 |
| --- | --- | --- | --- |
| Android | `androidApp`의 `BandalartApplication` | application Context로 만든 Android `PlatformBindings` | Application이 소유하고 Activity가 명시적으로 받아 `BandalartApp(graph)`에 전달 |
| iOS | `MainViewController()` 구성 시점 | iOS factory로 만든 `PlatformBindings` | `ComposeUIViewController`의 root composable에 직접 전달 |

`AppGraph` 생명주기는 프로세스/앱 인스턴스와 동일하다. 전역 top-level mutable singleton은 만들지 않는다.

### 7.3 scope 정책

| 대상 | scope | 이유 |
| --- | --- | --- |
| DB, DAO, Preferences DataStore | `AppScope` | 동일 파일에 복수 인스턴스가 생기는 것을 방지 |
| Repository, AppVersion/ImageHandler provider | `AppScope` | 현재 Koin `single` 의미 보존 |
| `Circuit` | `AppScope` | factory set을 한 번 구성해 양 플랫폼에서 공유 |
| Presenter | unscoped assisted instance | Screen/Navigator 입력마다 Circuit이 생성 |
| Screen UiState/retained modal state | DI scope 아님 | Circuit retained state가 수명 관리 |
| Activity/Screen/Session graph | 도입하지 않음 | 계정/세션/서버가 없고 현재 제품 요구가 없음 |

`develop`의 `ActivityRetainedComponent`는 Presenter의 수명 요구가 아니라 Hilt codegen의 aggregation key로 사용됐다. 이를 Metro의 별도 Activity scope로 기계적으로 재현하지 않는다.

### 7.4 multi-module KMP 규칙

Metro는 KMP를 지원하지만 Native target에서 `@Contributes*`를 사용한 multi-module contribution에는 KT-75865 제약이 있다. 따라서 초기 이식은 다음처럼 보수적으로 구성한다.

- app graph와 scope는 `commonMain`에 둔다.
- 공통 binding은 common source set의 명시적 `@Provides`/`@Binds` 또는 graph에 명시적으로 포함한 binding container를 우선한다.
- `iosMain` 전용 `@Contributes*` aggregation에 의존하지 않는다.
- 플랫폼 객체는 `PlatformBindings` factory input으로 명시한다.
- Circuit factory contribution은 Android와 iOS KMP 컴파일을 모두 통과한 뒤 사용 범위를 확정한다.

## 8. Circuit factory 설계

Metro 1.3.2는 `metro { enableCircuitCodegen.set(true) }`로 Circuit factory를 생성하고 `Presenter.Factory`/`Ui.Factory` set에 기여할 수 있다. 이 기능은 Kotlin 2.3.20 이상이 필요하다.

- `@CircuitInject`는 Hilt component를 인자로 받는 기존 형태로 복사하지 않는다.
- Presenter의 nested `@AssistedFactory`에 Metro/Circuit이 요구하는 annotation을 적용한다.
- `Navigator`와 구체 Screen은 Circuit이 제공하는 assisted parameter다.
- repository 등 나머지 생성자 인자는 Metro가 주입한다.
- UI factory도 공통 composable에서 생성되도록 검증한다.
- `Circuit`은 factory set으로 AppGraph에서 한 번 구성한다.

`develop`의 `CircuitModule`은 설계 참조만 하고 파일 자체는 이식하지 않는다.

## 9. 테스트 이식 맵

| 기능 | `develop` Presenter 기준 | `main`에서 추가 보존할 기준 |
| --- | --- | --- |
| Splash | onboarding 완료→Home, 미완료→Onboarding | 최초 데이터 생성 책임과 기존 DB 보존 |
| Onboarding | 완료 flag 저장 후 Home | 중복 event 처리와 저장 완료 전 navigation 여부 |
| Complete | screen data 노출, 완료 ID 저장, save/share side effect, back pop | KMP URI와 Android/iOS image handler 연결 |
| Home | 최근 항목 load, 빈 목록 create, 5개 제한, 거절한 update 재노출 방지 | 기존 ViewModel 테스트의 편집/삭제/modal/capture 동작 |
| In-app update | 선택 업데이트 상태 전이 | Android UI만 실행, iOS presenter/state가 Play API를 참조하지 않음 |

Presenter 테스트는 Circuit 공식 `Presenter.test`/Turbine 패턴을 유지한다. fake/recording repository는 별도 파일로 유지하고, production repository 구현을 Presenter 테스트 파일 안에 만들지 않는다.

## 10. 후속 PR 실행 순서

### 3-A. KMP AGP 9 구조 및 toolchain 정렬

- `androidApp` 분리
- `composeApp`을 Android-KMP library plugin 구조로 전환
- Kotlin 2.3.21, AGP 9.3.0, Gradle 9.5.0을 우선 후보로 적용
- compileSdk 37 / targetSdk 36 정렬
- Android package/version/signing/baseline profile 동작 보존
- Android debug/release 및 iOS framework 빌드 검증

이 PR은 Metro를 추가하지 않는다. 모듈 구조와 toolchain 회귀를 분리해 확인한다.

### 3-B. Metro bootstrap

- Metro 1.3.2 plugin/runtime 추가
- `AppScope`, 최소 `AppGraph`, `PlatformBindings` 추가
- 기존 Koin graph는 그대로 두고 신규 bootstrap probe만 Metro가 소유
- Android/iOS에서 graph 생성 및 앱 기동 확인
- CI에 Android compile/test와 iOS framework compile 추가

### 4-A. Platform/Room/DataStore graph

- 플랫폼 factory와 DB/DataStore subgraph를 Metro로 이전
- Koin bridge로 동일 인스턴스 노출
- schema, DB 파일명, DataStore key, 재시작 후 데이터 확인

### 4-B. Repository graph

- 세 repository 구현/binding을 Metro로 이전
- 기존 Koin constructor binding 제거, accessor bridge로 대체
- repository/DAO/DataStore 단위 테스트 유지

### 5. Splash + Onboarding vertical slice

- 공통 Circuit Screen/Presenter/UI factory 이식
- ViewModel/Compose Navigation/Koin feature module 제거
- Android 강제 업데이트 UI는 플랫폼 경계에 유지
- 양 플랫폼 기동/분기/상태 저장 검증

### 6-A. Complete

- Screen parameter, Presenter, side effect, test 이식
- KMP image save/share 연결

### 6-B. Home 읽기

- 목록, 최근 항목, 빈 목록 생성, 완료 감지

### 6-C. Home 편집/modal

- 생성/편집/삭제, bottom sheet/dialog retained state

### 6-D. Home 플랫폼 effect

- Android 선택 업데이트
- Android/iOS 공유, 저장, capture

### 7. Composition root 정리

- Circuit과 모든 runtime object를 Metro가 소유
- Koin bridge/start/context/module/plugin/runtime 제거
- Android/iOS entry point에서 동일 AppGraph 사용

### 8. 통합 검증 및 main 일원화

- 전체 Android/iOS build/test/static check
- 기존 로컬 데이터 upgrade/restart 확인
- 내부 테스트 배포
- `main` merge 및 `develop` 역할 종료

## 11. 단계별 merge gate

모든 후속 PR은 최소 다음을 만족해야 한다.

- Android debug unit test 및 대상 Presenter test 통과
- Android debug APK와 signed release AAB 생성
- iOS simulator 또는 framework compile 통과
- ktlint/code style 통과
- graph에서 동일 DB/DataStore/repository 중복 생성 없음
- 기존 DB schema와 DataStore key 변경 없음
- 기능 PR은 Android/iOS 수동 회귀 체크 결과 기록

toolchain/DI 전환 PR은 기능 변경을 포함하지 않고, 기능 전환 PR은 라이브러리 대규모 업데이트를 포함하지 않는다.

## 12. 미결정 사항과 확정 시점

| 항목 | 현재 판단 | 확정 시점 |
| --- | --- | --- |
| Metro 정확한 버전 | 1.3.2 우선 | 3-B dependency resolution/build 결과 |
| Circuit 정확한 버전 | develop의 0.35.1 이상 호환 버전 | 3-A toolchain 및 3-B codegen spike |
| Android/iOS 앱 버전 | 배포 이력이 갈라져 별도 확인 필요 | 3-A에서 Play/App Store 기준 확인 후 결정 |
| `@Parcelize` 대체 | 공통 Screen compile/상태 복원 spike 필요 | 5단계 시작 전 |
| Contribution 사용 범위 | explicit binding/container 우선 | 3-B의 Native compile 결과 |
| Splash의 빈 Bandalart 생성 제거 | Home이 불변식을 보장할 때 제거 | 5단계 회귀 테스트 |

## 13. 2단계 완료 체크

- [x] `main` 기준 통합 브랜치 생성
- [x] Screen, ViewModel/Presenter, repository, DI binding 대응표 작성
- [x] 이식·보존·대체 후 제거·비범위 파일 구분
- [x] Koin/Metro 공존 방향과 객체 소유권 이전 순서 정의
- [x] Android/iOS 공통 AppGraph와 플랫폼 binding 경계 정의
- [x] 후속 PR 순서와 merge gate 정의

이 문서를 기준으로 3-A부터 시작하며, 전체 `develop` merge나 Hilt annotation 복사는 하지 않는다.
