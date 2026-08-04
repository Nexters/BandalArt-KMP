# Circuit 0.35.1 및 SDK 37 마이그레이션 전략

## 배경

- 기준 브랜치: `develop`의 PR #185 머지 커밋
- 실행 이슈: #181
- 상위 로드맵: #180
- 현재 조합: AGP 8.9.1, Gradle 8.11.1, Kotlin 2.1.21, KSP 2.1.21-2.0.1, Circuit 0.29.0, compile/target SDK 36, 앱 2.2.6(20206)
- 목표 조합: AGP 9.3.0, Gradle 9.5.0, Kotlin 2.3.21, KSP 2.3.10, Hilt 2.60.1, Circuit 0.35.1, compile SDK 37

Circuit 0.35.1은 API 37로 컴파일된 Android 의존성을 사용한다. AGP 9.3은 API 37을 지원하며 Gradle 9.5.0 이상을 요구한다.

## 범위

### 포함

- Gradle Wrapper 및 Android Gradle Plugin 업그레이드
- AGP built-in Kotlin 전환
- AGP 9 public DSL에 맞춘 convention plugin 수정
- Kotlin, KSP, Hilt 및 직접 충돌하는 빌드 플러그인 호환 버전 정렬
- compileSdk 37 및 Circuit 0.35.1 적용
- Circuit API 변경 대응
- Presenter 테스트와 Android 빌드 회귀 검증

### 제외

- targetSdk 37 적용 및 플랫폼 동작 변경 대응
- iOS Circuit + Metro 전환
- 기능 및 디자인 변경
- 마이그레이션과 무관한 전체 의존성 최신화

## 원칙

- `android.builtInKotlin=false`, `android.newDsl=false` 등의 임시 opt-out을 사용하지 않는다.
- AGP 내부 타입과 deprecated Variant API를 public DSL/API로 교체한다.
- `org.jetbrains.kotlin.android` 플러그인을 Android 모듈에서 제거한다.
- KSP는 2.3.6 이상을 사용하고 Hilt는 2.59.2 이상을 사용한다.
- Kotlin은 최신 Hilt가 읽을 수 있는 메타데이터 버전인 2.3.21로 정렬한다.
- compileSdk만 37로 올리고 targetSdk는 36으로 유지한다.
- 한 번에 전체 빌드를 반복하지 않고 `help` → build-logic 컴파일 → app 컴파일 → 테스트 → release 순으로 검증한다.

## 사전 확인 결과

- 현재 프로젝트는 Android application/library 플러그인만 사용하며 KMP 플러그인은 적용하지 않는다.
- kapt 사용은 없고 Room, Hilt, Circuit codegen이 모두 KSP를 사용한다.
- build-logic에 AGP 9에서 제거되는 `com.android.build.gradle.LibraryExtension`, `BaseAppModuleExtension`, 제네릭 `CommonExtension` 사용이 있다.
- Android convention plugin이 `org.jetbrains.kotlin.android`를 직접 적용하고 있다.
- `core:common`에 custom BuildConfig field가 있으며 `buildFeatures.buildConfig`는 이미 활성화돼 있다.
- Circuit 0.33부터 `Navigator.resetRoot`의 주 API가 Boolean 인자에서 `StateOptions`로 변경되고 `forward`, `backward`, `peekNavStack`가 추가됐다.
- 현재 앱은 `goTo`, `pop`, 기본 옵션 `resetRoot`, `rememberCircuitNavigator(backStack)`만 사용하므로 호환 API가 남아 있지만 `FakeNavigator.ResetRootEvent`와 반환 타입은 테스트로 확인해야 한다.

## 단계

### 1. 툴체인 부트스트랩

1. Gradle Wrapper를 9.5.0으로 변경한다.
2. AGP를 9.3.0으로 변경한다.
3. Kotlin/KSP/Hilt를 목표 버전으로 정렬한다.
4. compileSdk를 37로 변경한다.
5. `./gradlew help`로 플러그인 해석 오류를 수집한다.

### 2. built-in Kotlin 및 신규 DSL 전환

