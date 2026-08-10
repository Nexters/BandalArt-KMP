# AdMob 홈 하단 배너 전략

## 배경

이슈 #206의 보상형 슬롯 확장 흐름은 PR #243에서 구현됐고, 후속 작업에서 홈 하단 Large Anchored Adaptive Banner를 연결했다. 실제 화면에서는 배너 높이가 홈 콘텐츠를 과도하게 밀어 올리는 문제가 확인돼, 광고 영역을 더 작게 유지하는 후속 조정이 필요하다.

## 목표

- Android 홈 하단에 GMA Next-Gen 1.3.0의 고정형 `AdSize.BANNER`(320x50dp)를 노출한다.
- 가용 폭이 320dp 이상일 때만 50dp 높이를 요청 전에 확보해 load 완료 전후로 홈 콘텐츠가 이동하지 않게 한다.
- 가용 폭이 320dp 미만이면 광고를 요청·렌더링하거나 빈 공간을 예약하지 않는다.
- 홈의 공유 버튼과 Snackbar는 배너 위에 배치하고 시스템 navigation bar safe area를 유지한다.
- bottom sheet, dialog, 이미지 capture, 보상형 광고 표시 및 loading 중에는 배너 creative와 클릭·접근성 focus를 숨긴다.
- iOS는 광고 SDK를 추가하지 않고 no-op host를 유지한다.
- Internal Testing AAB에서는 Rewarded와 Banner 모두 Google 공식 테스트 광고 단위 ID만 사용한다.

## 플랫폼 경계

- `core/common`에 SDK 타입을 노출하지 않는 `BannerAdHost`의 Compose 렌더링 계약을 둔다.
- Android 구현은 `androidApp`에서 앱의 환경별 광고 단위 ID, `AdsInitializer`, GMA `BannerAd`를 소유한다.
- Metro `PlatformBindings`를 통해 Home UI에 app-scoped host를 주입한다.
- iOS graph와 Preview/Test는 `NoOpBannerAdHost`를 사용한다.
- Home Presenter에는 광고 객체, load 상태 또는 Activity를 저장하지 않는다. 배너는 화면 수명에 맞춰 생성·폐기하는 platform UI element다.

## Android lifecycle과 크기

- 공식 고정형 `AdSize.BANNER`를 사용해 creative를 320x50dp로 제한한다. Large Anchored Adaptive보다 화면 점유를 줄이는 대신, 320dp보다 좁은 공간에서는 광고를 표시할 수 없다.
- 320dp 이상인 full-width host의 중앙에 320x50dp `AdView`를 배치하고 50dp 높이를 광고 요청 전에 예약한다.
- 가용 폭이 320dp 미만이면 `AdView`와 예약 공간을 만들지 않고 광고 요청도 보내지 않는다.
- GMA 초기화 완료 뒤 `AdView.loadAd()`를 한 번 호출하고 SDK가 creative 표시와 refresh를 관리하게 한다.
- 같은 Activity composition에서는 지원 범위 안의 폭 변경만으로 다시 요청하지 않는다. Activity가 바뀌거나 폭이 320dp 경계를 벗어나 view가 composition에서 제거되면 기존 `AdView`를 `destroy()`한다.
- 화면이 composition에서 제거되면 `AndroidView.onRelease`에서 `AdView`를 `destroy()`한다.
- load 실패는 홈 기능을 막거나 빈 공간의 클릭 영역을 만들지 않고 로그만 남긴다.

## Home 배치와 노출 규칙

- Home의 고정 영역을 `콘텐츠·Snackbar`와 `배너`의 세로 구조로 나눈다.
- 스크롤 콘텐츠와 공유 버튼은 배너가 차지한 실제 높이만큼 자동으로 위에 배치한다.
- Snackbar는 콘텐츠 영역의 하단에 두어 배너를 가리지 않는다.
- 앱 root Scaffold가 전달한 system bar inset 안에 배너를 두어 navigation bar와 겹치지 않게 한다.
- 가용 폭이 지원되는 동안 host와 예약 공간은 Home 수명 동안 유지하되 다음 조건이 모두 참일 때만 native 광고 view를 visible로 둔다. overlay가 열릴 때마다 광고를 다시 요청하지 않으면서 클릭과 접근성 focus를 차단한다.
  - Home loading이 끝남
  - bottom sheet가 닫힘
  - dialog가 닫힘
  - 이미지 capture 요청이 없음
  - 보상형 광고 요청이 없음

## Internal Testing

- Android 버전을 `2.2.18 (20218)`로 올린다. Play 전체 track의 현재 최대 versionCode는 `20217`이다.
- `-Pbandalart.useTestAds=true`가 release Rewarded와 Banner ID를 각각 Google 공식 테스트 ID로 바꿔야 한다.
- clean bundle과 publish task 모두 같은 property를 전달한다.
- AAB 검증 시 Rewarded 테스트 ID `ca-app-pub-3940256099942544/5224354917`와 Fixed Size Banner 테스트 ID `ca-app-pub-3940256099942544/6300978111`의 존재를 확인하고 두 production ad unit ID가 있으면 업로드하지 않는다.
- 이 테스트 광고 artifact는 production으로 promote하지 않는다. production 광고 ID를 쓰는 다음 배포는 새 versionCode로 다시 빌드한다.

## 검증

### 자동 검증

- Home banner visibility 규칙을 각 overlay/capture/loading/rewarded 상태별로 검증한다.
- Android graph가 실제 host를, iOS graph와 Preview/Test가 no-op host를 제공하는지 컴파일과 graph test로 확인한다.
- 관련 Android host tests, Detekt, Android lint/build와 iOS framework CI를 통과한다.
- Internal AAB의 package/version, 서명, Compose resource namespace와 두 테스트 광고 ID를 검증한다.

### Internal 수동 검증

- 홈 하단에 `Test Ad` 표시가 있는 배너 creative가 실제로 노출된다.
- 320dp 이상 화면에서 고정형 배너가 중앙에 표시되고 navigation bar, 공유 버튼과 겹치지 않는다. 320dp 미만 가용 폭에서는 광고 요청과 빈 공간이 모두 없어야 한다. 현재 앱은 portrait 고정이므로 orientation lock 해제는 이번 범위에 포함하지 않는다.
- Snackbar가 배너 위에 표시된다.
- bottom sheet, dialog, 공유·저장 capture와 보상형 광고 표시 중 배너가 숨겨진다.
- light/dark theme에서 광고 주변 container가 홈 배경과 어색하게 분리되지 않는다.
- 보상형 광고는 안내 dialog 뒤에 Google 테스트 creative가 표시되고, reward 완료 시 정확히 1개 생성되며 reward 전 종료 시 생성하지 않는다.
- 광고 unavailable snackbar 뒤 fail-open 생성만 발생했다면 AdMob 활성화 성공으로 판단하지 않는다.

## 비범위

- iOS AdMob SDK와 iOS 배너
- UMP/CMP 및 해외 동의 대상 지역 확대
- 광고 수익·퍼널 analytics
- 일반 Interstitial
- 템플릿 생성 #208의 광고 gate

## 공식 참고

- [GMA Next-Gen fixed size banner](https://developers.google.com/admob/android/next-gen/banner/fixed-size)
- [GMA Next-Gen test ads](https://developers.google.com/admob/android/next-gen/test-ads)
