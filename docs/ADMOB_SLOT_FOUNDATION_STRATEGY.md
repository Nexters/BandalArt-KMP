# AdMob 슬롯 기반 작업 전략

## 목표

반다라트 생성 정책을 기본 무료 슬롯 3개와 보상형 광고 1회당 영구 슬롯 1개 확장 구조로 전환할 수 있는 공통 기반을 만든다. 이번 단계는 KMP 공통 슬롯 정책과 저장소, Android GMA Next-Gen SDK 초기화까지만 포함한다.

## 성공 기준

- 기본 최대 슬롯은 3개다.
- 저장된 최대 슬롯과 현재 반다라트 수 중 큰 값을 사용해 기존 사용자의 표가 잠기지 않는다.
- 확장된 최대 슬롯은 Android와 iOS가 공유하는 DataStore 경로에 영구 저장되고, 표를 삭제해도 줄어들지 않는다.
- 슬롯 확장 1회는 최대 슬롯을 정확히 1개 늘린다.
- Android debug는 Google 테스트 광고 ID, release는 운영 광고 ID를 사용한다.
- GMA Next-Gen SDK는 앱 시작 시 백그라운드에서 한 번만 초기화한다.

## 설계

### 공통 슬롯 정책

`core:domain`에 SDK와 무관한 순수 정책을 둔다.

- `FREE_BANDALART_SLOT_COUNT = 3`
- 현재 반다라트 수와 기본 슬롯 중 큰 값으로 최소 최대 슬롯을 계산한다.
- 현재 보유 수가 최대 슬롯보다 작을 때만 광고 없이 생성 가능하다고 판단한다.

`BandalartSlotRepository`는 현재 보유 수를 받아 저장된 최대 슬롯을 보정하거나 1개 확장한다. 구현은 기존 `BandalartDataStore`를 사용하고 Metro graph에서 앱 범위 단일 인스턴스로 제공한다.

### 기존 사용자 보정

저장 값이 없는 기존 사용자는 첫 조회에서 `max(3, 현재 보유 개수)`를 저장한다. 저장 값이 있더라도 현재 보유 개수보다 작으면 같은 규칙으로 올린다. 이후 표 삭제는 저장된 최대 슬롯을 낮추지 않는다.

### Android 광고 SDK

`androidApp`에 GMA Next-Gen SDK를 추가한다. 광고 ID는 build type별 Android resource로 분리한다.

- debug: Google 공식 테스트 App ID, Rewarded/Banner ad unit ID
- release: AdMob 운영 App ID, Rewarded/Banner ad unit ID

`BandalartApplication`은 앱 시작 시 전용 initializer를 호출한다. initializer는 중복 호출을 막고 IO dispatcher에서 SDK를 초기화한다. 이번 단계에서는 광고를 load/show하지 않는다.

## 단계 분리

이번 PR에 포함하지 않는 작업:

- 4번째 생성 시 안내 dialog와 Rewarded Ad 표시
- reward callback 이후 슬롯 확장 및 새 반다라트 생성 연결
- 광고 load/show 실패 시 fail-open 처리
- 홈 하단 Anchored Adaptive Banner UI
- 광고 이벤트 분석
- iOS 광고 SDK

위 항목은 플랫폼 gateway와 Home 생성 flow를 함께 설계하는 후속 PR에서 구현한다. 현재의 최대 5개 생성 제한은 보상형 flow가 연결될 때 교체해, 광고를 볼 방법 없이 사용자가 3개에서 막히는 중간 상태를 배포하지 않는다.

## 개인정보 동의 범위

현재 국내 배포 범위에서는 UMP SDK를 이번 단계에 포함하지 않는다. EEA, 영국, 스위스 등 동의가 필요한 지역으로 배포 범위를 확대하거나 개인 맞춤 광고를 요청하기 전에는 CMP/UMP 구성, 개인정보 옵션 진입점, Play Console 및 개인정보처리방침을 별도 작업으로 완료한다.

## 검증

- 슬롯 정책의 기본값, 기존 사용자 보정, 생성 가능 여부 단위 테스트
- DataStore의 최초 보정, 저장 값 유지, 1회 확장 단위 테스트
- 저장소가 DataStore에 올바른 최소값을 전달하고 결과를 반환하는 단위 테스트
- Metro graph 구성 및 Android 광고 resource/dependency 정적 확인
- 실제 광고 초기화와 테스트 광고 load/show는 후속 UI 연동 단계에서 debug 빌드로 검증
