# Metro Composition Root 정리 전략

## 1. 목적

이 문서는 이슈 #182의 7단계인 Android/iOS composition root 정리와 Koin runtime 제거 범위를 구현 전에 고정한다.

- 기준 브랜치: `main` (`1f5e69805fdd76930bc86d3fd71b5ce5b7b7f708`)
- 작업 브랜치: `refactor/metro-composition-root`
- 선행 작업: PR #198 Home Circuit runtime 전환
- 목표: 양 플랫폼에서 `AppGraph`와 Circuit을 Metro만 생성·소유하도록 만들고 Koin bridge/start/context/dependency를 제거한다.
- 대상 플랫폼: Android, iOS

## 2. 현재 상태

Repository, DB/DataStore, 플랫폼 provider와 모든 Circuit Presenter/UI factory의 생성 책임은 이미 Metro로 이전됐다. Koin은 새 객체를 만들지 않고 Metro accessor를 다시 노출하는 bridge와 composition wrapper로만 남아 있다.

```text
Android Application / iOS MainViewController
        ├── Metro AppGraph
        │    ├── DB / DAO / DataStore
        │    ├── Repository / platform provider
        │    └── Circuit + Presenter/UI factories
        └── Koin runtime
             ├── metroKoinBridgeModule(AppGraph accessor 재노출)
             └── KoinContext
```

Home runtime까지 Circuit으로 전환됐기 때문에 Koin bridge를 조회하는 실제 화면이나 ViewModel은 더 이상 없다. 이 단계는 기능 객체를 재이전하지 않고 남은 우회 경로만 제거한다.

## 3. 완료 구조

```text
Android
  BandalartApplication
    └── createAndroidAppGraph(application)
          └── MainActivity → BandalartApp(appGraph)

iOS
  MainViewController
    └── createIosAppGraph()
          └── ComposeUIViewController → BandalartApp(appGraph)

BandalartApp
  └── CircuitCompositionLocals(appGraph.circuit)
        └── Splash → Onboarding/Home → Complete
```

- composition root는 플랫폼 entry point와 공통 `BandalartApp` 사이의 명시적 `AppGraph` 전달이다.
- 전역 service locator나 top-level mutable graph를 추가하지 않는다.
- `AppGraph`와 app-scoped 객체의 생성 횟수는 플랫폼 앱/controller 인스턴스당 한 번이다.

## 4. Android 책임

`BandalartApplication`은 다음만 수행한다.

1. `createAndroidAppGraph(this)`로 graph를 한 번 생성하고 프로세스 수명 동안 보관한다.
2. Napier, Firebase, cmptoast처럼 DI와 무관한 플랫폼 SDK를 초기화한다.
3. `MainActivity`가 같은 `appGraph`를 `BandalartApp`에 전달하게 한다.

다음 항목은 제거한다.

- `initKoin(appGraph)` 호출
- `androidContext(...)` 설정
- `koin-android` 의존성

Application/Activity 사이에 새 singleton holder나 `CompositionLocal<AppGraph>`를 만들지 않는다.

## 5. iOS 책임과 graph 수명

`MainViewController()`는 `createIosAppGraph()`를 한 번 호출하고 반환하는 `ComposeUIViewController`의 content closure가 같은 인스턴스를 캡처한다.

- Koin 초기화를 위한 `ComposeUIViewController.configure` 블록을 제거한다.
- controller가 유지되는 동안 graph와 DB/DataStore handle도 함께 유지된다.
- Swift 전역 singleton이나 Kotlin top-level mutable graph를 추가하지 않는다.
- `MainViewController()`가 다시 생성되면 별도 앱 UI 인스턴스로 보고 새 graph를 만든다.

현재 앱의 단일 root controller 구조에서는 이 수명이 Android Application 수명과 제품 동작상 동등하다.

## 6. 공통 Compose root

