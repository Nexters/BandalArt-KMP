# KMP AGP 9 마이그레이션 전략

이 문서는 `main`의 Kotlin Multiplatform(KMP) 구조를 Android Gradle Plugin(AGP) 9.3으로 이전하는 실행 기준을 정의한다. Android 앱 진입점을 `androidApp`으로 분리하고, 기존 `composeApp`과 기능 모듈은 Android-KMP 라이브러리 플러그인으로 전환한다.

## 1. 목표와 완료 조건

이번 작업의 목표는 AGP 9와 KMP가 같은 모듈에서 Android Application 플러그인을 함께 사용할 수 없는 문제를 해소하는 것이다.

완료 조건은 다음과 같다:

- `androidApp`만 `com.android.application`을 사용한다
- 모든 KMP 모듈이 `com.android.kotlin.multiplatform.library`를 사용한다
- `composeApp`의 iOS framework 이름 `ComposeApp`과 Xcode 연결을 유지한다
- Android 애플리케이션 ID `com.nexters.bandalart`와 저장 데이터 호환성을 유지한다
- Android 앱 버전을 `2.2.6 (20206)`으로 맞춘다
- `compileSdk 37`, `targetSdk 36`, `minSdk 28`을 적용한다
- Android debug APK, release AAB, KMP 테스트, iOS simulator framework가 빌드된다
- `android.builtInKotlin=false`, `android.newDsl=false`, `android.enableLegacyVariantApi=true`를 추가하지 않는다

## 2. 적용 근거

JetBrains의 `kotlin-tooling-agp9-migration` 스킬과 Android 공식 문서를 기준으로 `Path B`를 적용한다. `Path B`는 KMP와 Android Application 플러그인을 함께 사용하는 모듈에서 Android 앱 진입점을 별도 모듈로 분리하는 필수 경로다.

AGP 9.3의 공식 호환 조합은 다음과 같다:

| 항목 | 목표 버전 | 근거 |
| --- | --- | --- |
| AGP | 9.3.0 | API 37 지원 |
| Gradle | 9.5.0 | AGP 9.3 최소 및 기본 버전 |
| JDK | 17 | AGP 9.3 최소 및 기본 버전 |
| Kotlin | 2.3.21 | `develop` 검증 버전, KMP 구성 캐시 수정 포함 |
| KSP | 2.3.10 | AGP 9 지원 최소 버전 2.3.1 이상 |
| Compose Multiplatform | 1.10.3 | AGP 9 호환 최소 버전 1.9.3 이상 |
| SDK Build Tools | 36.0.0 | AGP 9.3 기본 버전 |
| Baseline Profile | 1.5.0-alpha06 | 새 AGP DSL 사용 가능 버전 |

Android-KMP DSL은 AGP 9.3 기준 `kotlin { android { ... } }`를 사용한다. 초기 문서의 `androidLibrary {}` 표기는 최신 DSL에서 사용하지 않는다.

이 표는 AGP 9 전환 시점의 최소 요구사항이다. 후속 Metro 부트스트랩부터는 Metro Gradle 플러그인 실행 요구사항에 맞춰 Gradle runtime을 JDK 21로 사용하되, 앱의 Java/Kotlin target과 toolchain은 17로 유지한다.

## 3. 현재 구조와 문제

현재 `composeApp`은 다음 책임을 한 모듈에 가진다:

- KMP 공통 UI와 Android/iOS actual 구현
- iOS 정적 framework 생성
- Android `Application`, `MainActivity`, manifest, 앱 리소스
- Android 서명, build type, R8, Baseline Profile 설정

`bandalart.kmp.android` convention plugin은 KMP 모듈에 `com.android.library`를 적용한다. `composeApp`은 여기에 `com.android.application`까지 적용한다. 이 조합은 AGP 9에서 허용되지 않는다.

build logic에도 AGP 9에서 제거된 타입과 DSL이 남아 있다:

