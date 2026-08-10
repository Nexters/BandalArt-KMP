# Android 2.2.22 릴리스 노트 조사

## 목적과 결론

이 문서는 공개 Android 2.2.13을 기준으로 현재 `main`의 Android 2.2.22까지 병합된 작업을 조사해, 사용자에게 실제로 보이는 변경과 내부 작업을 구분한 기록이다. 최종 스토어 문구나 다국어 번역본이 아니라 릴리스 노트 작성의 근거와 한국어 후보만 제공한다.

가장 설명 가치가 큰 변화는 다음 네 가지다.

1. 한국어 반다라트 템플릿 5종 추가
2. 설정에서 켤 수 있는 마감일 알림과 알림을 통한 해당 반다라트 이동
3. Fluent Color 이모지 300개, 최근 사용, 카테고리 탐색
4. 태스크 셀 롱클릭 완료·완료 해제와 진동

광고는 새로 추가됐지만 제품의 대표 기능처럼 강조하지 않는다. 릴리스 노트에 포함한다면 홈 하단 배너와, 무료 슬롯을 모두 사용한 뒤 광고 안내를 거쳐 반다라트를 추가할 수 있는 정책 변경을 사실 위주로 짧게 고지하는 편이 적절하다. 근거는 [슬롯·SDK 기반 #241](https://github.com/Nexters/BandalArt-KMP/pull/241), [보상형 생성 흐름 #243](https://github.com/Nexters/BandalArt-KMP/pull/243), [홈 배너 #247](https://github.com/Nexters/BandalArt-KMP/pull/247)이다.

## 기준선, 범위, 조사 방법

- 공개 기준선은 2026-08-07 병합된 [PR #225](https://github.com/Nexters/BandalArt-KMP/pull/225)다. 이 PR은 stale Gradle 생성물이 포함된 2.2.12 AAB를 대체하기 위해 Android 2.2.13을 준비했고, UI 변경이 없다고 명시했다.
- 종료점은 조사 시점 `main` HEAD인 [PR #274](https://github.com/Nexters/BandalArt-KMP/pull/274)다. [PR #272](https://github.com/Nexters/BandalArt-KMP/pull/272)에서 Android 버전을 `2.2.22 (20222)`로 올렸다.
- `main`의 first-parent merge 이력을 기준으로 #225를 포함하면 46개, 기준선 다음부터 #274까지는 45개 PR이다. 번호가 연속이지 않은 것은 해당 번호가 `main` 병합 PR이 아니기 때문이다. 특히 [PR #238](https://github.com/Nexters/BandalArt-KMP/pull/238)은 닫혔지만 병합되지 않아 범위에서 제외했다.
- GitHub CLI로 PR의 병합 상태, 제목, 본문, 변경 파일을 확인하고, 로컬 Git의 `766ca6e4`(#225 merge)부터 `21c094ea`(#274 merge)까지의 first-parent 이력과 diff를 대조했다. 직접 근거는 GitHub PR 메타데이터와 저장소 코드·diff만 사용했다.
- 작업 트리의 미병합 변경은 조사 범위에 넣지 않았다. 따라서 이 문서의 판단은 `21c094ea` 시점 `main`에 한정된다.

## 릴리스 노트 후보

아래 문장은 우선순위와 사실 범위를 보여 주는 한국어 초안이다. 최종 스토어 문구와 번역본은 별도로 다듬어야 한다.

### 우선 포함

- **반다라트 템플릿:** 새 반다라트를 만들 때 취업 준비, 운동 습관, 공부 계획, 재테크 습관, 여행 준비의 5가지 한국어 템플릿으로 시작할 수 있다. 템플릿 선택 즉시 25칸 반다라트가 만들어지고 기존 편집 흐름으로 이어진다. ([#264](https://github.com/Nexters/BandalArt-KMP/pull/264))
- **마감일 알림:** 설정에서 마감일 알림을 켜면 마감일 당일 오전 9시부터 기기 알림을 받을 수 있고, 알림을 누르면 해당 반다라트가 열린다. 권한 차단 시 시스템 설정으로 이동하는 동선과 시간·시간대 변경 후 예약 갱신도 포함한다. ([공통 계산·저장 기반 #250](https://github.com/Nexters/BandalArt-KMP/pull/250), [Android 알림·설정·이동 #251](https://github.com/Nexters/BandalArt-KMP/pull/251))
- **Fluent 이모지:** 목표 아이콘을 Fluent Color 이모지 300개에서 고를 수 있고, 최근 사용 이모지와 카테고리 탭으로 탐색할 수 있다. 최종 UI에는 검색창이 없으므로 검색 기능을 릴리스 노트에 쓰면 안 된다. ([렌더러 #230](https://github.com/Nexters/BandalArt-KMP/pull/230), [300개 피커 #231](https://github.com/Nexters/BandalArt-KMP/pull/231), [최근 사용 #232](https://github.com/Nexters/BandalArt-KMP/pull/232), [검색·카테고리 제거 #236](https://github.com/Nexters/BandalArt-KMP/pull/236), [카테고리 탭 재도입 #245](https://github.com/Nexters/BandalArt-KMP/pull/245), [아이콘·레이블 개선 #257](https://github.com/Nexters/BandalArt-KMP/pull/257), [ripple 제거 #265](https://github.com/Nexters/BandalArt-KMP/pull/265))
- **빠른 완료:** 제목이 있는 태스크 셀을 길게 눌러 편집 시트를 열지 않고 완료할 수 있으며, 완료된 셀을 다시 길게 누르면 완료가 해제된다. 저장 성공 시 진동으로 알려준다. ([완료·진동 #227](https://github.com/Nexters/BandalArt-KMP/pull/227), [완료 해제 #256](https://github.com/Nexters/BandalArt-KMP/pull/256))

### 조건부 또는 짧게 포함

- **광고와 추가 슬롯:** 홈 하단에 320×50 배너 광고가 표시될 수 있다. 기본 무료 슬롯을 모두 사용한 뒤 새 반다라트를 추가하면 안내 팝업이 먼저 나오고, 사용자가 동의해 보상형 광고를 끝까지 본 경우 슬롯과 반다라트가 하나씩 추가된다. 광고 SDK 실패는 생성이 막히지 않도록 처리되지만 광고를 중간에 닫으면 생성되지 않는다. 광고 자체를 혜택이나 신기능으로 강조하지 말고 생성 정책 변경으로 중립적으로 설명한다. ([#241](https://github.com/Nexters/BandalArt-KMP/pull/241), [#243](https://github.com/Nexters/BandalArt-KMP/pull/243), [#247](https://github.com/Nexters/BandalArt-KMP/pull/247), [배너 높이 #254](https://github.com/Nexters/BandalArt-KMP/pull/254), [홈 간격 #259](https://github.com/Nexters/BandalArt-KMP/pull/259), [안내 아이콘 #260](https://github.com/Nexters/BandalArt-KMP/pull/260))
- **화면 다듬기:** 이모지 선택창과 여러 모달 바텀시트의 하단 여백을 정리하고, 홈 대표 이모지를 키웠다. 공유 버튼과 배너 사이 간격도 줄였다. 주요 기능보다 우선순위가 낮으므로 공간이 남을 때 한 문장으로 묶는 것이 적절하다. ([#242](https://github.com/Nexters/BandalArt-KMP/pull/242), [#253](https://github.com/Nexters/BandalArt-KMP/pull/253), [#255](https://github.com/Nexters/BandalArt-KMP/pull/255), [#259](https://github.com/Nexters/BandalArt-KMP/pull/259))
- **업데이트 안내 정책:** 일반 배포는 사용을 막지 않는 선택 업데이트로, 긴급 배포만 필수 업데이트로 안내하도록 정책을 분리했다. 2.2.22 자체는 priority 0 검증용이므로, 사용자가 기존 강제 업데이트 동작을 경험한 경우에만 간단한 안정성 개선으로 포함한다. ([정책 구현 #270](https://github.com/Nexters/BandalArt-KMP/pull/270), [2.2.22 설정 #272](https://github.com/Nexters/BandalArt-KMP/pull/272))

## 최종 상태를 쓸 때 주의할 점

- [#231](https://github.com/Nexters/BandalArt-KMP/pull/231)은 검색과 카테고리를 함께 도입했지만 [#236](https://github.com/Nexters/BandalArt-KMP/pull/236)이 둘 다 제거했다. [#245](https://github.com/Nexters/BandalArt-KMP/pull/245)는 카테고리 탭만 다시 추가했다. 따라서 2.2.22의 기능을 “이모지 검색”으로 표현하면 사실과 다르다.
- [#241](https://github.com/Nexters/BandalArt-KMP/pull/241)의 SDK·슬롯 기반만으로는 사용자 UI가 완성되지 않았고, 실제 생성 안내와 보상 흐름은 [#243](https://github.com/Nexters/BandalArt-KMP/pull/243), 홈 배너는 [#247](https://github.com/Nexters/BandalArt-KMP/pull/247)에서 연결됐다. 광고 관련 설명은 이 최종 조합을 기준으로 한다.
- Android 릴리스 노트에서 iOS 전용 구현을 섞지 않는다. iOS 마감 알림은 [#261](https://github.com/Nexters/BandalArt-KMP/pull/261)이며 Android의 사용자 근거는 [#251](https://github.com/Nexters/BandalArt-KMP/pull/251)이다.
- [#273](https://github.com/Nexters/BandalArt-KMP/pull/273)의 Ding 알림 inspector는 debug 빌드에만 있고 release에는 no-op이므로 사용자 기능으로 소개하지 않는다. [#274](https://github.com/Nexters/BandalArt-KMP/pull/274)도 그 디버그 의존성의 호환성 수정이다.

## 내부 작업 제외 목록과 이유

다음 작업은 제품의 안정성이나 출하에 필요하지만 Android 스토어 릴리스 노트의 독립 항목으로는 제외한다.

- **조사·생성 파이프라인:** Fluent Emoji 리소스 spike와 카탈로그 측정은 앱 런타임을 바꾸지 않은 준비 작업이다. 최종 사용자 변화는 후속 renderer·picker PR에 귀속한다. ([#228](https://github.com/Nexters/BandalArt-KMP/pull/228), [#229](https://github.com/Nexters/BandalArt-KMP/pull/229))
- **버전·Internal Testing 메타데이터:** 앱 버전과 3개 언어 내부 테스트 안내만 바꾼 출하 준비다. 실제 기능 PR을 중복해 설명하지 않는다. ([#233](https://github.com/Nexters/BandalArt-KMP/pull/233), [#240](https://github.com/Nexters/BandalArt-KMP/pull/240), [#252](https://github.com/Nexters/BandalArt-KMP/pull/252), [#258](https://github.com/Nexters/BandalArt-KMP/pull/258), [#262](https://github.com/Nexters/BandalArt-KMP/pull/262), [#266](https://github.com/Nexters/BandalArt-KMP/pull/266), [#272](https://github.com/Nexters/BandalArt-KMP/pull/272))
- **문서·배포 자동화:** 상태 관리·문서 구조, 알림 전략, Fastlane CD, 버전 정책 문서 변경은 사용자 UI나 동작을 직접 추가하지 않는다. ([#244](https://github.com/Nexters/BandalArt-KMP/pull/244), [#249](https://github.com/Nexters/BandalArt-KMP/pull/249), [#248](https://github.com/Nexters/BandalArt-KMP/pull/248), [#271](https://github.com/Nexters/BandalArt-KMP/pull/271))
- **iOS 전용 작업:** Firebase·폰트 크기·TestFlight와 서명 복구, iOS 알림은 Android 릴리스 노트 범위 밖이다. 공통 사용자 기능의 Android 구현이 별도로 있으면 그 PR만 사용한다. ([#237](https://github.com/Nexters/BandalArt-KMP/pull/237), [#239](https://github.com/Nexters/BandalArt-KMP/pull/239), [#246](https://github.com/Nexters/BandalArt-KMP/pull/246), [#261](https://github.com/Nexters/BandalArt-KMP/pull/261), [#263](https://github.com/Nexters/BandalArt-KMP/pull/263), [#267](https://github.com/Nexters/BandalArt-KMP/pull/267), [#268](https://github.com/Nexters/BandalArt-KMP/pull/268), [#269](https://github.com/Nexters/BandalArt-KMP/pull/269))
- **디버그 도구:** Ding inspector와 minSdk 호환 수정은 release 제품에서 동작하지 않는다. ([#273](https://github.com/Nexters/BandalArt-KMP/pull/273), [#274](https://github.com/Nexters/BandalArt-KMP/pull/274))
- **독립 항목이 아닌 기반 작업:** 슬롯 저장·광고 SDK 기반 [#241](https://github.com/Nexters/BandalArt-KMP/pull/241), 알림 계산·저장 기반 [#250](https://github.com/Nexters/BandalArt-KMP/pull/250)은 후속 사용자 흐름을 설명할 때 근거로 묶되 단독 기능처럼 쓰지 않는다.

## PR별 근거 부록

| PR | 분류 | 확인된 내용과 릴리스 노트 판단 |
|---|---|---|
| [#225](https://github.com/Nexters/BandalArt-KMP/pull/225) | 기준선 | Android 2.2.13 clean release 복구. UI 변경이 없는 공개 기준점이므로 후보 범위에는 포함하지 않는다. |
| [#227](https://github.com/Nexters/BandalArt-KMP/pull/227) | 사용자 노출·핵심 | 미완료 태스크 셀 롱클릭 완료와 저장 성공 후 진동. 빠른 완료 후보의 시작점이다. |
| [#228](https://github.com/Nexters/BandalArt-KMP/pull/228) | 내부 | Fluent Emoji 20개 리소스 spike와 생성 파이프라인. 런타임 변경 없음. |
| [#229](https://github.com/Nexters/BandalArt-KMP/pull/229) | 내부 | 300개 Color 카탈로그를 측정·확정한 검증 단계. 앱에는 아직 포함하지 않았다. |
| [#230](https://github.com/Nexters/BandalArt-KMP/pull/230) | 사용자 노출·핵심 | Fluent Color 300개와 공통 renderer를 Home·편집·Complete에 적용했다. |
| [#231](https://github.com/Nexters/BandalArt-KMP/pull/231) | 사용자 노출·중간 상태 | 300개 picker와 검색·카테고리를 도입했지만 검색·카테고리는 #236에서 제거됐다. 최종 후보에는 300개 picker만 남는다. |
| [#232](https://github.com/Nexters/BandalArt-KMP/pull/232) | 사용자 노출·핵심 | 최근 선택을 중복 없이 최신순 최대 12개 저장하고 최근 사용 카테고리를 제공한다. |
| [#233](https://github.com/Nexters/BandalArt-KMP/pull/233) | 내부·릴리스 | Android 2.2.14와 Fluent Emoji Internal 노트 준비. 기능 근거는 #230~#232다. |
| [#236](https://github.com/Nexters/BandalArt-KMP/pull/236) | 사용자 노출·최종 상태 교정 | 300개와 Unicode 저장은 유지하고 기존 6열·4행 UI를 복원하며 검색·카테고리를 제거했다. |
| [#237](https://github.com/Nexters/BandalArt-KMP/pull/237) | 내부·iOS | iOS Firebase·Crashlytics 연동 복구. Android 사용자 노트에서 제외한다. |
| [#239](https://github.com/Nexters/BandalArt-KMP/pull/239) | 내부·iOS 최적화 | 미사용 폰트 리소스를 제거해 iOS release 크기를 줄인 작업. 사용자 기능이 아니다. |
| [#240](https://github.com/Nexters/BandalArt-KMP/pull/240) | 내부·릴리스 | Android 2.2.15 Internal 준비와 기존 picker UI 안내. |
| [#241](https://github.com/Nexters/BandalArt-KMP/pull/241) | 내부 기반·광고 | 기본 무료 슬롯 3개, 영구 슬롯 저장, GMA SDK 초기화 기반. 실제 사용자 광고 흐름은 후속 PR에서 완성됐다. |
| [#242](https://github.com/Nexters/BandalArt-KMP/pull/242) | 사용자 노출·보조 | 이모지 선택창의 중복 하단 inset과 navigation bar 배경을 수정했다. |
| [#243](https://github.com/Nexters/BandalArt-KMP/pull/243) | 사용자 노출·정책 | 슬롯 소진 뒤 안내, 동의, 보상 완료를 거쳐 슬롯과 반다라트를 하나 추가하는 흐름을 연결했다. |
| [#244](https://github.com/Nexters/BandalArt-KMP/pull/244) | 내부·문서 | 상태 수명 기준과 문서 구조를 정리했다. 제품 동작 변경 없음. |
| [#245](https://github.com/Nexters/BandalArt-KMP/pull/245) | 사용자 노출·핵심 | 전체·최근·9개 metadata 그룹 카테고리 탭을 compact picker에 다시 추가했다. 검색은 추가하지 않았다. |
| [#246](https://github.com/Nexters/BandalArt-KMP/pull/246) | 내부·iOS 릴리스 | iOS 1.1.0 TestFlight 후보와 검증 전략. Android 범위 밖이다. |
| [#247](https://github.com/Nexters/BandalArt-KMP/pull/247) | 사용자 노출·정책 | Android 홈 하단 배너와 노출·숨김 조건을 연결했다. 이후 #254에서 320×50으로 조정됐다. |
| [#248](https://github.com/Nexters/BandalArt-KMP/pull/248) | 내부·배포 | Android Play Internal·iOS TestFlight Fastlane CD 복구와 검증 강화. |
| [#249](https://github.com/Nexters/BandalArt-KMP/pull/249) | 내부·문서 | 로컬 알림 조사·전략만 추가했다. 기능 구현은 #250~#251이다. |
| [#250](https://github.com/Nexters/BandalArt-KMP/pull/250) | 내부 기반·부분 수정 | 알림 계산·저장 기반과 기본 OFF 설정을 추가하고 마감일 제거 저장을 수정했다. 플랫폼 UI는 아직 없다. |
| [#251](https://github.com/Nexters/BandalArt-KMP/pull/251) | 사용자 노출·핵심 | Android 설정 토글·권한 동선, WorkManager 예약, 알림 탭 시 해당 반다라트 이동을 구현했다. |
| [#252](https://github.com/Nexters/BandalArt-KMP/pull/252) | 내부·릴리스 | Android 2.2.19 Internal 버전과 마감 알림 테스트 노트 준비. |
| [#253](https://github.com/Nexters/BandalArt-KMP/pull/253) | 사용자 노출·보조 | 목표 입력·목록·설정 모달 시트의 중복 system inset을 제거하고 하단 간격을 통일했다. |
| [#254](https://github.com/Nexters/BandalArt-KMP/pull/254) | 사용자 노출·광고 보정 | adaptive 배너를 고정 320×50으로 줄이고 320dp 미만에서는 요청과 빈 슬롯을 생략했다. |
| [#255](https://github.com/Nexters/BandalArt-KMP/pull/255) | 사용자 노출·보조 | 홈 프로필 카드의 대표 이모지를 22dp에서 36dp로 키웠다. |
| [#256](https://github.com/Nexters/BandalArt-KMP/pull/256) | 사용자 노출·핵심 | 완료된 태스크 셀 롱클릭 시 완료 해제를 지원해 #227의 동작을 토글로 확장했다. |
| [#257](https://github.com/Nexters/BandalArt-KMP/pull/257) | 사용자 노출·보조 | 카테고리 탭을 단색 탐색 아이콘으로 바꾸고 선택 이름·자동 스크롤을 추가했다. |
| [#258](https://github.com/Nexters/BandalArt-KMP/pull/258) | 내부·릴리스 | 2.2.19 Internal 노트를 당시 병합 기능 전체 기준으로 갱신했다. |
| [#259](https://github.com/Nexters/BandalArt-KMP/pull/259) | 사용자 노출·보조 | 표와 공유 버튼 사이 간격을 64dp에서 16dp로 줄여 배너가 있는 홈 구성을 보정했다. |
| [#260](https://github.com/Nexters/BandalArt-KMP/pull/260) | 사용자 노출·보조 | 광고 안내 팝업의 추가 아이콘을 `add_circle`로 교체했다. |
| [#261](https://github.com/Nexters/BandalArt-KMP/pull/261) | 사용자 노출·iOS 전용 | iOS 마감 알림 구현. 기능 자체는 보이지만 Android 릴리스 노트에서는 제외한다. |
| [#262](https://github.com/Nexters/BandalArt-KMP/pull/262) | 내부·릴리스 | Android 2.2.20 Internal 버전·테스트 노트 준비. |
| [#263](https://github.com/Nexters/BandalArt-KMP/pull/263) | 내부·iOS 릴리스 | TestFlight 안내에 iOS 알림 검증 항목을 추가했다. |
| [#264](https://github.com/Nexters/BandalArt-KMP/pull/264) | 사용자 노출·핵심 | 기존 생성 시트에 한국어 템플릿 5종을 추가하고 선택 즉시 25칸 보드를 원자 생성한다. |
| [#265](https://github.com/Nexters/BandalArt-KMP/pull/265) | 사용자 노출·보조 | 이모지 카테고리 탭의 시각적 ripple만 제거하고 선택 상태·터치 영역·접근성은 유지했다. |
| [#266](https://github.com/Nexters/BandalArt-KMP/pull/266) | 내부·릴리스 | Android 2.2.21과 템플릿·이모지 ripple Internal 노트 준비. |
| [#267](https://github.com/Nexters/BandalArt-KMP/pull/267) | 내부·iOS 배포 | provisioning profile 만료일 검증의 `DateTime` 처리 수정. |
| [#268](https://github.com/Nexters/BandalArt-KMP/pull/268) | 내부·iOS 배포 | 앱 profile이 Swift Package target에 전파되지 않도록 서명 범위를 수정했다. |
| [#269](https://github.com/Nexters/BandalArt-KMP/pull/269) | 내부·iOS 배포 | iOS 앱 target의 Release 구성에만 수동 App Store 서명을 적용했다. |
| [#270](https://github.com/Nexters/BandalArt-KMP/pull/270) | 사용자 동작·조건부 | SemVer와 강제 업데이트를 분리하고 Play priority 4 이상만 Immediate로 처리한다. |
| [#271](https://github.com/Nexters/BandalArt-KMP/pull/271) | 내부·문서 | Android/iOS 버전과 배포 우선순위 운영 규칙을 문서화했다. |
| [#272](https://github.com/Nexters/BandalArt-KMP/pull/272) | 내부·릴리스 | Android 2.2.22(20222), priority 0, 3개 언어 Internal 검증 노트를 준비했다. |
| [#273](https://github.com/Nexters/BandalArt-KMP/pull/273) | 내부·debug | debug 빌드에만 알림 payload inspector를 연결하고 release에는 no-op을 넣었다. |
| [#274](https://github.com/Nexters/BandalArt-KMP/pull/274) | 내부·debug | Ding 0.5.1로 올려 debug minSdk 우회를 제거했다. UI와 release 동작 변경 없음. |

## 권장 우선순위

Play의 짧은 변경 사항에는 템플릿, 마감일 알림, Fluent 이모지, 롱클릭 완료 토글을 먼저 담는다. 광고 정책은 사용자에게 보이는 변화이므로 공간이 허용되면 중립적인 고지 한 문장을 추가한다. 화면 간격과 아이콘, 업데이트 priority는 핵심 항목을 모두 담은 뒤 남는 공간에서 묶어 설명한다. 내부·iOS·debug·배포 작업은 포함하지 않는다.
