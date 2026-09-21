# BandalArt Circuit Screen과 CommonParcelize 가이드

## 1. 요약

- `@CommonParcelize`는 라이브러리가 제공하는 어노테이션이 아니라 이 프로젝트가 `core/navigation`에 직접 정의한 커스텀 어노테이션이다.
- 어노테이션 자체는 아무 동작도 하지 않는다. Parcelize 컴파일러 플러그인의 `additionalAnnotation` 옵션이 "이 어노테이션도 `@Parcelize`처럼 처리하라"고 알려 줘서 Android 타깃에서만 `Parcelable` 구현이 생성된다.
- `@Parcelize`는 Android 전용이라 iOS 타깃이 있는 `commonMain`에서 쓸 수 없다. 그래서 common 코드에서 쓸 수 있는 자체 어노테이션을 둔다.
- Circuit의 `Screen`은 Android에서 `Parcelable`이다. common에 선언한 Screen이 Android에서 이 요구를 만족하도록 `@CommonParcelize`와 `ParcelableScreen`을 함께 쓴다.

## 2. 왜 필요한가

`@Parcelize`(`kotlinx.parcelize.Parcelize`)는 컴파일러 플러그인이 `android.os.Parcelable` 구현을 생성해 주는 Android 전용 어노테이션이다. `Parcelable`은 Android 타입이라 iOS 타깃이 있는 `commonMain`에서는 직접 참조할 수 없다.

반면 Circuit의 Android `Screen`은 `Parcelable`이다. Screen을 `commonMain`에 선언하려면 다음 두 가지가 필요하다.

1. common 코드에서 "Android에서는 Parcelable인 Screen"임을 표현하는 타입 → `ParcelableScreen`
2. Android 컴파일에서 `Parcelable` 구현을 만들어 주는, common에서 쓸 수 있는 어노테이션 → `@CommonParcelize`

## 3. `@CommonParcelize`

### 3.1 정의

```kotlin
// core/navigation/src/commonMain/kotlin/com/nexters/bandalart/core/navigation/CommonParcelize.kt
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class CommonParcelize
```

Circuit의 `ParcelableScreen` KDoc도 예시로 `@Parcelize // Or your own @CommonParcelize annotation`을 든다. 각 프로젝트가 자체 어노테이션을 정의해 쓰는 것이 의도된 사용 방식이다.

### 3.2 동작 원리

Parcelize 플러그인을 적용한 모듈의 Android 타깃에 다음 컴파일러 옵션을 준다.

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.parcelize")
}