- `BaseAppModuleExtension`
- `com.android.build.gradle.LibraryExtension`
- `TestedExtension`
- `androidTarget {}`
- top-level `android {}`를 전제로 한 KMP convention
- `org.jetbrains.kotlin.android`

## 4. 목표 모듈 구조

모듈 이름 변경은 최소화한다. `composeApp` 이름과 iOS framework 이름을 유지해 Xcode 설정 변경을 피한다.

```text
androidApp/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    kotlin/com/nexters/bandalart/BandalartApplication.kt
    kotlin/com/nexters/bandalart/MainActivity.kt
    res/values

composeApp/
  build.gradle.kts
  src/commonMain
  src/androidMain  # expect/actual과 Android DI factory만 유지
  src/iosMain

iosApp/
  기존 Xcode 프로젝트 유지
```

각 모듈의 책임은 다음과 같다:

| 모듈 | 적용 플러그인 | 책임 |
| --- | --- | --- |
| `androidApp` | `com.android.application` | Android 진입점, manifest, 앱 리소스, 서명, R8, Baseline Profile |
| `composeApp` | KMP + Android-KMP library | 공통 UI, Android/iOS actual, `ComposeApp` framework |
| `core:*`, `feature:*` | KMP + Android-KMP library | 기존 공통 기능과 플랫폼 구현 |
| `baselineprofile` | `com.android.test` | `androidApp` 대상 Baseline Profile 생성 |

## 5. 파일 이동 기준

Android 애플리케이션 수명주기와 패키징에 속하는 파일만 `androidApp`으로 이동한다:

- `BandalartApplication.kt`
- `MainActivity.kt`
- `AndroidManifest.xml`
- `src/androidMain/res/**`
- `proguard-rules.pro`과 앱 전용 ProGuard 파일
- `google-services.json`

다음 파일은 `composeApp`의 `androidMain`에 유지한다:

- `Platform.android.kt`
- `di/Modules.android.kt`
- 공통 expect 선언을 구현하는 Android actual 파일

`androidApp` namespace는 `com.nexters.bandalart`를 유지한다. `composeApp`의 Android namespace는 충돌을 피하도록 `com.nexters.bandalart.shared`로 변경한다.

## 6. Build logic 전환

build logic은 모듈 스크립트보다 먼저 전환한다:

1. `Plugins`에 `com.android.kotlin.multiplatform.library`를 추가한다
2. `bandalart.kmp.android`가 Android-KMP library plugin을 적용하도록 변경한다
3. KMP Android 설정을 `kotlin { android { /* module config */ } }`로 옮긴다
4. host test가 있는 모듈에 `withHostTest { isIncludeAndroidResources = true }`를 설정한다
5. `androidUnitTest` 디렉터리와 source set을 `androidHostTest`로 변경한다
6. Android Application convention은 public `ApplicationExtension`만 사용한다
7. `org.jetbrains.kotlin.android`와 구형 AGP extension 타입을 제거한다
8. Compose Preview 런타임 의존성은 `androidRuntimeClasspath`로 옮긴다

KMP 모듈의 namespace는 Android-KMP target에 둔다. 공통 convention은 compile SDK, min SDK, JVM 17과 host test 기본값을 담당한다.

## 7. 플러그인 호환성 결정

호환성 문제는 우회 플래그보다 플러그인 변경으로 해결한다:

| 플러그인 | 현재 | 결정 |
| --- | --- | --- |
| KSP | 2.1.20-1.0.32 | 2.3.10으로 업데이트 |
| Compose Multiplatform | 1.8.0-alpha02 | 1.10.3으로 업데이트 |
| Navigation | 2.8.0-alpha12 | Compose Multiplatform 1.10.3 공식 조합인 2.9.2로 업데이트 |
| Baseline Profile | 1.3.4 | 1.5.0-alpha06으로 업데이트 |
| Detekt | 1.23.8 | AGP 빌드와 분리해 유지, CI 강제 적용은 별도 정비 |
| Spotless | 7.0.1 | KMP 포맷 설정 정비 전까지 CI 강제 적용 제외 |
| Robolectric | 4.14.1 | Android 16을 지원하는 4.16.1로 업데이트 |
| JUnit5 Robolectric extension | 0.9.0 | Gradle plugin은 제거하고 extension과 launcher interceptor 설정만 명시적으로 유지 |
| Kotzilla | 1.0.1 | Android 앱 모듈로 한정하고 빌드 호환성을 검증 |

