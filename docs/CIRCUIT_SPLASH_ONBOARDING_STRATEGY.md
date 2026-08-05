# Circuit Splash·Onboarding 수직 슬라이스 전략

## 1. 목적

이 문서는 이슈 #182의 5단계인 Splash·Onboarding 전환 범위와 검증 기준을 구현 전에 고정한다.

- 기준 브랜치: `main` (`a23b3e133f0152a8bf7c50ae1a200c565c865a52`)
- 작업 브랜치: `refactor/circuit-splash-onboarding`
- 대상: Android, iOS 공통 UI와 상태 처리
- 목표: ViewModel + Compose Navigation + Koin feature module을 Circuit Presenter + Metro factory로 교체
- 비목표: Home·Complete Presenter 전환, NavStack 도입, Koin 완전 제거, 신규 기능 추가

## 2. 관찰된 현재 동작

### `main`

- `BandalartNavHost`가 Splash → Onboarding/Home → Complete/Home을 Compose Navigation으로 연결한다.
- Splash는 onboarding 완료 여부를 읽고, 미완료이면 빈 Bandalart를 생성한 뒤 Onboarding으로 이동한다.
- Home은 목록이 비어 있으면 Bandalart를 생성하므로 Splash의 데이터 생성 책임과 중복된다.
- Splash와 Onboarding은 KMP UI지만 상태 소유자는 Koin ViewModel이다.

### `develop` 참고 구현

- Splash와 Onboarding이 Circuit `Screen`, `Presenter`, `UiState/Event` 계약을 사용한다.
- Presenter 테스트가 화면 분기와 onboarding 완료 저장 동작을 검증한다.
- Splash UI에 Android Play 강제 업데이트 처리가 포함돼 있다.
- Hilt와 Android resource 기반 구현은 이식 대상이 아니다.

## 3. 결정

### 3.1 화면과 데이터 책임

- Splash는 강제 업데이트 완료 후 onboarding 완료 여부만 확인한다.
- onboarding 완료이면 Home으로, 미완료이면 Onboarding으로 이동한다.
- Splash에서 Bandalart를 생성하지 않는다. 빈 목록의 최초 생성 책임은 Home 하나로 통일한다.
- Onboarding은 완료 상태를 저장한 뒤 Home으로 이동한다.

### 3.2 과도기 내비게이션

이번 단계에서는 Circuit과 Compose Navigation이 아래 경계에서 공존한다.

```text
BandalartApp
  └─ Circuit BackStack
       ├─ SplashScreen
       ├─ OnboardingScreen
       └─ LegacyHomeScreen
            └─ Compose Navigation
                 ├─ Home
                 └─ Complete
```

- `SplashScreen`, `OnboardingScreen`, `LegacyHomeScreen`은 프로세스 복원을 위해 `ParcelableScreen`을 사용한다.
- 앱 root는 `rememberSaveableBackStack(SplashScreen)`으로 생성한다.
- Splash/Onboarding의 기존 Compose Navigation destination과 navigation class는 같은 단계에서 제거한다.
- `LegacyHomeScreen` UI factory는 Home을 시작점으로 하는 기존 NavHost를 렌더링한다.
- 6단계에서 Home·Complete를 Circuit으로 옮기면 `LegacyHomeScreen`과 기존 NavHost를 함께 제거한다.
- 기존 BackStack을 유지하며 NavStack 전환은 이 작업에 포함하지 않는다.

### 3.3 Metro와 Circuit codegen

- Circuit은 `0.35.1`, Metro는 `1.1.1`, Kotlin은 `2.4.10`을 사용한다. Circuit 0.35.1 Native artifact의 ABI가 Kotlin 2.4이므로 기존 2.3.21은 유지할 수 없다.
- Circuit 0.35.1이 제공하지 않는 Intel simulator용 `iosX64` target은 제거한다. `iosArm64`와 `iosSimulatorArm64`는 유지한다.
- Splash/Onboarding feature 모듈에서 Metro 플러그인과 `enableCircuitCodegen`을 활성화한다.
- Presenter는 unscoped assisted instance로 생성한다. `Navigator`는 assisted input, repository는 Metro graph input이다.
- UI와 Presenter factory는 `@CircuitInject(Screen::class, AppScope::class)`로 app graph의 factory set에 기여한다.
- app graph가 `Set<Presenter.Factory>`, `Set<Ui.Factory>`로 app-scoped `Circuit`을 한 번 생성한다.
- Android와 iOS compile에서 multi-module factory aggregation을 먼저 검증한다.
- Native aggregation 제약이 확인되면 feature 계약을 바꾸지 않고 `composeApp`의 명시적 factory binding으로만 대체한다.

### 3.4 Android 강제 업데이트 경계

