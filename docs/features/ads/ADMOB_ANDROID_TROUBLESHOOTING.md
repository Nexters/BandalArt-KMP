# Android AdMob 미노출 트러블슈팅

## 문서 목적

Play Internal Testing에서 홈 배너와 보상형 광고가 모두 나타나지 않았던 사건의 진단 과정과 재발 방지 절차를 기록한다. 같은 증상이 생기면 광고 크기나 광고 단위 ID부터 바꾸지 말고, 설치 산출물과 SDK 초기화부터 요청·표시까지의 생명주기를 아래 순서로 확인한다.

관련 이슈는 [#289](https://github.com/Nexters/BandalArt-KMP/issues/289), 최종 원인 수정은 [#297](https://github.com/Nexters/BandalArt-KMP/pull/297)이다. `2.2.26 (20226)`은 Google 테스트 광고 ID로 Internal Testing에 배포했으며, 이슈는 실기기에서 테스트 creative를 확인한 뒤 닫는다.

## 사건 요약

- Play에서 설치한 `2.2.23` 이후 빌드의 홈 하단에 약 50dp 공간만 남고 `Test Ad` creative가 표시되지 않았다.
- 무료 슬롯을 모두 쓴 뒤 반다라트를 추가하면 보상형 광고도 나타나지 않았다.
- 대신 `광고를 불러오지 못해 광고 없이 계속할게요.` 스낵바가 표시되고 반다라트는 정상 생성됐다.
- Internal Testing이 광고를 숨기는 것처럼 보였지만, Play Internal Testing 자체는 광고 요청이나 표시를 막지 않는다.
- 배너 높이와 테스트 광고 단위 ID를 먼저 의심했으나, 두 형식에 공통인 SDK 초기화가 더 위쪽에서 실패하고 있었다.

## 사용자 동작과 광고 성공을 구분한다

보상형 광고 실패 시에도 사용자가 기능을 계속 쓸 수 있도록 fail-open 정책을 적용한다. 따라서 스낵바 뒤 반다라트가 생성되는 것은 생성 정책이 정상이라는 뜻이지, AdMob이 정상이라는 뜻은 아니다.

| 광고 결과 | 사용자 결과 | 판정 |
| --- | --- | --- |
| `REWARDED` | 슬롯 확장 후 정확히 1개 생성 | 광고와 생성 흐름 모두 정상 |
| `FAILED` | unavailable 스낵바 후 슬롯 확장·생성 | fail-open 정상, 광고는 실패 |
| `DISMISSED` | 슬롯 확장·생성하지 않음 | 사용자가 광고를 닫은 정상 흐름 |
| 같은 요청의 늦거나 중복된 callback | 추가 생성 없음 | 중복 소비 방지 정상 |

템플릿으로 추가한 경우에도 선택한 템플릿 ID를 광고 결과까지 보존한다. `FAILED`라면 같은 템플릿으로 fail-open 생성하고, `DISMISSED`라면 생성하지 않는다.

## 최종 원인

GMA Next-Gen SDK 1.3.0에서 다음 순서로 초기화하고 있었다.

```kotlin
MobileAds.putPublisherFirstPartyIdEnabled(false)
MobileAds.initialize(context, initializationConfig) { /* ... */ }
```

`putPublisherFirstPartyIdEnabled(false)`는 초기화 뒤에만 호출할 수 있다. 초기화 전에 호출하면 `IllegalStateException`이 발생한다. 이 예외가 바깥 `runCatching`에 잡혀 초기화 결과가 `false`가 됐고, 배너와 보상형 양쪽 모두 광고 요청을 시작하지 못했다.

당시 release 빌드에는 Napier의 release `Antilog`가 없어 실패 로그도 보이지 않았다. UI에는 배너용 50dp host만 남았고 네트워크 요청, 광고 View와 WebView는 생성되지 않았다. 이 조합 때문에 문제를 배너 크기나 Internal Testing 환경 문제로 오인하기 쉬웠다.

[GMA Next-Gen `MobileAds` 문서](https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/MobileAds)에 따르면 `initialize()`는 동기식이며, 반환 시점부터 광고 요청을 보낼 수 있다. adapter 상태 callback을 Google 광고 요청의 별도 gate로 기다릴 필요가 없다.

수정한 순서는 다음과 같다.

```kotlin
MobileAds.initialize(context, initializationConfig)
MobileAds.putPublisherFirstPartyIdEnabled(false)
RewardedAdPreloader.start(adUnitId, preloadConfiguration)
initialization.complete(true)
```

초기화, 보상형 preloader 시작, 배너 load 실패에는 release에서도 보이는 최소 Android 로그를 남긴다. 성공 로그나 광고 ID, 사용자 정보는 남기지 않는다.

## 재현과 진단 순서

### 1. 설치 출처와 버전을 고정한다

로컬 debug가 아니라 사용자가 보고한 바로 그 Play 설치본인지 먼저 확인한다.

```bash
adb devices -l
adb shell dumpsys package com.nexters.bandalart
```

다음을 기록한다.

- `versionName`, `versionCode`
- installer가 Play Store인지
- 업데이트 뒤 기존 프로세스가 남지 않도록 cold start했는지

### 2. 실제 설치 APK와 배포 AAB의 설정을 확인한다

소스의 값이 아니라 설치된 산출물에 원하는 App ID와 광고 단위 ID가 들어갔는지 확인한다.

```bash
adb shell pm path com.nexters.bandalart
adb pull /data/app/.../base.apk /tmp/bandalart-base.apk
unzip -p /tmp/bandalart-base.apk resources.arsc | strings | grep 'ca-app-pub-'
```

Internal AAB에는 Google 공식 테스트 ID가 들어가야 한다.

- Rewarded: `ca-app-pub-3940256099942544/5224354917`
- Fixed Size Banner: `ca-app-pub-3940256099942544/6300978111`

운영 광고 단위 ID가 하나라도 들어 있으면 Internal 검증용 산출물로 업로드하지 않는다. 테스트 광고 AAB도 Production으로 승급하지 않고, 운영 ID와 새 versionCode로 다시 빌드한다.

### 3. 한 번의 cold start를 관찰한다

```bash
adb logcat -c
adb shell am force-stop com.nexters.bandalart
adb shell monkey -p com.nexters.bandalart 1
adb shell uiautomator dump /sdcard/bandalart-ui.xml
adb logcat -d -s AdsInitializer:E BannerAd:W '*:S'
```

가능하면 화면 녹화나 스크린샷도 함께 남긴다. 중요한 것은 `안 보인다`만 기록하는 것이 아니라 다음을 분리하는 것이다.

- 50dp host 자체가 없는가
- host만 있고 광고 View/WebView가 없는가
- 광고 요청이 발생했는가
- load callback이 성공 또는 실패했는가
- load 성공 뒤 View가 visible하고 등록됐는가

### 4. 가장 위에서 끊긴 경계를 찾는다

| 관찰 결과 | 먼저 볼 경계 |
| --- | --- |
| 배너 공간 자체가 없음 | Home 노출 조건, 가용 폭 320dp, layout |
| 빈 50dp 공간, 광고 요청도 없음 | SDK 초기화, 초기화 await/gate |
| 요청 있음, `LoadAdError` 있음 | 광고 단위 ID, 네트워크, no-fill, 요청 설정 |
| load 성공, 화면에는 없음 | native View 등록, visibility, overlay 상태 |
| 보상형 `FAILED` 후 생성됨 | fail-open은 정상, load/show 실패 원인은 별도 조사 |
| 보상형 `DISMISSED` 후 생성 안 됨 | 의도한 동작 |

빈 host만 보이면 크기를 다시 조정하기 전에 `AdsInitializer`가 성공했는지부터 확인한다. 요청 자체가 없다면 creative 형식이나 no-fill을 조사할 단계가 아니다.

### 5. AAB를 업로드 전에 검증한다

프로젝트의 검증 스크립트로 package, 버전, 서명, 리소스 namespace와 테스트/운영 광고 ID를 검사한다.

```bash
/usr/bin/python3 scripts/validate_play_aab.py \
  --aab androidApp/build/outputs/bundle/release/androidApp-release.aab \
  --bundletool /path/to/bundletool-all.jar \
  --version-name 2.2.26 \
  --version-code 20226
```

업로드 뒤에는 Play API로 track과 상태를 다시 확인한다.

```bash
uv run scripts/play_next_version_code.py \
  --expect-version-code 20226 \
  --verify-track internal \
  --verify-status completed \
  --expect-update-priority 0 \
  --retries 6
```

## 자동 테스트가 보장하는 계약

광고 SDK의 실기기 표시 여부는 단위 테스트로 증명할 수 없다. 대신 광고 결과 이후의 제품 동작은 다음 테스트로 고정한다.

- `HomePresenterRewardedAdTest.loadOrShowFailureFailsOpenButDismissDoesNotCreate`
  - load/show 실패는 스낵바 뒤 생성한다.
  - 사용자가 닫은 경우 생성하지 않는다.
- `HomePresenterRewardedAdTest.rewardExpandsSlotAndCreatesExactlyOnce`
  - reward 완료는 슬롯과 반다라트를 정확히 한 번만 추가한다.
- `HomePresenterRewardedAdTest.rewardedCreationPersistsSelectedTemplateUntilGrant`
  - 보상 결과까지 선택한 템플릿을 유지한다.
- `RewardedAdCallbackCoordinatorTest`
  - reward/dismiss callback 순서가 뒤집혀도 최종 결과를 한 번만 전달한다.
- Home banner visibility 테스트
  - loading, overlay, capture, 보상형 요청 중 배너 노출 규칙과 320dp 폭 경계를 검증한다.

이번 수정에서 보상형 gateway의 load/show 실패마다 release 직접 로그를 추가하지는 않았다. 공통 초기화와 preloader 시작 실패는 `AdsInitializer`, 배너 load 실패는 `BannerAd`로 확인한다. 공통 초기화가 성공한 뒤 보상형만 다시 실패한다면 그때 gateway의 load/show 경계에 개인정보 없는 실패 로그를 추가한다.

## 잘못된 방향과 배운 점

### 배너 높이

큰 adaptive 배너를 320x50dp 고정형으로 줄이는 것은 UI 개선으로는 맞았지만, 광고 요청이 0건인 원인을 해결하지는 못한다. 빈 공간의 크기와 SDK 요청 생명주기는 별개다.

### 테스트 광고 단위 ID

Anchored Adaptive 테스트 ID를 Fixed Size Banner 테스트 ID로 맞춘 [#290](https://github.com/Nexters/BandalArt-KMP/pull/290)도 필요한 수정이었다. 그러나 설치 APK에 올바른 ID가 들어간 뒤에도 네트워크 요청이 없다면 ID보다 위에 있는 초기화를 봐야 한다.

### Internal Testing 환경

여러 Internal 빌드에서 계속 광고가 안 보인다고 해서 환경의 제한으로 결론 내리면 안 된다. 같은 산출물의 설치 버전, 광고 ID, 초기화 로그와 요청 발생 여부를 확인해야 한다.

### release 로깅

`runCatching`으로 사용자 크래시는 막았지만, release에서 실패 원인까지 사라졌다. 외부 SDK의 필수 초기화처럼 전체 기능을 차단하는 경계에는 성공 로그가 아니라 최소 실패 로그가 필요하다.

### SDK 문서와 실제 버전

비슷한 구형 GMA API 예제의 callback 모델을 현재 Next-Gen API에 대입하지 않는다. 프로젝트가 고정한 정확한 SDK 버전의 공식 문서와, 필요하면 해당 binary 동작을 함께 확인한다.

## 재발 방지 체크리스트

- [ ] Play 설치본의 versionName/versionCode와 installer를 확인했다.
- [ ] 설치 APK 또는 업로드 AAB 안의 App ID와 두 광고 단위 ID를 확인했다.
- [ ] SDK 초기화 성공 전 다른 `MobileAds` API를 호출하지 않는다.
- [ ] 초기화 → 요청 → load → View 등록 순서에서 처음 끊긴 지점을 찾았다.
- [ ] fail-open 생성과 광고 성공을 구분했다.
- [ ] release에서 초기화·preloader·배너 load 실패를 확인할 수 있다.
- [ ] Presenter의 fail-open, dismiss, exactly-once, 템플릿 보존 테스트가 통과한다.
- [ ] Internal AAB에는 공식 테스트 ID만 포함한다.
- [ ] 테스트 광고가 포함된 AAB을 Production으로 승급하지 않는다.
- [ ] 실기기에서 배너 `Test Ad`와 보상형 테스트 creative를 각각 확인했다.

## 블로그 작성용 구성

### 제목 후보

- 빈 배너 공간만 남은 AdMob 버그: 광고 ID보다 먼저 봐야 했던 초기화 순서
- Play Internal Testing에서 광고가 안 보일 때 확인할 것들
- `runCatching` 뒤에 숨은 AdMob 초기화 실패를 찾기까지

### 글의 흐름

1. **도입:** Internal 빌드를 여러 번 올렸는데 50dp 빈 공간과 fail-open 스낵바만 보였다.
2. **혼동:** 배너 높이와 테스트 ID는 실제 문제였지만 최종 원인은 아니었다.
3. **관찰 전환:** UI 대신 설치 APK, logcat, 광고 네트워크 요청 유무를 고정된 한 번의 cold start에서 확인했다.
4. **결정적 단서:** 배너와 보상형이 함께 실패했고 요청이 0건이었다.
5. **원인 확인:** Next-Gen SDK binary와 공식 문서로 초기화 전 API 호출의 예외와 동기식 초기화 계약을 확인했다.
6. **수정:** `initialize`를 먼저 호출하고 release-safe 실패 로그를 최소한으로 추가했다.
7. **제품 정책:** 광고 장애 시 fail-open과 사용자의 자발적 dismiss를 다르게 취급한 이유를 설명한다.
8. **운영 교훈:** 정확한 산출물 검증, 상태 판별표, 테스트 광고 AAB의 Production 승급 금지를 체크리스트로 정리한다.

## 공식 참고

- [GMA Next-Gen MobileAds API](https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/MobileAds)
- [GMA Next-Gen banner](https://developers.google.com/admob/android/next-gen/banner)
- [GMA Next-Gen test ads](https://developers.google.com/admob/android/next-gen/test-ads)
