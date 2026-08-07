# iOS Release 앱 크기 조사

## 목적

App Store에 공개된 iOS `1.0.1`의 표시 크기 75.1MB와 Android의 Play 다운로드 크기 약 19MB 차이를 설명하고, 다음 iOS archive에서 안전하게 용량을 줄이기 위한 측정·검증 순서를 정한다.

## 결론

현재 근거만으로 iOS Release 최적화나 난독화가 누락됐다고 판단할 수 없다.

- App Store의 75.1MB는 Apple이 처리한 앱 크기이고 Play의 기기별 압축 다운로드 크기와 산정 방식이 다르다.
- `.xcarchive`나 로컬 `.ipa` 전체 크기도 dSYM과 여러 기기용 요소를 포함할 수 있어 사용자 다운로드 크기가 아니다.
- 현재 프로젝트는 Kotlin/Native Release framework와 Xcode Release 구성을 사용하며, KMP framework는 static link이다.
- 과거 공개 버전 소스에는 폰트 리소스 중복이 있었고 현재 `main`에서 이미 제거됐다. 다음 iOS 배포본은 이 영향만으로도 과거 버전보다 작아질 가능성이 높다.
- iOS에서는 난독화보다 App Thinning, compiler/linker optimization, dead-code stripping, 미사용 dependency와 중복 resource 제거가 우선이다.

## 확인된 정적 리소스 차이

| 기준 | 폰트 파일 합계 | Compose 리소스 합계 |
| --- | ---: | ---: |
| App Store `1.0.1` 대응 과거 소스 `59fa456` | 28,936,144B | 32,427,748B |
| 현재 `main` | 14,468,072B | 17,291,942B |

과거 소스에서는 Pretendard 등 폰트가 `composeApp`와 `core/designsystem` 양쪽에 포함됐다. 현재는 단일 소스화되어 Compose 정적 리소스가 약 15.1MB 감소했다.

현재 Fluent Emoji 300개의 실제 WebP 파일 합계는 약 0.9MB이고 resource block 기준 약 1.4MB다. 향후 앱 크기 증가 요소이지만 과거 App Store의 75.1MB를 설명하는 주원인은 아니다.

## 현재 Release 구성

### Kotlin Multiplatform

`composeApp/build.gradle.kts`:

- `iosArm64`, `iosSimulatorArm64` target 사용
- `ComposeApp.framework` 생성
- `isStatic = true`

Xcode의 KMP embed/link task는 configuration에 따라 Debug/Release framework를 선택한다. 실제 archive에서 Release framework가 링크됐는지는 산출물로 재확인한다.

### Xcode

Project Release 설정에서 확인된 값:

- `SWIFT_COMPILATION_MODE = wholemodule`
- `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym`
- `ENABLE_NS_ASSERTIONS = NO`
- `MTL_ENABLE_DEBUG_INFO = NO`
- `COPY_PHASE_STRIP = NO`

`COPY_PHASE_STRIP`은 copy phase에서 복사되는 바이너리를 strip하는 설정이며 linked product 자체의 최적화 여부를 뜻하지 않는다. 다음 archive에서는 `DEAD_CODE_STRIPPING`, `STRIP_INSTALLED_PRODUCT`, `STRIP_STYLE`, `STRIP_SWIFT_SYMBOLS`, `ENABLE_TESTABILITY`를 포함한 effective Release settings와 최종 Mach-O를 함께 확인해야 한다.

dSYM은 앱에 포함해 내려받는 리소스가 아니라 crash symbolication에 필요한 외부 디버그 산출물이다. 크기 감소를 목적으로 dSYM 생성을 끄면 안 된다.

### 의존성

iOS target은 Swift Package Manager로 다음 제품을 링크한다.

- `FirebaseAnalytics`
- `FirebaseCrashlytics`

두 제품의 실제 바이너리 기여도와 제품 요구사항을 측정하되, 측정 없이 제거하거나 static/dynamic linkage를 변경하지 않는다.

## 비교 기준

다음 iOS 출시에서 같은 코드와 같은 기능 집합을 기준으로 측정한다.

1. 최신 `main`의 Release archive를 생성한다.
2. archive export에서 App Thinning을 `All compatible device variants`로 지정한다.
3. 생성된 `App Thinning Size Report.txt`에서 대표 iPhone의 compressed download size와 uncompressed installed size를 기록한다.
4. App Store Connect의 기기별 앱 크기와 대조한다.
5. Play Console에서도 동일 버전의 대표 기기 download/install size를 기록한다.
6. archive 내부를 executable, embedded framework, resources/assets로 나눠 상위 항목을 기록한다.

Apple은 App Store Connect/TestFlight의 기기별 variant와 App Thinning size report를 실제 사용자 크기에 가장 가까운 측정값으로 안내한다. TestFlight 빌드는 추가 데이터를 포함해 최종 App Store 빌드보다 클 수도 있다.

## 최적화 후보 순서

측정 기준 archive를 보존하고 아래 후보를 한 번에 하나씩 적용한다.

1. effective Release compiler/linker와 strip 설정 보정
2. 미사용 Firebase/SPM product 제거
3. 중복 이미지·폰트·Compose resource 제거
4. 이미지 format, 해상도, density qualifier 보정
5. Kotlin/Native binary size 관련 공식 옵션을 현재 Kotlin 버전에서 제한적으로 실험

각 변경은 동일 iPhone variant의 download/install size 변화, 실행 성능, 기능 회귀와 crash symbolication을 같이 기록한다. 감소 효과가 미미하거나 위험이 큰 변경은 적용하지 않는다.

## 비권장 접근

- App Store 75.1MB와 Play 19MB를 그대로 비교해 목표값을 정하는 것
- `.xcarchive` 전체 크기를 사용자 설치 크기로 간주하는 것
- dSYM 생성을 끄거나 업로드하지 않는 것
- 업로드 archive의 필수 architecture를 수동 제거하는 것
- iOS 난독화를 용량 최적화의 첫 단계로 도입하는 것
- 측정 없이 static framework를 dynamic framework로 전환하는 것

## 완료 기준

- 동일 코드 기준의 iOS/Android 기기별 download/install size가 기록된다.
- iOS 크기의 주요 기여 항목이 실행 파일·framework·resource 단위로 식별된다.
- 효과가 확인된 안전한 최적화만 반영된다.
- dSYM, Crashlytics symbolication, 핵심 기능과 기존 데이터 호환에 회귀가 없다.
- 최종 감소량을 App Thinning/App Store Connect 수치로 문서화한다.

구체적인 MB 목표는 기준선을 확보한 뒤 정한다.

## 관련 이슈

- [#234 iOS 앱 크기 분석 및 Release archive 최적화](https://github.com/Nexters/BandalArt-KMP/issues/234)
- [#214 iOS Circuit + Metro 마이그레이션 실기기·배포 검증](https://github.com/Nexters/BandalArt-KMP/issues/214)
- [#212 Fluent UI Emoji 기반 이모지 선택기 리뉴얼](https://github.com/Nexters/BandalArt-KMP/issues/212)

## 공식 자료

- [Apple: Reducing your app's size](https://developer.apple.com/documentation/xcode/reducing-your-app-s-size)
- [Apple: Monitoring your app's storage metrics](https://developer.apple.com/documentation/xcode/monitoring-your-app-s-storage-metrics)
- [Apple: Testing a release build](https://developer.apple.com/documentation/xcode/testing-a-release-build)
- [Apple: Build settings reference](https://developer.apple.com/documentation/xcode/build-settings-reference)
- [Kotlin: Compose Multiplatform resources](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html)
- [Kotlin: Set up Compose Multiplatform resources](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html)
