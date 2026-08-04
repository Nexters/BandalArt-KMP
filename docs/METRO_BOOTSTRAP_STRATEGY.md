# Metro 부트스트랩 전략

## 목적

이 문서는 이슈 #182의 3단계인 Metro 부트스트랩 작업 범위를 고정한다. 기능 객체의 생성 책임은 아직 Koin에 두고, Android와 iOS가 공유하는 최소 Metro graph의 컴파일·생성·수명 경계만 먼저 검증한다.

## 기준점

- 기준 브랜치: `main`
- 기준 리비전: `38dbfe2a84f65cf665d1c42005d0afb71509ef4a`
- 작업 브랜치: `refactor/metro-bootstrap`
- Kotlin: 2.3.21
- AGP / Gradle: 9.3.0 / 9.5.0
- Compose Multiplatform: 1.10.3
- Android SDK: compileSdk 37 / targetSdk 36
- 기존 DI: Koin 4.1.0-Beta5

## Metro 버전 결정

Metro 1.1.1을 사용한다.

- 1.2.0부터 최신 1.4.0까지는 compiler 호환성 표에 Kotlin 2.3.21을 포함하지만, 실제 iOS runtime KLIB가 Kotlin 2.4 ABI로 배포돼 Kotlin/Native 2.3.21에서 읽을 수 없었다.
- 1.1.1은 Kotlin 2.3 계열로 빌드된 runtime과 현재 toolchain을 함께 유지할 수 있는 최신 안정 릴리스다.
- Gradle 플러그인을 적용하면 runtime과 Kotlin compiler plugin 연결이 함께 구성된다.
- 이번 단계에서는 Circuit codegen과 contribution aggregation을 활성화하지 않는다.

공식 참고 자료:

- [Metro 1.1.1 릴리스](https://github.com/ZacSweers/metro/releases/tag/1.1.1)
- [설치](https://zacsweers.github.io/metro/1.1.1/installation/)
- [Kotlin compiler 호환성](https://zacsweers.github.io/metro/1.1.1/compatibility/)
- [Dependency graph](https://zacsweers.github.io/metro/1.1.1/dependency-graphs/)

## 포함 범위

1. version catalog에 Metro 1.1.1 Gradle 플러그인을 추가한다.
2. 공통 UI와 Android/iOS entry point를 가진 `composeApp`에만 Metro 플러그인을 적용한다.
3. `commonMain`에 app scope의 최소 `AppGraph`를 만든다.
4. graph factory 입력으로 `PlatformBindings` 경계를 만든다.
5. Metro가 생성하는 app-scoped bootstrap probe를 graph accessor로 노출한다.
6. Android `Application`과 iOS `MainViewController`가 각각 플랫폼 binding으로 graph를 한 번 생성한다.
7. 생성한 graph를 `BandalartApp`에 명시적으로 전달해 composition root 수명에 연결한다.
8. bootstrap probe 생성과 scope 동일성을 Android host test로 고정한다.
9. 기존 CI 명령으로 Android test/lint/build와 iOS framework link를 검증한다.

## 제외 범위

- Room, DataStore, repository, ViewModel의 Metro binding 전환
- Koin module, `startKoin`, `KoinContext` 제거 또는 수정
- Koin이 생성하는 객체를 Metro graph에 입력하거나 Metro가 Koin을 조회하는 bridge
- Circuit 의존성, Screen, Presenter, factory codegen 추가
- Android/iOS 플랫폼 provider 구현 변경
- 화면 동작, 내비게이션, 디자인 변경
- 신규 Activity, Screen, Session scope 도입

## graph 설계

```text
Android BandalartApplication              iOS MainViewController
            │                                      │
            ├── AndroidPlatformBindings            ├── IosPlatformBindings
            │                                      │
            └──────────────┬───────────────────────┘
                           ▼
                   AppGraph.Factory
                           │
                           ▼
                 AppGraph(AppScope)
                           │
                           └── MetroBootstrapProbe

Koin composition root ── 기존 경로 그대로 유지
```

`PlatformBindings`는 이번 단계에서 플랫폼 경계를 표시하는 빈 계약이다. 다음 4단계에서 기존 DB/DataStore/AppVersion/ImageHandler factory를 이 계약에 추가한다. 지금부터 실제 객체를 옮기면 Koin과 Metro가 같은 singleton을 중복 생성할 위험이 있으므로 추가하지 않는다.

`MetroBootstrapProbe`는 제품 기능을 갖지 않는다. graph factory 입력이 정상 해석되고 app scope가 동일 인스턴스를 반환하는지만 검증한 뒤, 최종 composition root 정리 단계에서 제거할 수 있다.

## 수명과 소유권

- Android: `BandalartApplication`이 `AppGraph`를 프로세스 수명 동안 한 번 소유한다.
- iOS: `MainViewController` 생성 시 graph를 만들고 root composable이 controller 수명 동안 참조한다.
- 공통: `BandalartApp(appGraph)`는 graph를 명시적으로 받지만 아직 Koin으로부터 제품 의존성을 조회한다.
- 금지: 전역 mutable graph holder, Metro에서 Koin 조회, Koin에서 bootstrap probe 재생성.

## 구현 순서

1. Metro 플러그인 버전과 Kotlin 2.3.21 Android/iOS 호환성을 고정한다.
2. `composeApp`에 플러그인을 적용하고 의존성 해석만 확인한다.
3. 공통 graph, factory input, bootstrap probe를 추가한다.
4. Android와 iOS entry point에서 graph를 생성한다.
5. root composable에 graph identity를 연결한다.
6. bootstrap probe test를 추가한다.
7. Android/iOS 전체 검증 후 이 문서에 실제 결과를 기록한다.

## 검증 계획

```shell
./gradlew :composeApp:testAndroidHostTest
./gradlew allTests :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :androidApp:bundleRelease -x :androidApp:uploadCrashlyticsMappingFileRelease
```

정적 확인:

- Koin module과 `initKoin` 변경 없음
- Metro graph에서 기존 DB/DataStore/repository binding 없음
- Android/iOS가 같은 공통 `AppGraph` 타입을 생성
- bootstrap probe accessor가 app scope 안에서 같은 인스턴스 반환
- DB schema와 DataStore key 변경 없음

## 완료 조건

- Metro 1.1.1 compiler plugin과 runtime이 Kotlin 2.3.21의 Android/iOS compilation에서 동작한다.
- Android와 iOS entry point가 공통 `AppGraph`를 생성한다.
- 앱의 기존 Koin 기반 기능과 UI 흐름은 변경되지 않는다.
- bootstrap probe test, Android unit test, Android Lint, Android debug/release, iOS framework link가 통과한다.
- 다음 단계에서 `PlatformBindings`에 기존 core factory를 추가할 수 있는 경계가 준비된다.

## 검증 결과

2026-08-04 기준 아래 통합 검증을 통과했다.

```shell
./gradlew allTests \
  :androidApp:testDebugUnitTest \
  :androidApp:lintDebug \
  :androidApp:assembleDebug \
  :composeApp:linkDebugFrameworkIosSimulatorArm64 \
  :androidApp:bundleRelease \
  -x :androidApp:uploadCrashlyticsMappingFileRelease \
  --no-daemon \
  --stacktrace
```

- 전체 테스트와 Metro `AppGraph` Android host test 통과
- Android Lint와 debug APK 생성 통과
- iOS Simulator arm64 debug framework link 통과
- R8이 적용된 서명 release AAB 생성 통과
- 기존 Koin module과 `initKoin` 변경 없음
- Metro graph에 기존 DB, DataStore, repository binding 없음

Gradle 10에서 제거될 deprecated API 경고와 기존 Android Manifest의 불필요한 `tools:replace` 경고는 남아 있지만, 이번 Metro 변경에서 추가된 빌드 오류는 없다.

## 롤백

이 단계는 제품 객체의 소유권을 변경하지 않는다. 문제가 생기면 Metro 플러그인 선언, 공통 graph 파일, 플랫폼별 bootstrap 파일, entry point의 graph 전달만 되돌리면 기존 Koin 앱으로 완전히 복귀한다.
