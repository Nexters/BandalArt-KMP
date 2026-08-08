# KMP AGP 9 및 Metro 마이그레이션 Troubleshooting

## 문서 목적

이 문서는 BandalArt의 KMP AGP 9 전환과 Metro 도입 중 실제로 발생한 문제, 원인, 해결 방법을 기록한다. 비슷한 오류가 다시 발생했을 때 전체 마이그레이션 과정을 재조사하지 않고 증상별로 확인하기 위한 운영 문서다.

기준 작업:

- PR #189: KMP AGP 9 구조 전환
- PR #190: Metro 1.1.1 bootstrap
- PR #191: Platform/Room/DataStore graph 이전
- Kotlin 2.3.21, AGP 9.3.0, Gradle 9.5.0
- Gradle runtime JDK 21, 앱 Java/Kotlin target 17

새 문제를 추가할 때는 추측이 아니라 재현된 증상, 확인한 원인과 실제 해결을 함께 기록한다.

## 빠른 증상표

| 증상 | 먼저 확인할 항목 | 적용한 해결 |
| --- | --- | --- |
| KMP 모듈에서 Android application plugin 충돌 | `composeApp`에 KMP와 application plugin이 함께 있는지 | `androidApp` application 모듈 분리 |
| iOS framework native cache/link 실패 | Kotlin과 Navigation/CMP 구성 버전 | Navigation 2.9.2로 정렬 |
| Metro plugin 적용 직후 JVM version 오류 | Gradle daemon/CI setup JDK | Gradle runtime JDK 21 |
| Metro KLIB ABI/version 오류 | Metro와 Kotlin compiler 조합 | Kotlin 2.3.21에서는 Metro 1.1.1 사용 |
| provider 이동 후 Room/DataStore symbol 미해결 | provider가 위치한 모듈의 직접 의존성 | `composeApp`에 필요한 runtime 직접 선언 |
| bootstrap probe 제거 후 컴파일 오류 | 남은 accessor 참조 | root composition key를 `appGraph`로 변경 |
| 동일 generic DataStore binding 충돌 가능성 | raw `DataStore<Preferences>` binding 수 | wrapper를 직접 제공하고 raw type 미노출 |
| code style 전체 검사 실패 | 변경 파일인지 기존 baseline인지 | 신규 위반만 수정하고 기존 정비는 분리 |

## 1. AGP 9에서 KMP와 Android Application plugin을 한 모듈에 둘 수 없음

### 증상

기존 `composeApp`이 KMP 공통 코드와 Android application packaging을 동시에 담당해 AGP 9 구조로 전환할 수 없었다. 기존 convention plugin도 KMP 모듈에 Android library plugin을 적용한 뒤 application plugin 역할을 추가하는 구조였다.

### 원인

AGP 9의 Android-KMP 구조에서는 KMP Android library target과 Android application packaging을 같은 모듈에서 구성하지 않는다. 기존 모듈은 KMP source set, manifest, application ID, signing, build type과 앱 entry point 책임이 섞여 있었다.

### 해결

- `androidApp`: Android application plugin, manifest, application ID, version, signing, R8와 Android entry point
- `composeApp`: Android-KMP library, 공통 UI, Android/iOS actual 구현과 iOS framework

기능 코드를 재작성하지 않고 packaging 경계만 분리했다.

### 재발 방지

새 Android application 전용 설정은 `androidApp`에 둔다. KMP library 모듈에 `com.android.application` 또는 `org.jetbrains.kotlin.android`를 다시 추가하지 않는다.

## 2. AGP 9 public DSL 및 built-in Kotlin 전환

### 증상

기존 build logic에 AGP 9에서 제거되거나 바뀐 Android extension 타입과 DSL이 남아 있었다. 초기 문서의 `androidLibrary {}` 예시와 실제 AGP 9.3 DSL도 일치하지 않았다.

### 원인

AGP 9는 built-in Kotlin과 public DSL을 사용하며, 이전 AGP 내부 타입 또는 Android/Kotlin plugin 조합을 그대로 유지할 수 없다.

### 해결

