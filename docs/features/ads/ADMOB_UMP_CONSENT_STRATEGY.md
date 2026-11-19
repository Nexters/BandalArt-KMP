# AdMob UMP 동의 흐름 전략

관련 이슈: [#515](https://github.com/Nexters/BandalArt-KMP/issues/515)

## 배경

- 이슈 #206은 "국내 배포 단계에서는 UMP 연동을 보류하고, EEA·영국·스위스 등 동의 대상 지역으로 확대하기 전에 연동한다"고 정했다.
- 현재 Android 프로덕션은 178개 국가/지역에 배포 중이고, 앱은 한국어·영어·일본어를 지원한다. 위 결정의 전제가 바뀌었다.
- AdMob 국가별 보고서(2026-07-01~2026-11-19)에서 오스트리아·체코·프랑스·영국은 요청이 있는데도 eCPM이 `—`(노출 0)다. Google 인증 CMP 없이 EEA·영국 광고가 제한되는 증상과 일치한다.
- 두 플랫폼 모두 맞춤 광고를 코드에서 강제로 끈다.
  - Android: PR #278에서 모든 광고 요청에 `npa=1`을 붙인다(`androidApp/.../ads/AdRequestPrivacy.kt`).
  - iOS: `IosAdsBridgeImpl.start()`에서 `publisherPrivacyPersonalizationState = .disabled`를 설정한다.
  - 기록된 근거는 "UMP 보류"뿐이다. UMP를 도입하면 동의 대상 지역에서는 사용자의 동의가 맞춤 여부를 정하므로, 강제 비맞춤은 동의한 사용자에게도 맞춤 광고를 막는 설정이 된다.

## 목표

1. Android와 iOS에 Google UMP SDK를 연동해 동의 대상 지역 사용자에게 인증 CMP 동의 흐름을 제공한다.
2. 광고 SDK 초기화와 모든 광고 요청(보상형 사전 로드 포함)은 `canRequestAds()`가 true일 때만 시작한다.
3. 동의 대상 사용자에게 설정 화면의 "개인정보 옵션" 진입점을 제공해 동의를 다시 바꿀 수 있게 한다.
4. 개인정보처리방침을 갱신한 뒤 두 플랫폼의 맞춤 광고 강제 해제(`npa=1`, `.disabled`)를 제거한다.

## 범위 밖

| 항목 | 이유 |
| --- | --- |
| iOS ATT(IDFA 설명 메시지) | 처음에는 범위 밖으로 두었으나, iOS 비맞춤 강제를 제거하면 맞춤 광고가 Apple 기준의 추적에 해당할 수 있어 #519에서 도입했다. |
| `putPublisherFirstPartyIdEnabled(false)`, `setPublisherFirstPartyIDEnabled(false)` | 동의와 별개인 게시자 식별자 설정이다. 이번에는 유지한다. |
| Firebase `google_analytics_adid_collection_enabled`, `google_analytics_default_allow_ad_personalization_signals` | Analytics 설정이며 AdMob 광고 수익과 직접 관련이 없다. 유지한다. |
| Android 보상형 광고 사전 로드 방식 변경 | 요청 낭비 문제로 별도 작업이다. |

## 현재 구조

| 플랫폼 | 초기화 시점 | 광고 요청 경로 |
| --- | --- | --- |
| Android | `BandalartApplication.onCreate()`에서 `adsInitializer.initialize()` 호출 | 배너·종료 다이얼로그·보상형이 `awaitInitialized()`를 기다린 뒤 요청한다. 초기화 직후 보상형 2종을 `RewardedAdPreloader`로 사전 로드한다. |
| iOS | `iosApp.swift`에서 `adsBridge.start()` 호출 | `MobileAds.shared.start` 완료 후 배너 로드, 보상형 2종 로드 |

UMP 동의 양식은 Activity 또는 UIViewController가 있어야 띄울 수 있다. Android는 지금처럼 Application에서 초기화를 시작할 수 없으므로 시작 지점이 바뀐다.

## 설계

### 동의 흐름 (두 플랫폼 공통)

1. 첫 화면(Android `MainActivity`, iOS 루트 화면)이 뜨면 `requestConsentInfoUpdate`를 호출한다.
2. 이전 실행에서 이미 동의를 받았다면 `canRequestAds()`가 바로 true다. 이 경우 갱신 응답을 기다리지 않고 광고 초기화를 시작한다. Google 권장 흐름이며 재실행 시 광고 지연을 없앤다.
3. 갱신이 끝나면 `loadAndShowConsentFormIfRequired`를 호출한다. 동의 대상 지역이 아니면 양식이 뜨지 않는다.
4. 양식이 닫히거나 갱신이 실패해도 마지막에 `canRequestAds()`를 다시 확인한다. true면 광고 초기화를 시작한다.
5. 광고 초기화는 프로세스당 한 번만 실행한다. 기존 `AdsInitializer`의 `compareAndSet` 보호를 유지한다.

### 광고를 요청할 수 없을 때

- `canRequestAds()`가 끝내 false면 광고 초기화를 시작하지 않는다.
- 현재 `awaitInitialized()`는 초기화가 시작되지 않으면 영원히 기다린다. 동의 흐름이 끝났는데 광고를 요청할 수 없으면 `initialization.complete(false)`로 끝내야 한다.
- 보상형은 이미 `awaitAdsInitialized()`가 false면 `RewardedAdResult.FAILED`를 반환한다. 기존 fail-open 정책(#206)에 따라 광고 없이 슬롯이 확장되므로 핵심 흐름은 막히지 않는다.
- 배너와 종료 다이얼로그는 초기화 실패 시 공간을 예약하지 않는지 구현 단계에서 확인한다.

### 개인정보 옵션 진입점

- `core/common`에 SDK 타입을 노출하지 않는 작은 인터페이스를 둔다. 기존 `BannerAdHost`·`ExitDialogHost`·`RewardedAdGateway`와 같은 경계 방식이다.
  - 상태: 개인정보 옵션이 필요한지 여부. UMP `privacyOptionsRequirementStatus == REQUIRED`일 때만 true
  - 동작: 개인정보 옵션 양식 표시
- `SettingsBottomSheet`의 문의하기 행 근처에 "개인정보 옵션" 행을 추가하고, 상태가 true일 때만 노출한다. 한국 등 비대상 지역 사용자에게는 보이지 않는다.
- 문자열은 기본(한국어), `values-en`, `values-ja` 세 언어에 추가한다.

### 플랫폼 구현

| 플랫폼 | 의존성 | 구현 위치 |
| --- | --- | --- |
| Android | `com.google.android.ump:user-messaging-platform:4.0.0`. GMA Next-Gen 개인정보 가이드가 같은 UMP SDK를 사용한다. UMP는 manifest의 `com.google.android.gms.ads.APPLICATION_ID`에서 App ID를 읽으므로 이 meta-data를 추가한다. | `AdConsentManager`가 동의 흐름과 개인정보 옵션을 담당한다. `MainActivity.onCreate`에서 프로세스당 한 번 시작하고, `BandalartApplication`의 `adsInitializer.initialize()` 호출과 `awaitInitialized()`의 자동 초기화를 제거한다. |
| iOS | `GoogleMobileAds` SPM 패키지가 의존하는 `GoogleUserMessagingPlatform`(3.1.0 resolved)을 그대로 import한다. | `IosAdsBridgeImpl.start()`가 동의 흐름을 담당한다. 동의 양식은 key window가 필요하므로 `didFinishLaunching`이 아니라 SwiftUI scene이 `.active`가 될 때 시작한다. |

### 맞춤 광고 강제 해제 제거

- Android: `AdRequestPrivacy.kt`와 `setGoogleExtrasBundle(nonPersonalizedAdExtras())` 호출 4곳을 제거한다.
- iOS: `publisherPrivacyPersonalizationState = .disabled` 설정을 제거한다.
- **선행 조건**: 공개된 개인정보처리방침이 "Android 및 iOS 앱에서 비맞춤형 광고 제공"이라고 적고 있다(`docs/legal/PRIVACY_POLICY_ANDROID_AD_UPDATE.md` 제1조 4항). 방침을 먼저 갱신해 공개한 뒤 이 변경을 출시한다.

## 사용자 작업 (콘솔·정책)

코드로 처리할 수 없어 사용자가 직접 해야 하는 작업이다.

1. AdMob → 개인 정보 보호 및 메시지 → **유럽 규정(GDPR) 메시지**를 Android·iOS 앱 각각에 만들고 게시한다. IDFA 메시지는 만들지 않는다(범위 밖).
2. 개인정보처리방침의 "비맞춤형 광고" 문구를 "동의에 따른 맞춤형 또는 비맞춤형 광고"로 갱신하고 공개한다.
3. Play Console Data safety와 App Store 개인정보 라벨이 바뀐 광고 처리와 맞는지 점검한다.

## 단계와 PR 분리

처음에는 플랫폼별 4단계로 나눌 계획이었으나, 2026-11-19 Android·iOS 업데이트에 모두 담기로 결정했다. 하나의 PR에서 다음을 함께 처리한다.

| 항목 | 출시 조건 |
| --- | --- |
| Android·iOS UMP 연동, 초기화 게이트, 개인정보 옵션 진입점 | AdMob GDPR 메시지 게시 |
| Android `npa=1`, iOS `.disabled` 제거 | 개인정보처리방침 갱신본을 앱 업데이트보다 먼저 공개 |

방침 제12조는 중대한 변경에 합리적인 사전 통지 기간을 두도록 한다. 방침을 먼저 공개하고 스토어 심사를 거쳐 앱이 반영되는 순서로 통지 기간을 둔다.

## 검증

| 항목 | 방법 | 기대 결과 |
| --- | --- | --- |
| EEA 동의 양식 | `ConsentDebugSettings`에 테스트 기기 해시 ID와 debug geography EEA 설정 | 첫 실행에 동의 양식이 뜨고, 동의 후 테스트 광고가 노출된다 |
| 동의 거부 | 같은 설정에서 "동의 안 함" 선택 | 앱이 막히지 않는다. 보상형 생성은 fail-open으로 진행된다 |
| 재실행 | 동의 후 앱 재시작 | 양식 없이 바로 광고가 로드된다 |
| 비대상 지역 | debug geography 미설정(한국) | 양식이 뜨지 않고 기존과 같게 광고가 노출된다. 설정에 개인정보 옵션 행이 없다 |
| 개인정보 옵션 | EEA 설정에서 설정 → 개인정보 옵션 | 양식이 다시 뜨고 선택을 바꿀 수 있다 |
| 동의 초기화 | 앱 데이터 삭제(Android) 또는 앱 삭제 후 재설치(iOS) | 다음 실행에서 양식이 다시 뜬다 |
| 출시 후 | AdMob 국가별 보고서 | 영국·프랑스 등 EEA 국가의 노출수와 eCPM이 `—`에서 값으로 바뀐다 |

debug geography는 debug 빌드에서만 켤 수 있다.

- Android: `./gradlew :androidApp:installDebug -PumpDebugEea=true`. 에뮬레이터는 UMP 테스트 기기로 자동 처리된다.
- iOS: Debug scheme 환경 변수 `UMP_DEBUG_EEA=1`. 시뮬레이터는 UMP 테스트 기기로 자동 처리된다.