`BandalartApp(appGraph)`에서 `KoinContext` wrapper를 제거하고 다음 구조는 유지한다.

- `key(appGraph)`
- `BandalartTheme`
- root `SplashScreen` BackStack
- `CircuitCompositionLocals(appGraph.circuit)`
- snackbar `CompositionLocal`
- `NavigableCircuitContent`

Koin 제거를 계기로 navigation, snackbar 위치, theme나 화면 UI 구조를 변경하지 않는다.

## 7. 제거 대상

### 소스

- `composeApp/.../di/initKoin.kt`
- `composeApp/.../di/metro/MetroKoinBridgeModule.kt`
- Android `initKoin`/`androidContext` 호출과 import
- iOS `initKoin` 호출과 import
- `BandalartApp`의 `KoinContext`
- `AppGraphTest.koinBridgeExposesMetroGraphInstances`

### 의존성/catalog

- `androidApp`의 `libs.koin.android`
- `composeApp`의 `libs.koin.android`, `libs.koin.core`, `libs.koin.compose`
- 더 이상 참조되지 않는 Koin version과 catalog alias 전체
- Koin 3.5.6을 전이 의존성으로 포함하는 미사용 Kotzilla SDK/plugin/config

### 문서

- README의 현재 DI 설명을 Koin에서 Metro로 갱신한다.
- migration map의 7단계 결과와 troubleshooting을 구현 후 반영한다.

역사적 migration 문서에서 과거 Koin 구조를 설명하는 문구는 삭제하지 않는다. 현재 상태와 완료 여부만 갱신한다.

## 8. 유지 대상

- `AppGraph`의 DB/DAO/DataStore/repository/provider accessor
- `PlatformBindings`, Android/iOS graph factory
- Metro `AppScope`와 Circuit factory aggregation
- DB schema, 파일명, DataStore key와 repository 계약
- Firebase, Napier, cmptoast 플랫폼 초기화
- Android 강제/선택 업데이트 동작
- 기존 Circuit BackStack

Accessor는 Koin bridge 때문에 시작됐더라도 graph singleton과 platform factory 검증에 사용되므로 이번 단계에서 억지로 축소하지 않는다.

## 9. 비범위

- NavStack 전환
- 설정 화면, 다크 모드 등 신규 기능
- DB schema/DataStore key 변경
- Circuit/Metro/Kotlin/AGP 추가 버전업
- CI workflow 병렬화나 캐시 최적화
- baseline profile 재생성
- iOS SwiftUI 구조 변경

CI 병목 개선은 composition root 제거와 독립적인 workflow 작업으로 분리한다.

## 10. 테스트 전략

### Graph

- Android `createAndroidAppGraph`가 app-scoped 객체를 같은 인스턴스로 반환
- Circuit에 Splash, Onboarding, Home, Complete Presenter/UI factory가 모두 존재
- Koin bridge test 제거 후 graph test가 Koin 없이 compile/통과

### Entry point compile

- Android Application/Activity가 Koin import 없이 동일 `AppGraph`를 전달
- iOS `MainViewController`가 Koin configure 없이 framework compile
- 공통 `BandalartApp`이 Koin Compose runtime 없이 compile

### 정적 검증

- 프로덕션/테스트 소스와 version catalog에서 `org.koin`, `libs.koin`, `initKoin`, `metroKoinBridgeModule`, `KoinContext` 0건
- `androidApp`과 `composeApp` dependency graph에 Koin artifact 0건
- `AppGraph` 생성 지점은 Android/iOS 각각 한 곳

## 11. 검증 gate

- `:composeApp:testAndroidHostTest`
- 변경 모듈 Detekt와 변경 파일 Spotless
- Android Application/composeApp Kotlin compile
- `:composeApp:compileKotlinIosSimulatorArm64`
- PR CI 전체 unit test
- PR CI Android Lint
- PR CI Android assemble와 iOS Simulator Arm64 framework link
- Android/iOS 앱 기동 후 Splash → Onboarding/Home, Home → Complete 확인
- 기존 로컬 데이터가 동일하게 열리는지 확인

