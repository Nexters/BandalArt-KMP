# AdMob 보상형 반다라트 생성 게이트 전략

## 배경

AdMob 슬롯 기반 작업(PR #241)은 무료 슬롯 3개와 영구 확장 슬롯 저장소, Android GMA SDK 초기화까지만 제공한다. 실제 광고 load/show와 Home 생성 흐름은 후속 작업으로 남겨 두었기 때문에 현재 앱에서는 광고 동작을 검증할 수 없다.

## 목표

- 사용자가 현재 슬롯을 모두 사용한 상태에서 반다라트 추가를 누르면 광고보다 먼저 안내 팝업을 표시한다.
- 팝업은 광고를 시청하면 반다라트를 1개 더 추가할 수 있음을 명확히 알린다.
- 확인 시 Android 보상형 광고를 load/show하고, 보상 획득 뒤 슬롯을 1개 확장해 반다라트를 정확히 1개 생성한다.
- 광고 load/show 실패는 안내 후 fail-open으로 처리해 슬롯을 확장하고 반다라트를 생성한다.
- 슬롯 조회 또는 확장 영속화 실패는 게이트를 우회하지 않고 재시도 안내 후 생성을 중단한다.
- 사용자가 팝업 또는 광고를 닫으면 슬롯을 확장하거나 반다라트를 생성하지 않는다.
- iOS는 이번 범위에서 광고 SDK를 추가하지 않고 fail-open으로 동일한 생성 흐름을 유지한다.

## 설계

### 공통 Home 상태

- `BandalartSlotRepository`가 현재 목록 크기에 대응하는 최대 슬롯을 제공한다.
- 무료/확장 슬롯이 남아 있으면 기존 생성 흐름을 바로 실행한다.
- 슬롯을 모두 사용했으면 `RewardedCreate` 다이얼로그 상태를 노출한다.
- 확인 이벤트는 고유 요청 ID를 retained 화면 상태에 기록해 광고 실행을 요청한다.
- 완료 이벤트는 요청 ID를 대조해 중복·지연 콜백을 무시한다.

### 플랫폼 광고 경계

- core commonMain에는 보상형 광고 결과(`Rewarded`, `Dismissed`, `Failed`)와 gateway 계약만 둔다.
- androidApp은 앱 리소스의 환경별 광고 단위 ID와 GMA Next-Gen 1.3.0을 사용해 `RewardedAd`를 load/show한다.
- SDK background callback은 main thread로 직렬화하고 load 중 화면 재생성 시 callback 시점의 resumed `Activity`에 광고를 연결한다.
- iOS graph는 no-op gateway의 `Failed` 결과로 fail-open 정책을 적용한다.
- 광고 SDK 타입과 `Activity`는 androidApp 밖으로 노출하지 않는다.

### 정확히 한 번 생성

- 보상 또는 실패 결과를 처음 받은 요청만 슬롯 확장과 생성을 수행한다.
- 광고 표시 전 요청 ID와 목표 슬롯을 DataStore에 기록한다.
- reward callback은 같은 요청을 `granted`로 바꾸면서 목표 최대 슬롯을 한 번의 DataStore edit로 영속화한다.
- Android gateway는 reward callback을 기록하고 광고 dismiss 뒤 최종 `Rewarded` 결과를 반환한다. mediation의 dismiss-before-reward 순서도 짧은 callback grace 구간에서 합쳐 처리한다.
- 요청별 완료 결과는 Home이 소비할 때까지 gateway에 유지해 화면 재생성 시 같은 광고를 다시 load하지 않는다.
- 요청 ID는 Presenter 수명과 무관한 임의 64-bit 값으로 만들어 이전 화면의 미소비 결과와 충돌하지 않게 한다.
- reward 확정 뒤 생성은 화면 재생성 취소에 끊기지 않는다. 재실행 시 granted 요청의 목표 슬롯과 현재 Room 개수를 비교해 누락된 생성만 재개하고, 이미 목표 개수에 도달했으면 요청을 정리한다.
- 생성 처리 중에는 Room Flow가 목표 개수를 관찰할 때까지 coordinator를 잠가 stale count를 이용한 중복 추가를 막는다.
- 광고를 보지 않고 닫은 결과는 생성하지 않는다.

## UI 문구

- 제목: 새 반다라트를 더 만들까요?
- 본문: 광고를 끝까지 보면 새 반다라트 슬롯 1개를 계속 사용할 수 있어요.
- 확인: 광고 보고 추가하기
- 취소: 취소

한국어, 영어, 일본어 리소스를 함께 제공한다.

## 검증

- 무료 슬롯 이내 즉시 생성
- 슬롯 소진 시 안내 팝업 선노출
- 팝업 취소 시 미생성
- 확인 시 retained 광고 요청 1회 발생
- 보상/실패 시 슬롯 1개 확장 및 정확히 1개 생성
- 광고 dismiss 및 중복 콜백 시 미생성
- reward/dismiss 콜백 순서 역전과 화면 재생성 중 grant 완료
- granted 요청을 가진 process 재실행 시 누락 생성 복구 및 이미 생성된 요청 중복 방지
- Room Flow 반영이 지연된 동안 연속 Add 차단
- Android host unit test, Android lint, 관련 컴파일을 통과한다.
- 이 기능을 처음 배포했을 때는 Play Internal Testing에 `-Pbandalart.useTestAds=true`를 전달해 공식 Google 테스트 Rewarded ID를 사용했다. [#354](https://github.com/Nexters/BandalArt-KMP/issues/354) 이후에는 debug만 테스트 ID를 사용하고 Internal release는 운영 광고 ID를 사용한다.

## Android Internal Testing 배포

- 앱 버전은 `2.2.17 (20217)`로 올린다.
- 한국어, 영어, 일본어 Internal release notes를 보상형 생성 흐름에 맞게 갱신한다.
- 현재 clean AAB와 upload 명령에는 광고 ID override를 전달하지 않는다. release 운영 ID가 포함되고 테스트 ID가 없는 AAB만 업로드한다.

## 비범위

- 배너 광고
- iOS 광고 SDK 연동
- 서버 측 보상 검증
- 광고 수익/퍼널 분석 이벤트
