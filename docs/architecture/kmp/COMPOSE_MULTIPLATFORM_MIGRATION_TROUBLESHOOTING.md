# Compose Multiplatform 마이그레이션 문제 해결

문서 유형: Troubleshooting

이 문서는 [이슈 #168](https://github.com/Nexters/BandalArt-KMP/issues/168)에 기록된 Compose Multiplatform (CMP) 전환 문제를 현재 `main` 기준으로 진단한다. 먼저 증상별 확인 위치를 찾고, 현재 프로젝트가 채택한 해결 경로를 적용한다.

## 현재 기준선부터 확인하기

프로젝트 구조와 도구가 초기 마이그레이션 시점에서 바뀌었다. 과거 해결책을 다시 적용하기 전에 다음 기준을 확인한다.

| 항목 | 현재 기준 |
| --- | --- |
| 앱 구조 | `androidApp`은 Android host다. `composeApp`, `core:*`, `feature:*`, `iosWidgetShared`는 Kotlin Multiplatform (KMP) 모듈이다. |
| 사용자 인터페이스 (UI) | Compose Multiplatform 1.10.3과 `commonMain`의 AndroidX `@Preview`를 사용한다. |
| 의존성 주입 | Koin 대신 Metro `AppGraph`가 Android와 iOS 객체를 생성한다. |
| 데이터 | Room KMP와 DataStore를 사용한다. 플랫폼 factory가 경로와 Android `Context`를 제공한다. |
| iOS 연결 | Xcode build phase가 `:composeApp:embedAndSignAppleFrameworkForXcode`를 실행하는 direct integration 방식이다. |
| 플랫폼 기능 | `expect`/`actual`과 플랫폼 source set으로 앱 버전, 이미지 처리, 저장소 경로를 분리한다. |

현재 의존성 그래프와 Metro 관련 문제는 [KMP·Metro 트러블슈팅](../metro/KMP_METRO_MIGRATION_TROUBLESHOOTING.md)을 먼저 확인한다. 테스트 source set과 명령은 [KMP 테스트 가이드](KMP_TESTING_GUIDE.md)를 따른다.

## 증상별 진단 순서

아래 표에서 증상과 가장 가까운 항목을 찾는다. 각 행의 확인 위치에서 현재 설정을 먼저 대조한다.

| 증상 | 먼저 확인할 위치 | 현재 해결 경로 |
| --- | --- | --- |
| `Res.*`가 생성되지 않음 | `composeResources`, `KmpComposePlugin.kt` | 잘못된 resource 참조와 디렉터리를 고친 뒤 해당 모듈을 다시 빌드한다. |
| `commonMain` Preview가 보이지 않음 | `KmpComposePlugin.kt`, 대상 모듈 plugin | Android target, AndroidX Preview annotation, tooling dependency를 함께 확인한다. |
| iOS target에서 라이브러리를 찾지 못함 | 해당 모듈 `build.gradle.kts`, Gradle metadata | Bill of Materials (BOM) 또는 Android 전용 artifact 대신 iOS target을 제공하는 개별 artifact를 선언한다. |
| Room constructor의 `actual`이 생성되지 않음 | `RoomPlugin.kt`, `kspKmp()` | 사용하는 Android와 iOS target마다 Room compiler를 KSP에 연결한다. |
| 플랫폼 객체를 common code에서 만들 수 없음 | `AppGraph`, 플랫폼 factory | 플랫폼 source set에서 객체를 만들고 공통 graph에는 interface나 `expect`/`actual` 계약만 노출한다. |
| Xcode에서 `No such module 'ComposeApp'` 발생 | Xcode build phase, 공유 scheme | direct integration script와 Xcode build 환경을 확인한 뒤 Xcode에서 다시 빌드한다. |
| iOS 텍스트 입력이나 키보드 동작이 다름 | 공통 TextField, `Modifier.kt` | 선택·조합 상태와 키보드 inset을 공통으로 보존하고 양 플랫폼에서 검증한다. |
| 이미지 공유가 실패하거나 색이 달라짐 | 플랫폼별 `ImageHandlerProvider` | Android intent flag와 iOS bitmap channel·alpha 설정을 각각 확인한다. |
| iOS archive가 메모리 부족으로 실패함 | `gradle.properties`, Xcode build log | 현재 4 GB Gradle heap을 유지하고 실패 지점이 Kotlin compile인지 Xcode link인지 구분한다. |
| Firebase가 iOS에서 연결되지 않음 | iOS Xcode project와 Firebase 설정 | [Firebase iOS 통합 조사](../../releases/ios/FIREBASE_IOS_INTEGRATION_RESEARCH.md)의 bundle ID, linker, debug symbol (dSYM) 절차를 따른다. |

## `Res` accessor가 생성되지 않을 때

CMP resource accessor는 resource 디렉터리와 Extensible Markup Language (XML) 참조가 유효해야 생성된다. 이 프로젝트의 KMP Compose convention은 resource runtime과 Android resource 처리를 모든 적용 모듈에 설정한다.

다음 순서로 확인한다:

1. resource를 해당 모듈의 `src/commonMain/composeResources` 아래에 둔다.
2. 이미지, 문자열, 폰트, 일반 파일을 각각 `drawable`, `values`, `font`, `files`에 둔다.
3. XML과 Kotlin에서 존재하지 않는 resource를 참조하지 않는지 확인한다.
4. 해당 모듈의 `build.gradle.kts`가 `bandalart.kmp.compose` 또는 이를 포함한 convention plugin을 적용하는지 확인한다.
5. Kotlin이나 Compose 버전을 바꾼 직후에만 `./gradlew clean` 후 다시 빌드한다.

현재 설정은 `build-logic/src/main/kotlin/com/nexters/bandalart/buildlogic/KmpComposePlugin.kt`에 있다. resource 규칙은 [Compose Multiplatform resource 설정](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html)을 기준으로 유지한다.

## `commonMain` Preview가 보이지 않을 때

초기 #168에는 Preview를 `androidApp`으로 옮기는 방안이 적혀 있었다. 현재는 Compose Multiplatform 1.10.3이 AndroidX `@Preview`를 지원하므로 preview 함수도 `commonMain`에 둔다.

다음 설정을 함께 확인한다:

- `commonMain`은 `org.jetbrains.compose.ui:ui-tooling-preview`에 의존한다.
- preview source는 `androidx.compose.ui.tooling.preview.Preview`를 import한다.
- KMP 모듈에 Android target이 존재한다.
- Android tooling runtime이 Android target classpath에 포함된다.
- preview 함수는 런타임 graph나 플랫폼 객체 대신 고정된 UI state를 받는다.

프로젝트의 공통 설정은 `build-logic/src/main/kotlin/com/nexters/bandalart/buildlogic/KmpComposePlugin.kt`에 있다. Compose 또는 Android Gradle Plugin (AGP)을 올린 뒤 Preview가 사라졌다면 [Compose UI previews](https://kotlinlang.org/docs/multiplatform/compose-previews.html)에서 해당 AGP target의 tooling dependency 구성을 다시 확인한다.

Preview rendering은 통합 개발 환경 (IDE)의 KMP 지원에도 영향을 받는다. 같은 source가 compile돼도 지원하지 않는 IDE 버전에서는 Preview panel이 나타나지 않을 수 있다.

## iOS target에서 의존성을 찾지 못할 때

Android에서 resolve된 artifact가 iOS용 Kotlin/Native variant도 제공한다고 가정하면 안 된다. Gradle이 iOS compilation에서 artifact를 찾지 못하면 해당 라이브러리의 Gradle module metadata와 지원 target부터 확인한다.

이 프로젝트는 Landscapist bundle이나 BOM을 `commonMain`에 넣지 않는다. `feature/complete/build.gradle.kts`에서 `landscapist-coil3`와 `landscapist-placeholder`처럼 실제로 필요한 multiplatform artifact를 각각 선언한다.

다음 기준을 적용한다:

- version catalog bundle은 좌표 묶음일 뿐 플랫폼 호환성을 추가하지 않는다.
- BOM은 버전 정렬만 담당하므로 iOS variant 누락을 해결하지 않는다.
- Android 전용 artifact는 `androidMain`으로 옮긴다.
- 같은 기능의 multiplatform artifact가 없으면 공통 interface와 플랫폼 구현으로 분리한다.

## non-composable 코드에서 문자열이 필요할 때

UI composable은 `stringResource()`를 사용하고 effect나 suspend 경로는 `getString()`을 사용한다. 문자열을 얻기 위해 전체 화면 함수를 `@Composable`로 바꾸거나 하드코딩하지 않는다.

현재 예시는 다음 위치에서 확인한다:

- `feature/complete/src/commonMain/kotlin/com/nexters/bandalart/feature/complete/HandleCompleteEffects.kt`: effect handler에서 `getString()` 후 toast 표시
- `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/HomeScreen.kt`: UI effect에서 `getString()` 후 toast, snackbar, 공유 문구 처리
- `feature/home/src/androidMain/kotlin/com/nexters/bandalart/feature/home/FlexibleUpdateEffect.android.kt`: Android update 메시지 처리

resource Application Programming Interface (API)는 [Compose Multiplatform resources](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html)를 기준으로 사용한다. CMPToast의 하단 여백과 시각 디자인은 resource 접근 문제와 다른 제품 개선이므로 별도 이슈로 다룬다.

## Room constructor가 생성되지 않을 때

Room compiler가 한 target에서 빠지면 `RoomDatabaseConstructor`의 generated `actual`을 찾지 못한다. `core/database`에 compiler dependency를 반복 선언하지 말고 `bandalart.room` convention을 확인한다.

현재 `build-logic/src/main/kotlin/com/nexters/bandalart/buildlogic/RoomPlugin.kt`가 다음 책임을 가진다:

- Room과 Kotlin Symbol Processing (KSP) plugin 적용
- `commonMain`에 Room runtime과 bundled SQLite 추가
- `kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`에 Room compiler 연결
- Room schema 출력 위치 설정

새 target을 추가하면 `build-logic/src/main/kotlin/com/nexters/bandalart/buildlogic/configure/KmpGradleDsl.kt`의 `kspKmp()`에도 같은 target을 추가한다. target별 compiler 연결과 constructor 형식은 [Room database for KMP](https://developer.android.com/kotlin/multiplatform/room)를 따른다.

## Android `Context`나 플랫폼 객체가 필요할 때

`commonMain`에 Android `Context`를 전달하지 않는다. 플랫폼 source set이 필요한 객체를 만든 뒤 Metro graph가 공통 계약으로 노출한다.

현재 Android의 `createAndroidAppGraph(application)`은 다음 factory를 application `Context`로 생성한다:

- `BandalartDatabaseFactory`
- `BandalartDataStoreFactory`
- `AppVersionProvider`
- `ImageHandlerProvider`

iOS의 `createIosAppGraph()`는 같은 공통 계약의 iOS 구현을 생성한다. #168의 Koin module과 `NoDefinitionFoundException` 해결법은 현재 구조에 적용하지 않는다. 현재 graph 오류는 [KMP·Metro 트러블슈팅](../metro/KMP_METRO_MIGRATION_TROUBLESHOOTING.md)의 binding과 graph validation 절차로 진단한다.

## Xcode에서 `ComposeApp` module을 찾지 못할 때

현재 iOS host는 미리 생성한 framework를 수동으로 참조하지 않는다. Xcode의 build phase가 Gradle `embedAndSignAppleFrameworkForXcode` task를 실행해 현재 configuration과 architecture에 맞는 framework를 생성한다.

다음 순서로 확인한다:

1. Xcode project의 **Build Phases**에서 Kotlin framework script가 활성화됐는지 확인한다.
2. script가 저장소 root에서 `:composeApp:embedAndSignAppleFrameworkForXcode`를 호출하는지 확인한다.
3. `iosApp` 공유 scheme으로 다시 빌드한다.
4. Kotlin 또는 Compose major 버전을 바꾼 직후라면 Xcode의 **Clean Build Folder**와 `./gradlew clean`을 한 번 실행한다.

평소에 `linkDebugFrameworkIosSimulatorArm64`를 먼저 수동 실행하는 방식은 현재 표준 경로가 아니다. Xcode 환경 변수가 필요한 direct integration task를 사용하며, 구성은 [Kotlin Multiplatform direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html)을 따른다.

Xcode project navigator에 Kotlin source가 보이지 않는 현상은 framework import 실패와 다르다. Kotlin source는 Android Studio나 IntelliJ IDEA에서 열고, Xcode에서는 Swift host와 build phase를 관리한다.

## 공통 Compose API가 플랫폼별로 다를 때

공통 API인지 이름만 보고 판단하지 않는다. 사용 중인 CMP 버전의 API surface와 실제 Android/iOS 동작을 함께 확인한다.

현재 프로젝트는 다음 선택을 유지한다:

- 화면 크기 기반 배치는 `BoxWithConstraints`에서 현재 composable의 constraint를 읽는다.
- 키보드 노출 여부는 Input Method Editor (IME) inset인 `WindowInsets.ime.getBottom(density) > 0`으로 계산한다.
- `LineBreak` 지정은 `CellText.kt`에서 비활성화돼 있다. 최신 CMP에서 compile된다는 이유만으로 다시 켜지 않고 한·영·일 줄바꿈을 양 플랫폼에서 확인한다.
- 플랫폼 전용 API가 필요하면 `androidMain`과 `iosMain` 구현으로 분리한다.

플랫폼 기본 동작의 차이는 [Compose Multiplatform platform specifics](https://kotlinlang.org/docs/multiplatform/compose-platform-specifics.html)를 기준으로 판단한다.

## iOS 텍스트 입력이 깨질 때

iOS 입력기는 selection과 composition 상태를 사용한다. TextField 값을 매 recomposition마다 `String`으로 재구성하면 커서 위치와 한글 조합 상태가 사라질 수 있다.

다음 항목을 확인한다:

- 편집 중인 값이 selection과 composition을 보존하는가
- `onValueChange`에서 같은 텍스트를 새 state로 덮어쓰지 않는가
- focus 이동과 bottom sheet scroll이 서로 state를 갱신하지 않는가
- 한글 조합 중 presenter나 저장소 값으로 즉시 정규화하지 않는가

bottom sheet의 한글 자소 분리 문제는 CMP 업데이트로 해결했고, 커서 이동 문제는 [PR #171](https://github.com/Nexters/BandalArt-KMP/pull/171)에서 입력 상태를 보존하도록 수정했다. 관련 [이슈 #170](https://github.com/Nexters/BandalArt-KMP/issues/170)은 완료됐다.

## 이미지 공유와 저장 결과가 다를 때

공통 `ImageBitmap`을 플랫폼 이미지로 바꾸는 단계와 플랫폼 공유 API 호출을 나눠서 확인한다. 두 단계의 실패 원인이 다르다.

Android는 application `Context`에서 chooser를 열기 때문에 `FLAG_ACTIVITY_NEW_TASK`를 포함한다. iOS는 `readPixels()` 결과를 standard RGB (sRGB), premultiplied alpha, little-endian 32-bit bitmap으로 해석한 뒤 `UIImage`를 만든다.

색이 옅어지거나 channel이 바뀌면 Portable Network Graphics (PNG)와 Joint Photographic Experts Group (JPEG) 형식을 바꾸기 전에 bitmap channel order와 alpha premultiplication을 확인한다. App Store 스크린샷의 alpha 거부는 런타임 공유 이미지와 별개이며, store asset만 alpha가 없는 JPEG로 내보낸다.

## Kotlin/Native 도구 파일이 없을 때

`~/.konan` 내부 파일을 저장소에 복사하지 않는다. `env_blacklist`처럼 Kotlin/Native 배포 파일이 없다는 오류는 toolchain 다운로드나 cache가 불완전하다는 뜻이다.

다음 순서로 복구한다:

1. 오류에 표시된 Kotlin 버전과 `gradle/libs.versions.toml`의 Kotlin 버전이 같은지 확인한다.
2. Gradle에서 사용하는 iOS target compile 또는 link task를 다시 실행해 해당 toolchain을 받는다.
3. 같은 버전에서 계속 실패하면 손상된 해당 버전 cache만 제거하고 Gradle task로 다시 받는다.
4. 다른 샘플 프로젝트를 만들어 cache를 우회 설치하는 방법은 재현 가능한 해결 절차로 사용하지 않는다.

## archive와 생성 파일 문제를 구분하기

iOS archive의 `Java heap space`는 Kotlin compile 단계인지 Xcode link 단계인지 먼저 구분한다. 현재 `gradle.properties`는 Gradle Java Virtual Machine (JVM) heap을 4 GB로 설정한다. 이를 더 늘리기 전에 해당 task의 heap 사용량과 runner memory를 확인한다.

생성 파일 때문에 lint가 실패하면 생성 directory를 source로 취급한 경로부터 찾는다. 생성물을 `.gitignore`에만 추가해 lint 입력을 고치려 하지 말고, convention plugin과 static analysis task의 source 범위에서 생성 directory를 제외한다.

## Firebase와 iOS 앱 크기는 별도 문서를 사용하기

#168의 Firebase 미해결 항목과 iOS 크기 항목은 각각 독립 조사와 검증이 필요한 주제다. 이 페이지에서는 초기 CMP compile 문제로 다루지 않는다.

- iOS Firebase 설정과 검증: [Firebase iOS 통합 조사](../../releases/ios/FIREBASE_IOS_INTEGRATION_RESEARCH.md)
- iOS와 Android 크기 기준선: [iOS release size baseline](../../releases/ios/IOS_RELEASE_SIZE_BASELINE.md)
- iOS 크기 개선 결과: [iOS release size 최적화 조사](../../releases/ios/IOS_RELEASE_SIZE_OPTIMIZATION_RESEARCH.md)

현재 App Store Connect의 iOS 압축 파일 크기는 초기 조사보다 줄었지만 Android App Bundle과 표시 기준이 다르다. 두 콘솔의 숫자를 직접 배수로 비교하지 않고 App Store의 App Thinning 결과와 Play의 기기별 download size를 비교한다.

## #168 항목의 현재 상태

초기 migration checklist는 아래처럼 정리한다. 이 표는 새 작업 목록이 아니라 과거 항목의 현재 분류다.

| 초기 항목 | 현재 상태 |
| --- | --- |
| KMP 모듈 생성, 모놀리식 전환 후 재모듈화 | 완료. host와 KMP 모듈 경계가 현재 `settings.gradle.kts`에 반영됐다. |
| resource accessor, Preview | resource 생성 설정과 `commonMain` Preview 구성을 반영했다. IDE Preview rendering은 지원되는 IDE와 tooling 조합에서 확인한다. |
| Landscapist iOS resolve | 완료. 필요한 artifact를 개별 선언한다. |
| Room, DataStore, 플랫폼 객체 주입 | 완료. Koin 경로를 제거하고 Metro graph가 소유한다. |
| lifecycle runtime artifact | 완료. `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose`를 사용한다. |
| `removeUnusedEntriesAfterDays` Gradle 경고 | 완료. 폐기된 설정을 제거했으며 현재 설정에 다시 추가하지 않는다. |
| `LocalDateTime` parse 오류 | 완료. 플랫폼 구현이 명시적인 `toString()` 형식을 제공하고 host test가 변환을 검증한다. |
| expect/actual class beta 경고 | 완료. expect/actual을 사용하는 KMP 모듈이 `-Xexpect-actual-classes` compiler option을 명시한다. |
| `LocalConfiguration`, IME visibility | 완료. `BoxWithConstraints`와 `WindowInsets.ime` 경로를 사용한다. |
| `LineBreak` | 보류. 현재 UI에서 비활성화하며 독립적인 시각 회귀 검증 없이는 적용하지 않는다. |
| iOS framework 연결 | 완료. Xcode direct integration을 사용한다. |
| 이미지 공유, 저장, 색상 | 완료. 플랫폼별 구현이 유지된다. |
| 작은 화면의 완료 animation 겹침 | 과거 UI 수정 항목이다. 다시 재현되면 기기 크기와 화면을 포함한 UI 이슈로 분리한다. |
| iOS 입력 조합과 커서 | 완료. CMP 업데이트와 PR #171이 반영됐다. |
| Xcode에 Kotlin source가 보이지 않음 | 정상 도구 경계다. Kotlin은 Android Studio나 IntelliJ IDEA에서 편집한다. |
| 생성 파일로 인한 lint 실패 | 완료. 생성 directory를 정적 분석 source에서 제외한다. |
| Firebase iOS | 코드 설정 완료, 콘솔 수신 검증은 Firebase 전용 문서에서 추적한다. |
| App Store screenshot alpha | store asset을 alpha가 없는 형식으로 내보낸다. 런타임 이미지 변환과 분리한다. |
| iOS privacy manifest | 완료. `iosApp/iosApp/PrivacyInfo.xcprivacy`를 유지한다. |
| iOS 앱 크기 | 초기 최적화 완료, 수치는 release size 문서에서 추적한다. |
| CMPToast 디자인 | 기능 장애가 아닌 UI 개선 후보다. 필요하면 별도 이슈로 분리한다. |
| Koin annotation 전환 | 폐기. Koin을 제거하고 Metro로 전환했다. |

새 migration 문제를 발견하면 이슈 본문에 체크 항목을 다시 늘리지 않는다. 재현 조건, 오류 원문, 영향 platform, 실패 task와 최소 해결책을 이 문서의 해당 증상에 추가한다.
