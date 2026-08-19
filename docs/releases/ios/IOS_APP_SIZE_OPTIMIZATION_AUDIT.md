# iOS 앱 크기 최적화 적용 상태 감사

## 목적과 기준

- 감사일: 2026-08-19
- 대상: iOS `1.3.0` Release와 Widget Extension
- 기준: Apple의 [앱 크기 축소 개요](https://developer.apple.com/documentation/xcode/reducing-your-app-s-size), [기본 최적화](https://developer.apple.com/documentation/xcode/doing-basic-optimization-to-reduce-your-app-s-size), [고급 최적화](https://developer.apple.com/documentation/xcode/doing-advanced-optimization-to-further-reduce-your-app-s-size)
- 프로젝트 근거: [iOS 크기 기준선](./IOS_RELEASE_SIZE_BASELINE.md), [최적화 조사](./IOS_RELEASE_SIZE_OPTIMIZATION_RESEARCH.md), [최적화 전략](./IOS_RELEASE_SIZE_OPTIMIZATION_STRATEGY.md), [이슈 #234](https://github.com/Nexters/BandalArt-KMP/issues/234)

절감량은 같은 코드와 같은 산출물 조건의 A/B가 있는 경우에만 확정값으로 쓴다. 실행 파일 ZIP은 방향을 보기 위한 proxy이며 최종 다운로드 크기는 App Thinning Size Report와 App Store Connect의 기기별 App File Sizes로 판정한다.

## 결론

| 항목 | 확인 결과 | 결정 |
| --- | --- | --- |
| 앱 본체 Kotlin/Native `smallBinary` | 대표 iPhone download `-1,578,503 B(-6.95%)`, install `-5,449,840 B(-9.31%)` | **채택·유지** |
| `IosWidgetShared smallBinary` | Widget Mach-O `-260,048 B(-7.58%)`, ZIP proxy `-101,193 B(-7.19%)`; 전체 앱 약 `-0.5%` | 기능 검증 후 **채택** |
| 미사용·중복 Compose resource 9개 | raw `526,524 B`, 현재 ZIP entry 약 `50,139 B` | **채택** |
| Swift `-Osize` | Swift `__text`는 조금 줄었지만 최종 main·Widget Mach-O와 ZIP은 증가 | **보류** |
| Pretendard variable font | 현재 정적 OTF 4개보다 공식 variable TTF가 `405,360 B` 큼 | **보류** |
| Asset Catalog 전면 이관, HEIF, ODR | 접근 가능한 크기보다 KMP 분기·호환·운영 복잡성이 큼 | **보류** |

## 현재 Release와 App Thinning 기준선

effective Release 설정은 다음과 같다.

- Swift compiler: `-O -whole-module-optimization`
- Clang linker: `-Os -dead_strip`
- `DEAD_CODE_STRIPPING=YES`
- `STRIP_INSTALLED_PRODUCT=YES`
- `STRIP_SWIFT_SYMBOLS=YES`
- `STRIP_STYLE=all`
- `ENABLE_TESTABILITY=NO`
- `DEBUG_INFORMATION_FORMAT=dwarf-with-dsym`

dSYM은 사용자 다운로드 bundle이 아니며 Crashlytics와 crash symbolication에 필요하므로 제거 대상이 아니다. App Store와 TestFlight는 App Thinning을 자동 적용하고, 개발 중에는 Xcode의 App Thinning Size Report를 사용한다.

### 앱 본체 `smallBinary` 전후

| 대표 iPhone 지표 | 적용 전 | 적용 후 | 차이 |
| --- | ---: | ---: | ---: |
| compressed download | 22,712,896 B | 21,134,393 B | -1,578,503 B (-6.95%) |
| uncompressed install | 58,520,842 B | 53,071,002 B | -5,449,840 B (-9.31%) |
| main executable | 45,049,200 B | 39,599,360 B | -5,449,840 B |

감소분이 main executable 감소분과 일치한다. Kotlin/Native·Compose·Skiko가 정적으로 링크된 실행 코드가 줄어든 결과이며, 적용 코드는 [ComposeApp framework 설정](../../../composeApp/build.gradle.kts)에 있다. Kotlin은 `smallBinary`가 LLVM의 size-oriented `-Oz`를 사용하고 runtime 성능에 영향을 줄 수 있다고 설명한다. [Kotlin 공식 문서](https://kotlinlang.org/docs/native-improving-compilation-time.html#reduce-the-size-of-release-binaries)

### 현재 큰 구성요소

| 구성요소 | 설치 bundle 내 크기 | 해석 |
| --- | ---: | --- |
| main executable | 39,599,360 B | 설치 크기의 약 74.6%; K/N·Compose·Skiko 포함 |
| Compose resources | 9,529,819 B | Asset Catalog 밖 KMP 공통 리소스 |
| 폰트 6개 | 6,688,112 B | Compose resources 중 가장 큼 |
| Fluent Emoji WebP 300개 | 935,622 B | 이미 압축된 WebP |
| Widget Extension bundle | 3,481,739 B | 별도 K/N framework 포함 |
| native `Assets.car` | 34,008 B | App Icon 중심, 이미 catalog/thinning 적용 |
| On-Demand Resources | 0 B | asset pack 없음 |

## 크기 최적화 플래그 구분

`-Os`, `-Osize`, `-Oz`는 이름이 비슷하지만 서로 다른 compiler와 코드에 적용된다. Swift `-Osize`의 `O`는 숫자 `0`이 아니라 대문자 알파벳이다.

| 설정 | 대상 | 의미와 현재 상태 | BandalArt 결과 |
| --- | --- | --- | --- |
| Clang `-Os` | C, Objective-C, C++ | Apple의 `Fastest, Smallest`. 현재 Release linker invocation에서 확인 | 유지. prebuilt framework나 K/N output에 소급 적용되지는 않음 |
| Swift `-O` | Swift source | speed-oriented Release 최적화. 현재 기본값 | 비교 기준 |
| Swift `-Osize` | Swift source | code size 우선. Apple WWDC18의 당시 compatibility suite에서 Swift machine code 10~30% 감소, runtime은 보통 약 5% 저하 | 현재 앱 최종 파일은 줄지 않아 보류 |
| Kotlin/Native `smallBinary`가 선택하는 `-Oz` | K/N framework | Release binary size 우선. runtime 영향 가능 | 본체와 Widget에서 유의미한 감소 확인 |

Apple의 10~30% 수치는 2018년 Swift machine code suite의 역사적 참고치이지 현재 compiler, 전체 앱, K/N framework, resource 또는 prebuilt dependency의 절감률 보장이 아니다. [WWDC18: What's New in Swift](https://developer.apple.com/videos/play/wwdc2018/401/)

### Swift `-Osize` A/B

기준 commit `e20ba038`에서 `SWIFT_OPTIMIZATION_LEVEL=-Osize`를 command-line override해 source Swift package까지 포함한 Release archive를 만들었다.

| 산출물 | 기본 `-O` | `-Osize` | 차이 |
| --- | ---: | ---: | ---: |
| main Swift `__text` | 23,858,764 B | 23,846,384 B | -12,380 B (-0.05%) |
| main Mach-O | 39,502,816 B | 39,505,192 B | +2,376 B |
| main ZIP proxy | 13,541,071 B | 13,542,051 B | +980 B |
| Widget Swift `__text` | 2,741,268 B | 2,740,208 B | -1,060 B (-0.04%) |
| Widget Mach-O | 3,429,120 B | 3,429,608 B | +488 B |
| Widget ZIP proxy | 1,406,665 B | 1,407,480 B | +815 B |

Swift `__text` 감소가 Mach-O segment 정렬과 다른 section 변화보다 작아서 최종 파일에는 이득이 남지 않았다. source Swift package까지 override한 상한에 가까운 실험에서도 이득이 없으므로 target에만 설정하는 더 좁은 적용은 채택하지 않는다.

### `IosWidgetShared smallBinary` A/B

| 산출물 | 적용 전 | 적용 후 | 차이 |
| --- | ---: | ---: | ---: |
| K/N static framework binary | 7,728,240 B | 6,881,624 B | -846,616 B (-10.96%) |
| strip 후 Widget Mach-O | 3,429,120 B | 3,169,072 B | -260,048 B (-7.58%) |
| Widget executable ZIP proxy | 1,406,665 B | 1,305,472 B | -101,193 B (-7.19%) |

기존 App Thinning 대표 variant에 단순 대입하면 설치·다운로드 모두 약 `0.5%` 추가 감소다. 범위는 작지만 한 줄 설정이고 본체에서 이미 채택한 방식이므로, Widget placeholder/snapshot/timeline, interactive action과 shared database 접근을 확인한 뒤 채택한다.

## Apple 기본·고급 권고 대조

| Apple 권고 | 현재 상태와 기대 효과 | 결정 |
| --- | --- | --- |
| App Thinning Size Report와 App Store Connect로 측정 | 동일 variant A/B를 사용 중. `.app`, `.xcarchive`, raw IPA 크기를 사용자 크기로 보지 않음 | **유지** |
| Release compiler/linker와 dead-code stripping 확인 | Swift `-O`, Clang `-Os`, `-dead_strip`, product/symbol strip 적용 | **유지** |
| 미사용 asset·파일 제거 | 미사용 8개와 runtime에서 읽지 않는 Fluent Emoji JSON 중복본 식별 | clean compile/test 후 **채택** |
| 데이터를 source가 아닌 asset file로 제공 | 현재 runtime JSON은 생성 Kotlin과 중복이라 JSON만 제거. 생성 Kotlin 자체의 runtime file 전환은 suspend load와 parse 비용을 동반 | 중복 제거 **채택**, 구조 전환 **별도 실험** |
| Asset Catalog metadata·slicing 활용 | native App Icon은 적용. KMP 공통 resource 전면 이관은 Android 공유를 깨뜨림 | native asset만 **유지** |
| 효율적 image format·압축 | Emoji는 이미 WebP 300개, 합계 0.94 MB. HEIF는 Compose/Android decoder 검증 필요 | **후순위** |
| update package에서 불필요한 파일 변화 최소화 | generated catalog 순서와 도구 output을 결정적으로 유지 | 원칙 **채택** |
| ODR로 드물게 쓰는 대형 resource 지연 전달 | 현재 ODR 0 B이고 후보 Emoji 전체도 약 0.94 MB | 비용 대비 **보류** |
| dSYM과 archive 보조 파일을 앱 크기로 오해하지 않기 | dSYM 생성과 Crashlytics symbol upload 유지 | **유지** |

## 폰트와 미사용 리소스

사용 중인 Pretendard Regular/Medium/SemiBold/Bold OTF 합계는 `6,333,976 B`다. 공식 Pretendard 1.3.9 variable TTF 하나는 `6,739,336 B`로 `405,360 B` 더 크므로 단순 교체하지 않는다. glyph subset은 한국어·영어·일본어와 임의 사용자 입력 문자를 누락시킬 위험이 있어 제품 입력 범위를 보장할 수 있을 때만 다시 검토한다. [Pretendard 1.3.9](https://github.com/orioncactus/pretendard/releases/tag/v1.3.9)

정적 검색과 생성 accessor 대조로 확인한 정리 대상은 다음과 같다.

- `empty_box.json`, `splash.json`
- `ic_image.xml`, `ic_smile.xml`, `compose-multiplatform.xml`
- `ic_add.xml`, `ic_splash.xml`, `ic_cell_add.xml`
- runtime에서 읽지 않고 generated Kotlin과 중복인 `fluent_emoji_catalog.json`

9개 합계는 raw `526,524 B`, 현재 ZIP의 파일·AppleDouble entry 합계는 약 `50,139 B`다. Fluent Emoji 생성 도구는 `tools/fluent-emoji/measurement/catalog-candidate.json`을 원본으로 유지하고 WebP와 generated Kotlin만 runtime 경로로 동기화한다.

## Android와 iOS의 차이

| 구분 | Android | iOS | BandalArt 적용점 |
| --- | --- | --- | --- |
| 코드 축소 | R8 whole-program shrink·optimize·obfuscate | compiler/linker optimization, dead-code/symbol stripping, K/N `smallBinary` | iOS에는 R8 직접 대응 도구가 없고 난독화보다 AOT binary 최적화가 중심 |
| 리소스 | `shrinkResources`, AAPT2, density/language split | Asset Catalog와 App Thinning; Compose file resource는 자동 축소가 제한적 | iOS 공통 resource는 수동 사용성 감사가 중요 |
| 전달 | AAB가 ABI·density·language split 생성 | App Store가 기기별 slicing/thinning | raw AAB와 IPA가 아니라 같은 기기의 download/install size 비교 |
| Compose | DEX가 R8 분석 대상 | Compose·Skiko·K/N가 Mach-O에 정적 링크 | `smallBinary` 효과가 main executable에 직접 나타남 |
| 심볼 | mapping/native symbols 별도 업로드 | dSYM 별도 업로드 | 크기를 위해 심볼 산출물을 버리지 않음 |
| 측정 | Play Console, bundletool, APK Analyzer | App Thinning report, App Store Connect | 플랫폼별 기기 variant 전후 감소율로 판단 |

## 채택 범위와 검증

이번 후속 변경에는 다음만 포함한다.

1. `IosWidgetShared` Release framework에 `binaryOption("smallBinary", "true")` 적용
2. 미사용·중복 Compose resource 9개 제거
3. Fluent Emoji sync 도구가 runtime JSON을 다시 복사하지 않도록 수정

`-Osize`, variable font, glyph subset, ODR, HEIF, dependency 제거는 포함하지 않는다.

완료한 대표 검증:

- `:iosWidgetShared:linkReleaseFrameworkIosArm64`
- `:core:designsystem:generateComposeResClass`
- `:core:ui:testAndroidHostTest`
- `:composeApp:compileKotlinIosArm64`
- `bash -n tools/fluent-emoji/measure-catalog.sh`
- `git diff --check`

1.3.0 release candidate에서는 App·Widget cold launch, Widget timeline/action, Fluent Emoji 검색·recent ordering, 주요 화면과 Crashlytics/dSYM을 확인한다. 이후 같은 기기 App Thinning report와 App Store Connect 수치를 이슈 #234에 기록한다.

## 공식 자료

- [Apple: Reducing your app’s size](https://developer.apple.com/documentation/xcode/reducing-your-app-s-size)
- [Apple: Doing basic optimization to reduce your app’s size](https://developer.apple.com/documentation/xcode/doing-basic-optimization-to-reduce-your-app-s-size)
- [Apple: Doing advanced optimization to further reduce your app’s size](https://developer.apple.com/documentation/xcode/doing-advanced-optimization-to-further-reduce-your-app-s-size)
- [Apple: Build settings reference](https://developer.apple.com/documentation/xcode/build-settings-reference)
- [Apple WWDC18: What's New in Swift](https://developer.apple.com/videos/play/wwdc2018/401/)
- [Apple: Managing assets with asset catalogs](https://developer.apple.com/documentation/xcode/managing-assets-with-asset-catalogs)
- [Apple: On-Demand Resources Guide](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/On_Demand_Resources_Guide/index.html)
- [Kotlin: Kotlin/Native binary options](https://kotlinlang.org/docs/native-binary-options.html)
- [Kotlin: Reduce the size of release binaries](https://kotlinlang.org/docs/native-improving-compilation-time.html#reduce-the-size-of-release-binaries)