- Android-KMP 설정을 `kotlin { android { ... } }`에 맞춤
- 제거된 내부 extension 대신 AGP public DSL 사용
- Kotlin Android plugin 중복 적용 제거
- AGP 9.3.0, Gradle 9.5.0, Kotlin 2.3.21, KSP 2.3.10으로 정렬

### 남은 경고

Gradle 10에서 제거될 reporting API 경고가 남아 있다. 현재 AGP 9 빌드를 막지는 않으므로 이번 기능 마이그레이션과 섞지 않고 build logic 정비 대상으로 남긴다.

## 3. Navigation 구버전의 Kotlin/Native link 실패

### 증상

Navigation 2.8.0-alpha12를 유지한 상태에서 Kotlin 2.3.21로 iOS simulator framework를 링크하면 Native cache/link 단계가 실패했다.

### 원인

기존 Navigation 바이너리와 Kotlin 2.3.21 및 Compose Multiplatform 1.10.3 조합이 맞지 않았다.

### 해결

Compose Multiplatform 1.10.3 구성과 맞는 Navigation 2.9.2로 올린 뒤 iOS simulator arm64 framework link가 통과했다.

### 확인 순서

Native link 오류가 생기면 application 코드부터 수정하지 말고 Kotlin, Compose Multiplatform, Navigation 바이너리 조합을 먼저 확인한다.

## 4. Metro와 Kotlin/Native ABI 호환성

### 증상

최신 Metro를 바로 사용하려 했을 때 현재 Kotlin compiler가 읽을 수 없는 iOS runtime KLIB/ABI 문제가 확인됐다.

### 원인

Metro 1.2.0 이상 배포물은 Kotlin 2.4 계열 ABI를 사용하고, 프로젝트는 Kotlin 2.3.21을 사용한다.

### 해결

현재 toolchain에서는 Metro 1.1.1을 고정했다. Android compilation만 성공한 상태로 버전을 확정하지 않고 iOS framework link까지 통과한 조합을 기준으로 삼았다.

### 재발 방지

Metro를 올릴 때는 다음을 한 묶음으로 검증한다.

1. Kotlin compiler 및 Compose Multiplatform 호환성
2. Android Metro codegen
3. iOS KLIB resolve와 framework link
4. Circuit integration codegen

## 5. Metro Gradle plugin은 JDK 21이 필요함

### 증상

PR #190 CI에서 Metro plugin 적용 후 build가 시작되기 전에 JVM version 오류로 실패했다. 로컬 구현은 맞았지만 GitHub Actions가 JDK 17로 Gradle을 실행하고 있었다.

### 원인

앱 bytecode target과 Gradle plugin 실행 JVM을 같은 값으로 취급했다. Metro 1.1.1 Gradle plugin은 JVM 21이 필요하지만 앱의 Java/Kotlin target은 17을 유지할 수 있다.

### 해결

- GitHub Actions setup JDK를 21로 변경
- Gradle runtime JDK는 21 사용
- 앱 Java/Kotlin target과 toolchain은 17 유지
- README와 마이그레이션 문서에서 두 값을 분리해 기록

### 확인 명령

CI 실패 시 로그의 Gradle runtime JVM을 먼저 확인한다. `jvmTarget` 또는 Android device 요구사항을 불필요하게 21로 올리지 않는다.

## 6. Provider 위치 이동 후 직접 의존성 누락

### 증상

Room DB와 DataStore provider를 core Koin module에서 `composeApp`의 Metro binding container로 옮기자 `BundledSQLiteDriver`, Room runtime과 DataStore 관련 symbol을 찾지 못해 compilation이 실패했다.

### 원인

provider 구현 코드가 사용하는 라이브러리는 해당 코드가 위치한 모듈의 compile classpath에 직접 있어야 한다. core module의 `implementation` 의존성은 `composeApp`의 provider 구현에 전이 API로 보장되지 않는다.

### 해결

`composeApp` common source set에 provider 구현이 직접 사용하는 다음 runtime 의존성을 선언했다.

- Room runtime
- Bundled SQLite driver
- DataStore core/preferences

### 재발 방지