Detekt와 Spotless는 현재 규칙 충돌과 모듈 적용 누락이 있다. 이번 PR은 AGP 9 빌드, 테스트, 패키징을 검증하고 코드 스타일 CI 재설계는 별도 작업으로 남긴다.

## 8. 실행 순서

작업은 다음 순서로 진행한다:

1. 전략 문서를 커밋 전 기준으로 확정한다
2. 버전 카탈로그와 Gradle wrapper를 업데이트한다
3. build logic을 AGP 9 public DSL과 Android-KMP plugin에 맞춘다
4. 모든 KMP 모듈의 Android DSL과 host test source set을 전환한다
5. `androidApp`을 만들고 Android 진입점과 앱 리소스를 이동한다
6. `composeApp`을 순수 KMP 라이브러리로 전환한다
7. Baseline Profile 대상과 CI 빌드 태스크를 `androidApp`으로 변경한다
8. Android, KMP, iOS 검증을 수행한다
9. 결과와 남은 경고를 이 문서에 기록한다

## 9. 검증 게이트

각 검증은 이전 단계가 통과한 뒤 실행한다:

### 9.1 구조와 플러그인

- KMP 모듈에 `com.android.application` 또는 `com.android.library`가 없는지 확인
- AGP 9 모듈에 `org.jetbrains.kotlin.android`가 없는지 확인
- `androidUnitTest`와 `androidInstrumentedTest` 경로가 남지 않았는지 확인
- 구형 AGP extension과 variant API 참조가 없는지 확인

### 9.2 Android와 테스트

```bash
./gradlew :androidApp:testDebugUnitTest
./gradlew allTests
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:bundleRelease
```

release AAB는 서명 정보가 있는 로컬 환경에서 검증한다. APK와 AAB의 package, versionCode, versionName, compile SDK, target SDK를 확인한다.

### 9.3 iOS

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

가능하면 `xcodebuild`로 iOS simulator 앱까지 빌드한다. framework 이름과 import가 `ComposeApp`으로 유지되는지 확인한다.

### 9.4 CI

CI는 다음 항목을 실행하도록 변경한다:

- Android host 및 공통 단위 테스트
- `:androidApp:assembleDebug`
- `:composeApp:linkDebugFrameworkIosSimulatorArm64`

기존 `ktlintCheck detekt` 명령은 KMP 설정 정비 전까지 제거한다. 검사 제거 사유와 후속 작업을 PR에 명시한다.

## 10. 롤백 기준

다음 문제가 해결되지 않으면 변경을 원인별 커밋까지 되돌린다:

- iOS framework 이름이나 Xcode 연결 변경
- Room schema 또는 DataStore 파일 경로 변경
- Android application ID 또는 서명 설정 변경
- release R8 빌드 실패
- 플러그인 호환성을 위해 legacy opt-out 플래그가 필요한 상황

모듈 분리, build logic, 버전 업데이트를 별도 커밋으로 나눠 원인 단위 롤백이 가능하게 한다.

## 11. 이번 작업에서 제외하는 범위

다음 항목은 AGP 9 마이그레이션 이후 별도 단계에서 진행한다:

- `develop`의 Circuit 화면과 Presenter를 `main`으로 이식
- Koin과 Hilt를 Metro로 수렴
- BackStack을 NavStack으로 변경
- Wasm target과 브라우저 History API 어댑터 추가
- 설정 화면, 다크 모드, 신규 기능 추가
- `composeApp`을 `shared`로 이름 변경하는 전체 구조 개편

AGP 9 브랜치는 플랫폼 진입점과 빌드 도구만 변경한다. 사용자 기능과 데이터 동작은 유지한다.