- Play `AppUpdateManager`, `AppUpdateInfo`, `ActivityResultLauncher` 등 Android 타입을 공통 Screen/Presenter 상태에 노출하지 않는다.
- 공통 Splash UI는 플랫폼 effect를 호출하고, effect가 완료됐을 때 Presenter에 `CheckOnboarding` 이벤트를 보낸다.
- Android actual은 즉시 업데이트가 허용되면 시스템 업데이트 UI를 실행하고, 이미 진행 중인 업데이트는 재개한다.
- 업데이트가 없거나 허용되지 않거나 확인에 실패하면 앱 진입을 막지 않고 onboarding 확인으로 진행한다.
- iOS actual은 Play 업데이트 단계가 없으므로 즉시 onboarding 확인으로 진행한다.
- 시스템 업데이트 UI의 edge-to-edge 영역은 앱이 직접 렌더링하는 화면이 아니므로 이번 범위에서 수정하지 않는다.

## 4. 구현 순서

1. Circuit/Metro Gradle 설정과 공통 Screen 계약을 추가한다.
2. Presenter와 Presenter 테스트를 작성해 분기·저장 동작을 고정한다.
3. 기존 KMP Splash/Onboarding UI를 Circuit UiState/Event에 연결한다.
4. Android/iOS 강제 업데이트 effect를 추가한다.
5. app graph에 Circuit과 factory set을 연결한다.
6. `BandalartApp`을 Circuit root로 바꾸고 `LegacyHomeScreen` 어댑터를 연결한다.
7. Splash/Onboarding ViewModel, Koin module, Compose Navigation destination을 제거한다.
8. Android/iOS compile과 대표 테스트를 검증하고 트러블슈팅 문서에 결과를 기록한다.

## 5. 테스트 및 완료 기준

### 자동 검증

- Splash Presenter
  - onboarding 완료 → Home root로 교체
  - onboarding 미완료 → Onboarding root로 교체
  - 동일 이벤트 중복 처리로 화면 전환이 중복되지 않음
- Onboarding Presenter
  - 시작 이벤트 → 완료 상태 저장 → Home root로 교체
  - 저장이 끝나기 전에 이동하지 않음
- 기존 ViewModel 테스트에만 있던 의미 있는 회귀 사례를 Presenter 테스트로 이전
- Metro graph가 repository와 Circuit을 생성하고 Android/iOS target에서 factory set을 합성
- Android host test, 공통 테스트, Android Kotlin compile, iOS simulator framework link가 통과

### 수동 검증

- Android 신규 설치: Splash → Onboarding → Home
- Android onboarding 완료 상태: Splash → Home
- Android 강제 업데이트 대상/비대상과 진행 중 업데이트 재개
- iOS 신규 설치 및 완료 상태에서 동일 분기
- Home에서 Complete로 이동하고 다시 Home으로 돌아오는 기존 흐름
- 앱 재생성 후 현재 root screen 복원

### 구현 검증 결과

- `:feature:splash:testAndroidHostTest`, `:feature:onboarding:testAndroidHostTest`, `:composeApp:testAndroidHostTest` 통과
- 전체 `allTests`와 `:androidApp:testDebugUnitTest` 통과
- 변경 모듈 Spotless/Detekt 및 `:androidApp:lintDebug` 통과
- `:composeApp:compileAndroidMain` 통과
- `:composeApp:linkDebugFrameworkIosSimulatorArm64` 통과
- Android는 Circuit의 기본 root back dispatcher를 유지하고, iOS는 root pop을 no-op으로 명시
- 실제 기기에서 강제 업데이트, onboarding 분기, Home/Complete 회귀는 PR merge 전 수동 확인 필요

## 6. 롤백 경계

- repository/DB/DataStore 소유권은 이전 단계의 Metro graph를 그대로 사용하며 저장 형식을 바꾸지 않는다.
- feature별 ViewModel과 Koin module 제거는 Circuit root가 양 플랫폼에서 컴파일된 뒤 수행한다.
- aggregation 실패 시 명시적 factory binding으로 후퇴하며 Koin ViewModel 구조로 되돌리지 않는다.
- Home·Complete 회귀가 발생하면 `LegacyHomeScreen` 어댑터 내부만 수정하고 Splash/Onboarding 계약은 유지한다.

## 7. 참고 자료

- [Circuit code generation](https://slackhq.github.io/circuit/docs/code-gen/)
- [Circuit Screen](https://slackhq.github.io/circuit/docs/screen/)
- [Circuit navigation](https://slackhq.github.io/circuit/docs/navigation/)
- [Circuit testing](https://slackhq.github.io/circuit/docs/testing/)
- [Metro Circuit integration 1.1.1](https://zacsweers.github.io/metro/1.1.1/circuit/)
- [Android Play in-app updates](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java)

## 8. 문서 적용 메모

현재 `main`에는 `AGENTS.md`가 참조하는 `.claude/CODING.md`, `.claude/WORKFLOW.md`가 없다. 이번 작업은 루트 `AGENTS.md`, 기존 소스의 코드 스타일, `docs/CIRCUIT_METRO_KMP_MIGRATION_MAP.md`를 기준으로 수행한다.