4-B에서 repository provider 또는 binding 위치를 옮길 때도 구현 모듈의 직접 의존성을 먼저 확인한다. 단순히 하위 모듈을 dependency로 가진다는 이유로 내부 `implementation` 라이브러리를 사용할 수 있다고 가정하지 않는다.

## 7. Bootstrap probe 제거 후 남은 accessor 참조

### 증상

4-A에서 임시 `MetroBootstrapProbe`를 실제 DB/DataStore graph로 교체한 뒤 `BandalartApp`이 삭제된 `bootstrapProbe` accessor를 계속 참조해 compilation이 실패했다.

### 원인

bootstrap 단계에서 graph identity를 composition key로 쓰기 위해 probe accessor를 사용했고, 임시 타입 제거 시 호출부가 함께 정리되지 않았다.

### 해결

root composition의 key를 임시 probe가 아니라 `appGraph` 자체로 변경했다.

### 재발 방지

임시 spike/probe를 제거할 때는 타입 선언뿐 아니라 accessor, test, composition key와 entry point 참조를 함께 검색한다.

## 8. 동일 generic DataStore binding의 모호성

### 관측된 구조

기존 Koin `dataStoreModule`은 Bandalart와 인앱 업데이트용 `DataStore<Preferences>` 두 개를 qualifier 없이 같은 raw generic type으로 등록하고 wrapper 생성 시 `get()`으로 조회했다.

### 위험

등록 순서나 DI 해석에 따라 wrapper가 의도하지 않은 Preferences 파일을 받을 수 있고, Metro/Koin 공존 중 같은 파일 handle을 중복 생성할 가능성도 있다.

### 해결

- Metro graph에 raw `DataStore<Preferences>` accessor를 노출하지 않음
- `BandalartDataStoreFactory`에서 각 DataStore를 만든 즉시 대응 wrapper에 전달
- graph에는 `BandalartDataStore`, `InAppUpdateDataStore`라는 서로 다른 타입만 등록
- Koin bridge는 Metro가 만든 wrapper 인스턴스만 반환

### 재발 방지

같은 generic type의 저장소가 둘 이상이면 qualifier 없이 raw type을 graph에 공개하지 않는다. 파일별 wrapper 또는 명시적인 qualifier로 구분한다.

## 9. Koin과 Metro 과도기 중 singleton 중복 생성

### 문제 조건

동일한 DB, DataStore 또는 repository를 Koin과 Metro 양쪽에서 `single`/`@SingleIn`으로 각각 생성하면 타입 수준 테스트는 통과해도 파일 handle과 캐시 상태가 분리된다.

### 적용한 해결

허용 방향을 `Koin → Metro AppGraph accessor` 하나로 제한했다.

- Metro: 실제 객체 생성과 app scope 소유
- Koin bridge: Metro accessor 결과를 기존 호출자에게 노출
- 금지: Metro에서 Koin 조회, Koin 객체를 graph factory input으로 재주입, 양쪽의 부분 graph 혼합

Robolectric test에서 AppGraph accessor의 singleton identity와 Koin bridge 반환 identity를 함께 확인한다.

## 10. Robolectric JUnit 5 host test 설정

### 적용한 설정

실제 Android `Application`으로 Metro platform graph를 생성하기 위해 `androidHostTest`에 다음 구성을 사용했다.

- Android unit test bundle
- Robolectric
- Robolectric JUnit 5 extension
- SDK 35 명시
- JUnit Platform launcher interceptor 활성화

Robolectric SDK 35는 host test runtime 선택이며 앱의 compileSdk 37/targetSdk 36을 낮추지 않는다.

### 테스트 종료 처리

graph identity test가 생성한 Room DB는 test 종료 시 close한다. production app graph의 DB 수명은 application 수명이며 Koin stop과 연결하지 않는다.

## 11. Code style 검사와 KMP source set baseline

### 증상

전체 Spotless/Detekt 실행 시 이번 변경과 무관한 기존 Compose/Swift entry naming, trailing comma와 legacy formatting 위반이 함께 보고됐다.

### 원인

