# AdMob 광고 갱신 검증

## 목적

같은 광고 소재가 반복 노출되는 현상만으로 새 요청 실패를 판단하지 않는다. AdMob은 새 요청에도 같은 creative를 반환할 수 있으므로 요청, 응답, refresh, impression을 함께 확인한다.

## 현재 갱신 계약

### 홈 배너

- Android와 iOS는 화면 인스턴스가 만들어질 때 최초 광고 요청을 한 번 보낸다.
- 이후 갱신 주기는 AdMob 광고 단위의 `Automatic refresh` 설정을 Google Mobile Ads SDK가 적용한다.
- 배너가 화면에 보이지 않을 때 SDK 자동 갱신이 발생하지 않아야 한다.
- 앱에 별도 반복 타이머를 두지 않는다.
- 자동 갱신 실패 전에 정상 광고가 표시됐다면 기존 광고를 숨기지 않는다.

AdMob 콘솔의 Android/iOS 홈 배너 광고 단위에서 `Automatic refresh`를 `Custom 30 seconds`로 설정한다. 30초는 콘솔이 허용하는 최저값이다. 20초 수동 갱신은 허용 범위 밖이므로 구현하지 않는다. Google은 60초 이상 유지할 때 fill rate에 유리하다고 안내하므로, 적용 전후 Requests, Impressions, Match rate, Show rate와 eCPM을 함께 비교한다.

### Android 종료 다이얼로그 네이티브 광고

- Home 진입 후 광고를 preload한다.
- 백버튼 시점의 광고 snapshot만 열린 다이얼로그에 사용한다.
- 이전 요청 후 60초 전에는 같은 광고 객체를 유지한다.
- 60초 이후 실제 표시된 다이얼로그를 닫으면 기존 객체를 파기하고 다음 generation을 요청한다.
- Home을 벗어난 상태에서는 새 광고를 요청하지 않는다.

종료 다이얼로그는 사용자가 백버튼을 반복해 노출을 만들 수 있으므로 홈 배너의 30초 설정을 공유하지 않는다. 60초 미만의 수동 재요청은 추가하지 않는다.

## Debug/QA 확인

### 홈 배너

Android Logcat에서 `BannerAd`, iOS Console에서 `Banner ad`를 검색한다.

1. Home 진입 직후 `phase=initial` 요청과 응답이 각각 한 번 발생하는지 확인한다.
2. 배너를 계속 보이는 상태에서 AdMob 자동 갱신 주기 이후 `phase=refresh` 응답이 발생하는지 확인한다.
3. `responseId`가 달라졌다면 새 광고 응답이다. creative가 같아 보여도 갱신 실패로 판정하지 않는다.
4. `phase=refresh_failed` 뒤에도 이전 배너가 계속 보이는지 확인한다.
5. 다이얼로그나 다른 화면으로 배너를 숨긴 동안 후속 refresh가 발생하지 않는지 확인한다.

### 종료 다이얼로그

Android Logcat에서 `NativeAd`를 검색한다.

1. `request generation=N`과 `loaded generation=N instance=...`를 한 쌍으로 확인한다.
2. 최초 표시 후 60초 전에 닫았다 다시 열면 `retain` 로그와 같은 instance를 확인한다.
3. 60초 이후 닫으면 `recycle` 뒤 다음 generation 요청이 발생하는지 확인한다.
4. 새 generation에서 같은 creative가 보이더라도 instance와 요청 generation이 다르면 객체 재사용이 아니다.

## 운영 지표 판단

AdMob에서 광고 단위별 Requests, Impressions, Match rate, Show rate, eCPM, Estimated earnings를 같은 기간으로 비교한다. Firebase와 AdMob이 연결돼 있다면 자동 수집되는 `ad_query`, `ad_impression`, `ad_exposure`, `ad_click`도 함께 확인한다.

- Requests가 늘고 Impressions가 늘지 않으면 match/show 구간을 먼저 확인한다.
- Impressions가 늘었지만 수익이 그대로면 eCPM 변동을 확인한다.
- 같은 creative의 반복 노출만으로 갱신 실패나 수익 저하의 원인을 확정하지 않는다.

## 참고

- [AdMob 배너 자동 새로고침](https://support.google.com/admob/answer/3245199)
- [AdMob 구현 가이드](https://support.google.com/admob/answer/2936217)
- [Android Next-Gen AdView](https://developers.google.com/admob/android/next-gen/reference/com/google/android/libraries/ads/mobile/sdk/banner/AdView)
- [iOS 배너 광고](https://developers.google.com/admob/ios/banner)
- [광고 자동 수집 이벤트](https://support.google.com/admob/answer/9755157)