전체 Android/iOS build와 수동 기기 검증은 기존 정책대로 PR CI와 사용자 수동 gate에서 수행한다.

## 12. 구현 순서

1. Koin bridge test를 제거하고 Metro graph/factory 검증을 유지한다.
2. Android/iOS entry point에서 Koin 초기화를 제거한다.
3. 공통 `BandalartApp`에서 `KoinContext`를 제거한다.
4. `initKoin`, `MetroKoinBridgeModule` 파일을 삭제한다.
5. 모듈 의존성과 version catalog의 Koin 항목을 제거한다.
6. README와 migration map/troubleshooting을 갱신한다.
7. 정적 참조 0건, 대상 test/compile/static check를 검증한다.

## 13. 중단 조건

- Koin을 조회하는 미발견 runtime 경로가 존재함
- Android/iOS 중 한 플랫폼에서 AppGraph 수명이 entry point보다 짧아짐
- DB/DataStore가 graph 밖에서 추가 생성됨
- 제거를 위해 새로운 DI framework나 전역 service locator가 필요함
- 기존 데이터 계약 변경이 필요함

이 경우 Koin 제거 범위를 확장하지 않고 실제 조회 지점과 객체 수명을 문서에 추가한 뒤 설계를 다시 확인한다.

## 14. 롤백 경계

- 이 PR은 객체 생성 구현을 바꾸지 않고 composition root의 Koin wrapper/bridge만 제거한다.
- DB/DataStore/repository와 Circuit factory는 동일 Metro graph를 계속 사용한다.
- PR 단위 revert 시 기존 bridge와 KoinContext가 함께 복구된다.
- Android/iOS 중 한 플랫폼만 Koin을 복구하는 부분 롤백은 허용하지 않는다.

## 15. 참고 문서

- [Circuit + Metro KMP 이식 맵](CIRCUIT_METRO_KMP_MIGRATION_MAP.md)
- [Metro 부트스트랩 전략](METRO_BOOTSTRAP_STRATEGY.md)
- [Metro Platform/Room/DataStore graph 전략](METRO_PLATFORM_DATA_GRAPH_STRATEGY.md)
- [Circuit Home 런타임 전환 전략](CIRCUIT_HOME_RUNTIME_MIGRATION_STRATEGY.md)
- [KMP Metro 마이그레이션 트러블슈팅](KMP_METRO_MIGRATION_TROUBLESHOOTING.md)

## 16. 구현 결과

- Android `BandalartApplication`이 Metro `AppGraph`만 생성하고 `MainActivity`에 동일 인스턴스를 전달한다.
- iOS `MainViewController`가 Koin configure 없이 Metro graph를 생성해 `BandalartApp`에 전달한다.
- 공통 `BandalartApp`에서 `KoinContext`를 제거하고 Circuit composition 구조를 유지했다.
- `initKoin`, `MetroKoinBridgeModule`과 bridge test를 제거했다.
- Koin version/catalog/module 의존성을 제거했다.
- Kotzilla SDK가 Koin 3.5.6을 전이로 포함하는 것을 dependency insight에서 확인해 미사용 SDK/plugin/config도 함께 제거했다.
- 프로덕션/테스트 소스와 version catalog의 Koin/Kotzilla 참조가 0건이다.
- Android debug runtime dependency graph에서 `io.insert-koin` artifact가 0건이다.
- composeApp graph test, Android Kotlin compile, iOS Simulator Arm64 compile, composeApp Detekt와 Spotless가 통과했다.
- `androidApp`에는 Spotless task가 구성돼 있지 않아 Android entry point는 compile과 diff check로 검증했다.
- 전체 unit test, Android Lint/build와 iOS framework link는 PR CI에서 확인한다.