kotlin {
    targets.withType<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        compilerOptions.freeCompilerArgs.addAll(
            "-P",
            "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.nexters.bandalart.core.navigation.CommonParcelize",
        )
    }
}
```

- Android 타깃: `@CommonParcelize`가 붙은 클래스에 `Parcelable` 구현이 생성된다.
- iOS 타깃: 옵션을 주지 않으므로 아무 코드도 생성되지 않는다.

`@Parcelize`를 `expect`/`actual typealias`로 우회하는 방식은 Kotlin 2.x에서 동작하지 않는다. 자세한 경위는 [KMP·Metro 트러블슈팅 #16](../metro/KMP_METRO_MIGRATION_TROUBLESHOOTING.md)을 따른다.

## 4. `@Parcelize`와 `@CommonParcelize` 비교

| 구분 | `@Parcelize` | `@CommonParcelize` |
| --- | --- | --- |
| 정의 | `kotlinx.parcelize.Parcelize` (Kotlin 제공) | `com.nexters.bandalart.core.navigation.CommonParcelize` (프로젝트 정의) |
| 사용 가능 위치 | Android 전용. `commonMain`에서 직접 참조할 수 없다 | `commonMain`에서 사용할 수 있다 |
| 필요한 설정 | Parcelize 플러그인 | Parcelize 플러그인 + Android 타깃 `additionalAnnotation` 옵션 |
| Android 결과 | `Parcelable` 구현 생성 | 동일 |
| iOS 결과 | 사용할 수 없다 | 생성되는 코드 없음 |

## 5. Circuit Screen 계열 타입

Circuit 0.36.1(`circuit-runtime-screen`) 소스 기준이다.

| 타입 | 역할 | Android 정의 |
| --- | --- | --- |
| `Screen` | 화면을 식별하는 키 타입. `Presenter.Factory`와 `Ui.Factory`의 키로 쓰인다 | `Screen : CircuitSaveable, Parcelable` |
| `ParcelableScreen` | common 코드에서 "Android에서는 `Parcelable`인 Screen"을 표현하는 `expect interface`. 다른 플랫폼에서는 그냥 `Screen`이다 | `ParcelableScreen : Screen, Parcelable` |
| `StaticScreen` | Presenter 없이 UI만 있는 화면을 표시하는 마커 인터페이스 | `StaticScreen : Screen` (빈 마커) |

- Android의 `Screen`이 이미 `Parcelable`이므로 common에 선언한 모든 Screen은 Android 컴파일에서 `Parcelable` 구현을 요구받는다. 이 구현을 `@CommonParcelize`가 생성한다.
- `ParcelableScreen`은 타입 계약이고 `@CommonParcelize`는 구현 생성이다. 둘을 함께 쓴다.
- `StaticScreen`은 `Presenter` 없이 `addUi<Screen> { ... }`만으로 화면을 등록하는 경우에 쓴다.

## 6. 프로젝트 적용 현황

| Screen | 위치 | 상속 | Screen 계약 파일 | composable 파일 |
| --- | --- | --- | --- | --- |
| `HomeScreen` | `feature/home` | `ParcelableScreen`, `StaticScreen` | `HomeScreen.kt` | `Home.kt` |
| `SplashScreen` | `feature/splash` | `ParcelableScreen` | `SplashScreen.kt` | `Splash.kt` |
| `OnboardingScreen` | `feature/onboarding` | `ParcelableScreen` | `OnboardingScreen.kt` | `Onboarding.kt` |
| `CompleteScreen` | `feature/complete` | `ParcelableScreen` | `CompleteScreen.kt` | `Complete.kt` |
| `CloudBackupScreen` | `core/navigation` | `ParcelableScreen` | `CloudBackupScreen.kt` | `CloudBackupScreenUi.kt` (`feature/backup`) |

5개 Screen 모두 `@CommonParcelize`를 사용한다.

Parcelize 플러그인과 `additionalAnnotation` 옵션은 다음 모듈의 `build.gradle.kts`에 각각 있다.

- `core/navigation`
- `feature/home`
- `feature/splash`
- `feature/onboarding`
- `feature/complete`
- `feature/backup`

## 7. 파일과 이름 규칙

feature마다 Screen 계약과 화면 composable을 이름이 같은 두 파일로 나눈다.

| 선언 | 파일 | 내용 |
| --- | --- | --- |
| `XxxScreen` | `XxxScreen.kt` | Circuit Screen 키와 `State`, `Event`, `Effect` 계약 |
| `Xxx`, `XxxContent` | `Xxx.kt` | `@CircuitInject` 진입 composable과 화면 본문, 관련 Preview |

- Screen 계약 파일에는 UI 코드를, composable 파일에는 Screen 선언을 두지 않는다.
- 이전에는 Home만 계약이 `HomeCircuitScreen.kt`, composable이 `HomeScreen.kt`에 있어 파일명이 선언과 뒤바뀌어 있었고, Splash·Onboarding·Complete는 한 파일에 함께 있었다. 이 규칙에 맞춰 정리했다.
- `CloudBackup`은 Screen 키가 `core/navigation`에, composable이 `feature/backup`의 `CloudBackupScreenUi.kt`에, 상태가 `CloudBackupUiState.kt`에 있어 이 규칙에 맞추지 않았다.

## 8. 새 Screen 추가 체크리스트

1. Screen을 선언하는 모듈에 `id("org.jetbrains.kotlin.plugin.parcelize")` 플러그인을 적용한다.
2. 같은 모듈의 Android 타깃에 3.2의 `additionalAnnotation` 옵션을 추가한다. 기존 모듈의 설정을 그대로 복사한다.
3. Screen에 `@CommonParcelize`를 붙이고 `ParcelableScreen`을 상속한다.
4. Screen 계약은 `XxxScreen.kt`에, composable은 `Xxx.kt`에 둔다.
5. 생성자 인자는 Parcelize가 지원하는 타입만 쓴다.
6. 옵션이 빠지면 Android에서 `Parcelable` 구현이 생성되지 않는다.

## 9. 확인 필요

- `HomeScreen`은 `StaticScreen`도 상속하지만 `HomePresenter : Presenter<HomeScreen.State>`가 있고, `AppGraphTest`가 `circuit.presenter(HomeScreen, ...)`가 null이 아님을 검증한다. `StaticScreen`의 정의(Presenter가 필요 없는 화면)와 어긋나 보인다. 이 마커는 `b02d72f4`(2026-08-05, Home 읽기 상태를 Circuit으로 전환)에서 추가됐다. 제거 가능 여부와 실제 동작 영향은 아직 확인하지 않았다.

## 10. 참고

- Circuit 0.36.1 소스: `circuit-runtime-screen/src/commonMain/.../Screen.kt`, `ParcelableScreen.kt`와 `androidMain/.../Screen.android.kt`, `ParcelableScreen.android.kt`
- [Parcelize setup for Kotlin Multiplatform](https://developer.android.com/kotlin/parcelize#setup_parcelize_for_kotlin_multiplatform)
- [KMP·Metro 트러블슈팅 #16](../metro/KMP_METRO_MIGRATION_TROUBLESHOOTING.md)
