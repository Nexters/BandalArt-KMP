# KMP 테스트 소스셋과 Circuit Presenter 테스트 가이드

- 작성 기준: 2026-08-06, `docs/kmp-testing-guide`
- 관련 이슈: [#209](https://github.com/Nexters/BandalArt-KMP/issues/209)
- 대상 구성: AGP 9 Android-KMP plugin, Kotlin Multiplatform, Circuit 0.35.1, JUnit 5

## 결론

- `androidHostTest`는 프로젝트가 임의로 붙인 이름이 아니라 `com.android.kotlin.multiplatform.library`가 제공하는 공식 Android 호스트 테스트 source set이다.
- KMP 모듈 테스트는 모두 `androidHostTest`에 있고, Android application module에는 별도 `androidApp/src/test` JVM 테스트가 있다. `commonTest`, `androidDeviceTest`, iOS test source set은 아직 없다.
- 현재 JUnit 5, MockK, Robolectric 기반 테스트를 억지로 `commonTest`로 옮기지 않는다. 공통 테스트의 플랫폼 중복 실행 가치가 생길 때 `kotlin.test`와 multiplatform test dependency로 별도 전환한다.
- Circuit Presenter 테스트는 `Presenter.test()`가 Molecule과 Turbine을 내부에서 조합한다. 프로젝트 코드가 Molecule API를 직접 import하지 않으므로 `molecule-runtime`을 직접 선언할 필요가 없다.
- CI의 `allTests`는 현재 존재하는 11개 KMP 모듈의 `testAndroidHostTest`를 모두 실행한다. `androidApp`의 JVM 단위 테스트는 별도 `:androidApp:testDebugUnitTest`로 실행한다.

## Source set 선택 기준

| Source set | 실행 위치 | 사용할 때 | 현재 예 |
| --- | --- | --- | --- |
| `commonTest` | 구성된 각 KMP target | 플랫폼 API 없이 동일 계약을 Android와 iOS 등에서 검증할 가치가 있을 때 | 없음 |
| `androidHostTest` | 로컬 JVM | JUnit 5, MockK, Robolectric, Android API 또는 Android target 실제 구현을 사용할 때 | Repository, Room, DataStore, Presenter, Metro graph |
| `androidDeviceTest` | Android emulator/device | 실제 framework, lifecycle, UI, 권한, 시스템 integration이 필요할 때 | 없음 |
| iOS test source set | Kotlin/Native simulator/device | iOS `actual` 구현이나 Native에서만 발생하는 동작을 검증할 때 | 없음 |

AGP Android-KMP plugin에서는 host/device test가 기본 비활성이다. 이 저장소의 `KmpAndroidPlugin`은 `src/androidHostTest` 디렉터리가 있는 모듈에만 `withHostTest`를 활성화한다. 공식 migration 표도 기존 `src/test`를 `src/androidHostTest`, `src/androidTest`를 `src/androidDeviceTest`로 옮기도록 안내한다.

- [Android Developers: Android-KMP plugin 설정과 test source set](https://developer.android.com/kotlin/multiplatform/plugin)
- [Kotlin Multiplatform: 공통·플랫폼 테스트 실행](https://kotlinlang.org/docs/multiplatform/multiplatform-run-tests.html)

### `commonTest`로 옮길지 판단하는 질문

아래 질문이 모두 `예`일 때만 이동을 고려한다.

1. 테스트 대상과 fixture가 Android/JVM API를 사용하지 않는가?
2. JUnit 5 전용 API, MockK JVM, Robolectric, `java.io` 등을 `kotlin.test`와 multiplatform dependency로 바꿀 수 있는가?
3. 같은 테스트를 iOS/Native에서도 실행해 실제 회귀를 잡을 수 있는가?
4. Native test 시간과 유지비보다 플랫폼 간 계약을 검증하는 가치가 큰가?

단순히 production 코드가 `commonMain`에 있다는 이유만으로 테스트도 `commonTest`로 옮기지는 않는다. 현재 순수 로직 후보인 `ThemeModeTest`, `InAppUpdatePolicyTest`도 2~3개의 작은 JUnit 5 테스트라서, 이들만 이동하면 runner가 이원화되고 iOS 전용 회귀를 추가로 잡지 못한다. 공통 도메인 계약 테스트가 늘어날 때 함께 전환한다.

## 현재 모듈 감사 결과

| 모듈 | `androidHostTest` 내용 | 현재 위치 판단 |
| --- | --- | --- |
| `composeApp` | Metro `AppGraph` 생성과 factory 연결, Robolectric/Application | Android host 유지 |
| `core:common` | Android color 변환, 날짜 포맷, 인앱 업데이트 정책 | Android color와 JUnit 5 때문에 유지. 정책 테스트는 향후 공통 후보 |
| `core:data` | Repository와 DAO/DataStore 협력, MockK | Android host 유지 |
| `core:database` | Room DAO/Robolectric, Android database path | Android host 유지 |
| `core:datastore` | JVM temp file 기반 DataStore와 Android path | Android host 유지 |
| `core:domain` | `ThemeMode` 순수 로직 | 향후 공통 후보지만 지금은 유지 |
| `core:ui` | Fluent Emoji catalog, category/filter와 최근 항목 조합 | Android host 유지. UI restoration harness는 아직 없음 |
| `feature:complete` | Circuit Presenter와 repository recording fake | Android host 유지 |
| `feature:home` | Circuit Presenter 상태·event·navigation, Turbine helper | Android host 유지 |
| `feature:onboarding` | Circuit Presenter 중복 입력과 root navigation | Android host 유지 |
| `feature:splash` | Circuit Presenter 초기 routing | Android host 유지 |

현재 KMP 모듈에는 `commonTest`, `androidDeviceTest`, `src/test`, `src/androidTest`, iOS test 디렉터리가 없다. Android application module에는 `androidApp/src/test`가 있다. 이번 문서 갱신에서는 테스트 파일을 이동하지 않는다.

## Circuit, Molecule, Turbine의 관계

Circuit 공식 문서에서 `Presenter.test()`는 `presenterTestOf()`의 단축 API이며, Molecule로 composable Presenter를 실행하고 Turbine으로 방출된 `CircuitUiState`를 검증할 수 있게 한다.

```kotlin
runTest {
    presenter.test {
        val state = awaitItem()
        state.eventSink(Event.Submit)
        assertEquals(expected, awaitItem())
    }
}
```

- [Circuit 공식 Testing 문서](https://slackhq.github.io/circuit/docs/testing/)

프로젝트가 소비 중인 `circuit-test:0.35.1` Gradle Module Metadata를 확인한 결과는 다음과 같다.

- common metadata와 Android/JVM/iOS/JS/Wasm/macOS 변형을 게시한다.
- `molecule-runtime:2.2.0`과 `turbine:1.2.1`을 전이 의존성으로 제공한다.
- 따라서 `Presenter.test()`만 쓰는 모듈은 `libs.circuit.test`만 직접 선언하면 된다.
- `feature:home`은 테스트 코드가 `app.cash.turbine.ReceiveTurbine`을 직접 import하므로 `libs.turbine`을 직접 선언한 현재 구성이 맞다.
- 프로젝트 production/test 코드에는 `app.cash.molecule` 직접 import가 없다. 직접 `moleculeFlow`, `launchMolecule` 등을 사용하기 전에는 Molecule dependency를 추가하지 않는다.

Circuit Presenter 테스트를 iOS source set에서도 실행하는 것은 기술적으로 가능하다. 다만 현재 Presenter가 공통 코드에 있고 Android host test가 상태 전이와 navigation 계약을 이미 검증하며, iOS `actual` 구현을 Presenter가 직접 사용하지 않는다. 같은 테스트를 Native에서 중복 실행하는 비용보다 이점이 작으므로 지금은 추가하지 않는다. iOS platform adapter나 Native 전용 상태 분기가 생기면 해당 계약만 iOS test로 추가한다.

## 로컬 실행 명령

전체 host test와 Android app JVM test:

```bash
./gradlew allTests :androidApp:testDebugUnitTest
```

특정 KMP 모듈의 host test:

```bash
./gradlew :feature:home:testAndroidHostTest
./gradlew :core:database:testAndroidHostTest
```

Circuit/Metro 변경의 대표 회귀 검사:

```bash
./gradlew \
  :feature:complete:testAndroidHostTest \
  :feature:home:testAndroidHostTest \
  :feature:onboarding:testAndroidHostTest \
  :feature:splash:testAndroidHostTest \
  :composeApp:testAndroidHostTest
```

`androidDeviceTest`는 아직 활성화하지 않았으므로 이 저장소에 대응 task가 없다. `baselineprofile`은 앱의 unit test 체계와 별개의 계측 모듈이며, 실제 기기/에뮬레이터 기반 성능 검증이 필요할 때 해당 workflow에서 실행한다.

## CI 실행 범위

`.github/workflows/android-ci.yml`의 Unit tests job은 macOS/JDK 21에서 다음 명령을 실행한다.

```bash
./gradlew allTests :androidApp:testDebugUnitTest --stacktrace
```

현재 `allTests`는 아래 task를 실행한다.

```text
:composeApp:testAndroidHostTest
:core:common:testAndroidHostTest
:core:data:testAndroidHostTest
:core:database:testAndroidHostTest
:core:datastore:testAndroidHostTest
:core:domain:testAndroidHostTest
:core:ui:testAndroidHostTest
:feature:complete:testAndroidHostTest
:feature:home:testAndroidHostTest
:feature:onboarding:testAndroidHostTest
:feature:splash:testAndroidHostTest
```

`androidApp:testDebugUnitTest`는 보상형 광고 SDK callback 순서를 검증하는 `RewardedAdCallbackCoordinatorTest`를 실행한다. Android lint/build와 iOS framework build는 각각 별도 CI job이며 테스트 실행을 대체하지 않는다.

### `androidApp`에 `bandalart.kotest`를 적용한 이유

보상형 광고 작업에서 `androidApp/src/test`에 처음 JUnit 5 테스트가 생겼다. 기존 `bandalart.kotest` convention plugin을 재사용한 직접적인 이유는 Android application module의 `Test` task에 `useJUnitPlatform()`과 공통 결과 logging을 적용해 이 테스트를 CI에서 발견·실행하기 위해서다.

현재 테스트 자체는 Kotest spec/assertion DSL을 사용하지 않고 JUnit Jupiter `@Test`와 assertion을 사용한다. `kotest-runner-junit5-jvm`도 convention plugin이 함께 추가하지만 이 테스트가 요구하는 API는 아니다. plugin은 모든 실행에서 테스트 결과를 다시 보도록 `outputs.upToDateWhen { false }`도 설정한다. 따라서 “광고 로직 때문에 Kotest가 필요했다”는 뜻은 아니다. plugin 이름과 runner dependency가 실제 책임보다 넓은 것은 기존 build-logic의 기술 부채다. 다음에 test convention을 정리할 때 `bandalart.junit5` 같은 중립적인 plugin으로 분리할 수 있지만, 이번 문서 정리에서 빌드 구성을 함께 바꾸지는 않는다.

문서만 변경한 PR은 workflow의 `paths-ignore: "**/*.md"` 조건으로 Android CI가 시작되지 않는다. 문서 PR에서는 링크와 저장소 구성 대조가 검증 기준이고, 코드나 Gradle 구성이 함께 바뀌면 전체 CI를 실행한다.

## 새 테스트 추가 규칙

1. 먼저 테스트하려는 계약이 공통인지 플랫폼 전용인지 정한다.
2. 현재 runner와 dependency로 충분하면 새 framework를 추가하지 않는다.
3. Circuit Presenter는 `Presenter.test()`와 `FakeNavigator`를 우선 사용한다.
4. Turbine 타입/API를 직접 import하는 모듈만 Turbine을 직접 dependency로 선언한다.
5. Android framework가 필요한 로컬 테스트는 `androidHostTest`와 Robolectric을 사용한다.
6. 실제 시스템 UI, permission, notification, widget처럼 host fake로 의미가 없는 동작만 `androidDeviceTest`로 올린다.
7. 새 test source set을 만들면 로컬 task뿐 아니라 CI 진입 task가 실제로 포함하는지 로그로 확인한다.