KMP source set에 task는 생성되지만 현재 저장소의 기존 코드와 Spotless/Detekt 규칙이 완전히 정렬돼 있지 않다. `bandalart.lint`는 Android Lint가 아니라 Spotless ktlint와 Detekt를 묶은 convention이다.

### 대응

- 새 provider/test 파일에서 발생한 formatting과 test naming 위반만 수정
- Spotless가 root `.editorconfig`를 명시적으로 읽게 하고 Compose annotation이 붙은 함수는 ktlint naming 검사에서 제외
- Detekt formatting의 indentation/trailing-comma rule은 비활성화해 Spotless와 상충하는 이중 포맷 판정을 제거
- 기존 파일 전체 포맷이나 naming 변경은 DI 마이그레이션 PR에 포함하지 않음
- 필수 CI인 unit test, Android Lint, Android/iOS build를 별도로 유지

기존 baseline 정비는 별도 작업으로 수행한다. 코드 스타일 실패를 이유로 관련 없는 파일까지 일괄 수정하지 않는다.

## 12. 실패가 아닌 경고 구분

다음 경고는 현재 검증 산출물을 실패시키지 않았다.

- Gradle 10에서 제거 예정인 reporting API
- 기존 Koin/GitLive Firebase iOS 바이너리 ABI linker 경고
- iOS framework bundle ID 자동 추론 경고
- GitHub Actions의 Node.js 20 action deprecation 안내
- GitHub Actions Gradle cache restore 400

경고를 무시한다는 의미는 아니다. 다만 기능/DI 변경과 원인이 다른 경고는 별도 build/CI 정비로 분리해 회귀 원인을 섞지 않는다.

## 13. 후속 단계 확인 체크리스트

### Repository graph 4-B

- repository의 실제 생성자는 Metro 한쪽에만 존재하는가
- Koin module은 Metro accessor의 동일 인스턴스만 반환하는가
- `core:data` contribution의 iOS multi-module aggregation을 가정하지 않았는가
- provider 위치가 바뀌었다면 직접 compile dependency가 있는가
- 기존 repository/DAO/DataStore 테스트가 그대로 유효한가

### Circuit vertical slice

- Metro 1.1.1 Circuit codegen이 Android와 iOS 모두에서 생성되는가
- Hilt component 인자를 복사하지 않고 repository만 Metro에서 주입하는가
- `Screen`과 `Navigator`가 assisted input으로 처리되는가
- Presenter test가 Android Play API를 iOS common state에 노출하지 않는가

### Composition root 정리

- Koin 제거 전에 남은 bridge consumer가 없는가
- Android Application과 iOS entry point가 각각 하나의 AppGraph만 소유하는가
- DB/DataStore close 또는 재생성 책임이 Koin lifecycle에 남아 있지 않은가

## 14. Circuit Native artifact와 Kotlin ABI 불일치

### 증상

Kotlin 2.3.21에서 Circuit 0.35.1을 추가한 뒤 Android는 컴파일되지만 iOS compile이 아래 오류로 실패했다.

```text
circuit-runtime-iosSimulatorArm64 ... incompatible ABI 2.4.0
library produced by 2.4.10 compiler
```

### 원인

JVM artifact는 이전 Kotlin compiler에서도 사용할 수 있는 반면 Kotlin/Native KLIB은 compiler ABI 호환 범위가 더 엄격하다. Circuit 0.35.1의 iOS artifact는 Kotlin 2.4.10으로 만들어져 Kotlin 2.3.21 compiler가 읽을 수 없다.

### 해결

- Circuit을 낮추지 않고 Kotlin Gradle plugin과 Kotlin/Native를 2.4.10으로 정렬
- Metro 1.1.1 공식 호환표에서 Kotlin 2.4 계열 지원 확인
- KSP 2.3.10 유지: Kotlin 2.4 default module name과 AGP 9 built-in Kotlin 관련 수정이 포함된 버전
- Android compile 후 iOS Simulator Arm64 framework link까지 순서대로 검증

참고:

- [Metro 1.1.1 Kotlin compatibility](https://zacsweers.github.io/metro/1.1.1/compatibility/)
- [Kotlin releases](https://kotlinlang.org/docs/releases.html)
- [KSP 2.3.10 release](https://github.com/google/ksp/releases/tag/2.3.10)

## 15. Circuit 0.35.1의 `iosX64` artifact 부재

### 증상

KMP dependency resolution에서 `circuit-*`의 `iosX64` variant를 찾지 못했다.

### 원인

프로젝트 convention plugin은 Intel simulator target인 `iosX64`까지 모든 KMP module에 만들고 있었지만 Circuit 0.35.1은 해당 target artifact를 배포하지 않는다.

### 해결

- 공통 iOS target을 `iosArm64`, `iosSimulatorArm64`로 제한
- app/feature framework target과 KSP `kspIosX64` configuration을 함께 제거
- Apple Silicon simulator와 실제 iOS device target은 유지

Intel Mac simulator 지원이 다시 필요해지면 Circuit이 해당 artifact를 제공하는지 먼저 확인하고 target을 복원한다.

## 16. Kotlin 2.x에서 공통 Parcelize annotation typealias가 동작하지 않음

### 증상

common screen에 `@CommonParcelize`를 선언했지만 Android actual에서 `Parcelable` 구현이 생성되지 않았다.

### 원인

Parcelize plugin을 활성화하는 annotation을 `expect`/`actual typealias`로 우회하는 방식은 Kotlin 2.x에서 지원되지 않는다.

### 해결

- `commonMain`에 일반 `CommonParcelize` annotation을 선언
- Screen을 소유하는 각 module에 Parcelize plugin 적용
- `plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.nexters.bandalart.core.navigation.CommonParcelize` compiler option 설정

참고: [Parcelize setup for Kotlin Multiplatform](https://developer.android.com/kotlin/parcelize#setup_parcelize_for_kotlin_multiplatform)

## 17. `rememberCircuitNavigator`의 Android 전용 기본 root pop

### 증상

`rememberCircuitNavigator(backStack)`은 Android compile에서는 통과했지만 iOS compile에서 `onRootPop` 인자가 없다는 오류가 발생했다.

### 원인

Circuit 0.35.1은 Android source set에 `LocalOnBackPressedDispatcherOwner`로 root pop을 처리하는 단일 인자 overload를 제공한다. 공통/iOS API는 `onRootPop` callback을 필수로 받는다.

### 해결

플랫폼별 `rememberBandalartNavigator` 래퍼를 두었다.

- Android: 단일 인자 overload를 호출해 기존 앱 종료/back dispatcher 동작 유지
- iOS: `onRootPop`을 no-op으로 명시

공통 코드에서 빈 callback을 직접 넘기면 Android root back 동작까지 소비하므로 사용하지 않는다.

참고: [Circuit navigation](https://slackhq.github.io/circuit/navigation/)

## 18. Circuit Presenter Android host test의 Android stub 호출

### 증상

`Presenter.test` 실행 중 Molecule/Compose 내부의 `android.util.Log` 또는 `android.os.Trace` 호출이 `Method ... not mocked` 오류로 실패했다.

### 원인

Presenter 자체는 Android API를 사용하지 않지만 Android host test runtime에서 Compose/Molecule이 Android stub method를 호출한다. Robolectric JUnit 5 extension만 추가해도 Molecule이 실행되는 coroutine context 전체가 sandbox 처리되지는 않았다.

### 해결

KMP Android convention의 host unit test option에 `isReturnDefaultValues = true`를 설정했다. feature test에 불필요한 Robolectric annotation과 dependency는 추가하지 않았다.

이 옵션은 Android stub method의 기본 반환만 허용하며 Presenter의 navigation/repository assertion은 Circuit test와 fake repository로 계속 검증한다.

## 19. composeApp에서 Circuit Presenter test 확장 함수를 찾지 못함

### 증상

`composeApp`에 처음 Presenter test를 추가한 뒤 `com.slack.circuit.test.test`와 `kotlinx.coroutines.test.runTest`를 찾지 못해 Android host test compile이 실패했다.

### 원인

기존 `composeApp` 테스트는 `FakeNavigator`만 사용해 `circuit-test` 의존성만으로 동작했다. Presenter의 composition을 실행하는 `Presenter.test`는 coroutine test runtime도 필요하지만 `composeApp`의 `androidHostTest`에는 `kotlinx-coroutines-test`가 직접 선언돼 있지 않았다.

### 해결

- `composeApp`의 `androidHostTest`에 `libs.kotlinx.coroutines.test`를 직접 추가
- `LegacyHomePresenterTest`를 다시 실행해 Home → Complete `goTo` 이벤트 검증
- Complete feature의 Presenter test와 composeApp Metro graph test를 함께 실행해 factory aggregation 회귀 확인

KMP test source set에서는 다른 module의 transitive test dependency를 가정하지 않고, 사용하는 test runtime을 해당 source set에 직접 선언한다.

## 20. Presenter test에서 동일한 State를 기다리면 timeout 발생

### 증상

Home 편집 Presenter test에서 최대 길이를 초과한 title을 입력한 뒤 `awaitItem()`을 호출하자 `No value produced in 3s`로 실패했다.

### 원인

validation은 초과 입력을 버리고 기존 draft를 그대로 유지한다. `mutableStateOf`의 새 값이 기존 State와 구조적으로 같으므로 recomposition과 새 Turbine item이 발생하지 않는다. timeout은 Presenter 실패가 아니라 테스트가 존재하지 않는 State 변경을 기다린 결과다.

### 해결

- 거절된 입력은 `expectNoEvents()`로 검증한다.
- 기존에 받은 State의 draft 값이 유지되는지도 함께 확인한다.
- 실제 State가 바뀌는 유효 입력과 repository mutation은 계속 `awaitItem()`으로 검증한다.

Circuit Presenter test에서 모든 Event가 새 item을 만든다고 가정하지 않는다. no-op과 validation 거절은 이벤트 없음 자체가 계약이다.

## 21. 완료 화면 이동이 이미지 캡처보다 먼저 실행됨

### 증상

기존 Home ViewModel은 완료 감지 후 고정 지연으로 캡처와 화면 이동 순서를 맞췄다. 기기 성능이나 프레임 타이밍에 따라 URI가 만들어지기 전에 빈 URI로 `CompleteScreen` 이동이 시작될 수 있었다.

### 원인

이미지 캡처는 Compose `GraphicsLayer`와 파일 저장을 거치는 UI side effect인데, ViewModel이 실제 완료 신호 없이 시간만 기다렸다. 상태 소유자가 플랫폼 캡처 결과를 알 수 없는 구조였다.

### 해결

- Presenter는 `ImageRequest.Complete`와 이동 metadata만 State에 노출한다.
- 공통 UI는 다음 프레임에서 layer를 캡처하고 `bitmapToFileUri` 완료를 기다린다.
- UI가 `CaptureFinished(uri)` Event를 보낸 뒤에만 Presenter가 `CompleteScreen`으로 이동한다.
- Presenter test에서 capture 완료 전에는 navigation이 없고 URI 수신 후 한 번만 이동하는지 검증한다.

비동기 UI 작업과 navigation 순서를 고정 지연으로 맞추지 않고 실제 결과 Event를 경계로 삼는다.

## 22. flexible update를 공통 Presenter에 직접 넣으면 iOS 계약이 오염됨

### 증상

Android의 `AppUpdateManager`, install status listener와 lifecycle 처리를 Home Presenter에 옮기면 commonMain이 Play Core 타입이나 Android Context에 의존하게 된다.

### 원인

업데이트 제안 여부와 거절 versionCode는 제품 상태이지만, Play Core flow 실행·listener·`completeUpdate()`는 Android UI lifecycle에 묶인 플랫폼 effect다.

### 해결

- Presenter는 후보 versionCode와 거절 기록만 `InAppUpdateRepository`로 관리한다.
- Android 구현은 `androidMain`의 `FlexibleUpdateEffect`에서 listener 등록/해제, 재진입 복구와 update flow를 처리한다.
- major/minor 강제 업데이트 후보는 Home의 flexible flow에서 제외하고 순수 정책 함수로 테스트한다.
- iOS actual은 같은 공통 계약을 유지하는 no-op으로 둔다.

플랫폼 SDK 호출은 source set effect에 두고, 공통 Presenter에는 직렬화 가능한 상태와 Event만 남긴다.

## 23. Spotless module apply가 기존 Home UI까지 대량 변경함

### 증상

Home migration 파일을 포맷하려고 모듈 단위 `spotlessApply`를 실행하자 이번 작업과 무관한 기존 UI 파일 15개에도 포맷 diff가 생겼다.

### 원인

Home 모듈에는 현재 formatter 규칙과 맞지 않는 기존 파일이 남아 있다. 모듈 전체 apply는 변경 파일 범위와 관계없이 이 baseline까지 수정한다.

### 해결

- 무관한 formatter diff는 즉시 되돌렸다.
- 이번에 변경한 Kotlin 파일만 formatter 결과를 유지하고 Detekt 및 대상 compile/test로 검증했다.
- KTS/XML Spotless check는 별도로 통과시켰다.
- 기존 Home UI 포맷 부채는 기능 migration PR에 섞지 않고 별도 정리 대상으로 남긴다.

대규모 migration 중 formatter baseline이 깨져 있으면 module-wide apply 전에 변경 대상 목록을 고정한다.

## 24. 직접 Koin 의존성을 지워도 APK runtime에 Koin이 남음

### 증상

소스, version catalog의 Koin alias와 `KoinContext`를 모두 제거한 뒤에도 Android `debugRuntimeClasspath`에서 Koin 3.5.6이 조회됐다.

### 원인

사용처 없이 남아 있던 Kotzilla SDK가 내부 graph 관측을 위해 Koin core를 전이 의존성으로 포함했다. 직접 의존성 검색만으로는 이 runtime 잔재를 발견할 수 없었다.

### 해결

- `dependencyInsight --dependency io.insert-koin --configuration debugRuntimeClasspath`로 유입 경로를 확인했다.
- 코드 사용처가 없는 Kotzilla SDK dependency와 root buildscript plugin을 제거했다.
- 더 이상 사용되지 않는 Kotzilla version/catalog alias와 Android config 파일도 함께 제거했다.
- dependency insight를 다시 실행해 Koin artifact 0건을 확인했다.

DI runtime 제거는 소스 import 0건뿐 아니라 최종 앱 runtime classpath까지 확인한다.

## 25. Spotless ratchet이 linked worktree에서 저장소를 찾지 못함

### 증상

`ratchetFrom("origin/main")`을 convention에 항상 적용한 뒤 linked worktree에서 Spotless task를 생성하면 `Cannot find git repository in any parent directory`로 실패했다.

### 원인

일반 clone의 `.git`은 디렉터리지만 `git worktree`로 만든 작업 디렉터리의 `.git`은 공통 git directory를 가리키는 파일이다. Spotless의 JGit ratchet 탐색이 이 구조를 repository directory로 인식하지 못했다.

### 해결

- convention은 `spotlessRatchetFrom` Gradle property가 있을 때만 ratchet을 활성화한다.
- GitHub Actions quality job은 full fetch 후 `-PspotlessRatchetFrom=origin/main`을 전달한다.
- linked worktree의 로컬 변경 파일은 absolute path를 전달하는 `spotlessIdeHook`으로 포맷한다.
- 기존 baseline 전체에 `spotlessApply`를 실행하지 않는다.

CI clone과 로컬 linked worktree의 Git metadata 형태가 다르므로 ratchet 활성화를 명시적인 실행 입력으로 둔다.

## 참고 문서

- [KMP AGP 9 마이그레이션 전략](../kmp/KMP_AGP_9_MIGRATION_STRATEGY.md)
- [Metro 부트스트랩 전략](METRO_BOOTSTRAP_STRATEGY.md)
- [Metro Platform/Room/DataStore graph 전략](METRO_PLATFORM_DATA_GRAPH_STRATEGY.md)
- [Circuit + Metro KMP 이식 맵](CIRCUIT_METRO_KMP_MIGRATION_MAP.md)
- [JetBrains kotlinconf-app](https://github.com/JetBrains/kotlinconf-app/tree/0b1616cba68e)