## 12. 실행 결과

### 12.1 최종 버전과 구조

- AGP 9.3.0, Gradle 9.5.0, Kotlin 2.3.21, KSP 2.3.10
- Compose Multiplatform 1.10.3, Navigation 2.9.2
- compileSdk 37, targetSdk 36, minSdk 28
- 앱 버전 2.2.6 (20206)
- `androidApp`만 Android Application plugin을 사용한다
- `composeApp`, `core:*`, `feature:*`는 Android-KMP library plugin을 사용한다
- Android 앱 진입점, manifest, 리소스, 서명과 R8 설정은 `androidApp`으로 이동했다
- 공통 UI와 플랫폼 actual 구현, `ComposeApp` iOS framework는 `composeApp`에 유지했다

### 12.2 테스트와 빌드

다음 검증을 통과했다:

```bash
./gradlew allTests :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:bundleRelease -x :androidApp:uploadCrashlyticsMappingFileRelease
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Robolectric database 테스트는 Java 17에서 실행 가능하도록 SDK 35를 명시했다. 앱의 targetSdk 36과 compileSdk 37에는 영향을 주지 않는다. Gradle 9에서 필요한 JUnit Platform launcher와 launcher interceptor도 명시적으로 구성했다.

Navigation 2.8.0-alpha12는 Kotlin 2.3.21 네이티브 캐시 링크에 실패했다. Compose Multiplatform 1.10.3 공식 구성 버전인 Navigation 2.9.2로 올린 뒤 iOS simulator framework 링크가 통과했다.

Xcode 프로젝트의 `iosApp` scheme과 Swift Package 해석은 확인했다. generic simulator 전체 빌드는 arm64와 x86_64 framework를 함께 생성하던 중 로컬 임시 디스크 부족으로 중단됐으며, 필수 게이트인 arm64 simulator `ComposeApp.framework` 링크는 별도로 통과했다.

### 12.3 Android 산출물

debug APK에서 다음 메타데이터를 확인했다:

- package: `com.nexters.bandalart.dev`
- versionName: `2.2.6`
- versionCode: `20206`
- compileSdk: `37`
- targetSdk: `36`
- minSdk: `28`

서명된 release AAB와 R8 mapping 파일도 생성됐다. 로컬 release 검증에서는 네트워크 업로드와 무관하게 빌드 결과를 판단하기 위해 Crashlytics mapping 업로드 task를 제외했다.

### 12.4 코드 품질 도구

- Android Lint는 `:androidApp:lintDebug`에서 KMP 라이브러리와 `androidHostTest` lint model까지 집계해 통과했다
- Detekt는 KMP target/source set별 task를 생성하지만 기존 규칙과 소스 범위 정비가 필요하다
- `bandalart.lint`는 Android Lint가 아니라 Spotless의 ktlint 엔진과 Detekt를 묶은 convention이다
- Spotless는 KMP 디렉터리를 처리할 수 있지만 현재 license/style 규칙과 기존 소스가 맞지 않아 이번 CI 게이트에서는 제외했다
- CI는 단위 테스트, Android Lint, Android debug APK, iOS simulator framework를 검증한다

### 12.5 의존성 정리와 남은 경고

- 실제 사용 아이콘이 모두 기본 Material icon이므로 `material-icons-extended`를 제거하고 `material-icons-core`만 유지했다
- 공통 Preview annotation은 최신 멀티플랫폼 Preview API로 통일했다
- 공통 코드의 JVM 전용 clock 사용을 `kotlin.time.Clock`으로 교체해 iOS 컴파일을 복구했다
- Koin 4.1.0-Beta5와 GitLive Firebase 2.1.0의 오래된 iOS 바이너리에서 ABI 관련 linker 경고가 남지만 framework 링크는 성공한다. Metro 전환 및 Firebase 정비 단계에서 함께 갱신한다
- Gradle 10에서 제거될 reporting API 경고와 일부 기존 deprecation 경고는 후속 build logic 정비 대상으로 남긴다