1. Android convention plugin에서 Kotlin Android 플러그인 적용을 제거한다.
2. `CommonExtension` 제네릭과 AGP 내부 extension 타입을 public API로 변경한다.
3. Kotlin compiler option과 generated source 연결을 built-in Kotlin 호환 방식으로 수정한다.
4. KSP/Hilt/Room/Circuit codegen 태스크 연결을 확인한다.
5. BuildConfig 생성과 앱 버전 계산을 확인한다.

### 3. Circuit 최신화

1. Circuit을 0.35.1로 변경한다.
2. 의존성 메타데이터와 codegen 컴파일을 확인한다.
3. `Navigator.resetRoot`의 `StateOptions` 전환과 `FakeNavigator` 이벤트 모델 변경을 확인한다.
4. 기존 BackStack 경로를 유지한 상태에서 `rememberCircuitNavigator`, `goTo`, `pop`, root reset 동작을 검증한다.
5. deprecated/internal API 및 상태 보존·내비게이션 변경점을 점검한다.
6. 필요한 소스와 Presenter 테스트만 수정한다.

### 4. 검증

1. `./gradlew help`
2. `./gradlew build --dry-run`
3. 관련 모듈 Presenter 테스트
4. `./gradlew ktlintCheck`
5. `./gradlew :app:compileDebugKotlin`
6. `./gradlew :app:assembleDebug`
7. `./gradlew :app:bundleRelease`

## 완료 조건

- 임시 호환 플래그 없이 AGP 9.3.0과 built-in Kotlin으로 Gradle sync/build가 동작한다.
- compileSdk 37 및 Circuit 0.35.1 조합으로 Android CI 검증 항목이 통과한다.
- 기존 Circuit Presenter 테스트와 인앱 업데이트 관련 테스트가 통과한다.
- targetSdk와 앱 버전은 기존 값인 36, 2.2.6(20206)을 유지한다.

## 실행 결과

- AGP 9의 built-in Kotlin과 public DSL로 convention plugin을 전환했다.
- Kotlin 2.3에서 더 이상 안전하지 않은 `INLINE_FROM_HIGHER_PLATFORM`/`DSL_SCOPE_VIOLATION` 억제를 제거했다.
- Kotlin/KSP는 공식 plugin DSL로만 해석되므로 불필요한 루트 `buildscript` classpath 주입은 사용하지 않는다.
- Kotlin 2.4 메타데이터를 Hilt가 읽지 못하는 오류를 확인해 Kotlin 2.3.21과 Hilt 2.60.1로 호환 버전을 정렬했다.
- `Navigator.resetRoot`의 주 API는 `StateOptions`를 받도록 변경됐지만 기존 Boolean/default 호출용 호환 확장이 유지된다.
- 현재 앱은 `resetRoot(screen)`의 기본 옵션만 사용하므로 소스 변경 없이 호환된다.
- `FakeNavigator.ResetRootEvent`의 모델 변경 후에도 온보딩과 스플래시 Presenter 테스트에서 목적지 이벤트가 정상 검증된다.
- 현재 앱에는 순방향 기록이 필요하지 않으므로 기존 `BackStack`을 유지하고 `NavStack` 전환은 보류한다.

### 검증 완료

- `./gradlew help`
- `./gradlew build --dry-run`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew testDebugUnitTest ktlintCheck`
- `./gradlew :app:assembleDebug :app:bundleRelease`
- `./gradlew :app:dependencyInsight --dependency com.slack.circuit:circuit-foundation --configuration debugRuntimeClasspath`
- 생성 APK 메타데이터: compileSdk 37, targetSdk 36, versionName 2.2.6, versionCode 20206

## 참고

- [AGP 9.3.0 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
- [AGP 및 Gradle 호환성](https://developer.android.com/build/releases/about-agp)
- [AGP built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Circuit 0.35.1 release](https://github.com/slackhq/circuit/releases/tag/0.35.1)
- [Circuit NavStack migration](https://slackhq.github.io/circuit/docs/navigation-navstack-migration/)
- [BandalArt Circuit BackStack과 NavStack 가이드](./CIRCUIT_NAVSTACK_GUIDE.md)
