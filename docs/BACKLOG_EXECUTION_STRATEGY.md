# BandalArt 백로그 실행 우선순위

- 갱신일: 2026-08-09
- 기준 브랜치: `origin/main`
- 대상: KMP/Circuit/Metro 기반 앱의 안정화와 신규 기능 백로그

## 현재 기준점

- 계층별 KMP, Circuit, Metro 통합 작업인 #180, #181, #182는 코드 기준 완료되어 닫았다.
- 포그라운드 복귀 시 강제 업데이트를 다시 검사하는 #186 구현은 PR #215로 `main`에 병합했다.
- KMP 테스트 가이드 #209, Circuit 상태 보존과 날짜 피커 복원 #210, 설정 화면 이메일 문의 #207은 각각 PR #216, #218, #220으로 `main`에 병합해 완료했다.
- 태스크 셀 롱클릭 완료와 KMP 햅틱 #213은 PR #227로 `main`에 병합했다.
- Fluent UI Emoji #212는 Color 300개 resource pipeline, 공통 renderer, picker·최근 사용과 Android 출시 검증을 완료했고 PR #245에서 metadata category 탐색을 추가했다. iOS 실제 기기·artifact 검증은 #214로 이관했다.
- AdMob #206은 PR #241에서 무료 슬롯 정책과 Android GMA SDK 기반을, PR #243에서 사전 안내→Rewarded→정확히 한 개 생성과 process 복구를, PR #247에서 홈 Anchored Adaptive Banner를 병합했다. 보관된 `2.2.18 (20218)` AAB의 metadata·release note가 Play Internal release와 일치하며, 실제 설치본의 전체 Rewarded·Banner acceptance와 운영·개인정보 설정은 후속으로 남아 있다. Play API로 업로드 binary SHA나 Git SHA를 대조할 수는 없다.
- 따라서 다음 작업은 남은 마이그레이션이 아니라 테스트·상태 정책을 고정한 뒤 작은 기능부터 확장하는 순서다.

## 실제 출하 현황 체크리스트

2026-08-09 기준 코드 병합, 스토어 업로드, 수동 검증을 서로 다른 완료 조건으로 기록한다.

### `main` 병합·CI

- [x] `origin/main`은 PR #248까지 병합된 `0058885c`이며 해당 PR의 CI는 통과했다.
- [x] 현재 열린 PR은 없다.
- [x] PR #241의 무료 슬롯·GMA 기반, PR #243의 Rewarded 생성 흐름, PR #245의 Fluent Emoji category 탐색, PR #247의 adaptive test banner가 `main`에 병합됐다.

### Android Play Internal Testing

- [x] Play Internal Testing에 `2.2.18 (20218)`이 `completed` 상태로 업로드됐다.
- [x] PR #247 병합 시점 `374908af` worktree에 보관된 release AAB의 package·`2.2.18 (20218)` metadata와 release note가 Play release와 일치한다. Play API로 업로드 binary SHA나 Git SHA를 대조할 수는 없다.
- [x] AAB에 Rewarded·Banner 공식 test unit ID가 있고 production unit ID가 없음을 확인했다.
- [x] 사용자 수동 확인에서 Internal 설치본의 Rewarded 광고, 홈 Banner와 기존 컬러 emoji category 탐색 UI 노출을 확인했다.
- [ ] Rewarded 중도 종료 시 미생성, 보상 뒤 정확히 한 개 생성·영구 슬롯 유지, 광고 실패 fail-open과 Banner no-fill·재진입까지 전체 acceptance를 완료한다.
- [ ] 운영 AdMob unit, UMP, `app-ads.txt`, Play Console 광고·Data safety 선언을 완료한다.

### 로컬 작업 — 아직 Internal 미반영

- [ ] Teams식 단색 category 아이콘과 선택 category 제목: `fix/fluent-emoji-category-icons`에서 로컬 테스트 12개 통과, 아직 commit·PR·merge·Internal 배포 전이다.
- [ ] 홈 Banner `320x50` 고정과 추가 bottom sheet의 중복 window inset 제거: `fix/home-bottom-insets-banner-height`에서 로컬 테스트 2개 통과, 아직 commit·PR·merge·Internal 배포 전이다.
- [ ] #211 로컬 알림 조사·전략과 공통 foundation: 로컬 구현·검증 중이며 아직 commit·PR·merge·Internal 배포 전이다.

### iOS·TestFlight

- [ ] App Store Connect의 `1.1.0` build를 생성하고 TestFlight에 업로드한다. 현재 `1.1.0` build train은 0건이다.
- [ ] TestFlight 설치본에서 데이터 보존, Circuit/Metro, Fluent Emoji, navigation과 현재 iOS AdMob no-op/fail-open 경계를 검증한다. iOS Banner는 아직 구현되지 않았다.
- [x] 과거 TestFlight `1.0.0 (1·2·3)`과 `1.0.1 (2)`가 모두 만료돼 활성 build가 없음을 확인했다.

### Fastlane CD

- [x] PR #248의 Android/iOS Fastlane CD 복구 코드는 `main`에 병합됐고 CI는 통과했다.
- [x] Android signing을 포함한 기존 repository secret 이름은 등록돼 있다.
- [ ] 2026-08-08에 재발급한 Play publisher JSON으로 `PLAY_SERVICE_ACCOUNT_JSON`을 교체하고 preflight를 통과시킨다. 현재 secret 갱신일은 재발급보다 앞서므로 기존 값은 폐기된 것으로 취급한다.
- [ ] GitHub Environment를 구성한다. 현재 Environment는 0개다.
- [ ] iOS signing·App Store Connect secret을 등록한다. 현재 iOS 배포 secret은 없다.
- [ ] GitHub Actions에서 Android Internal과 iOS TestFlight workflow를 각각 최초 1회 성공시킨다. 현재 배포 workflow 실행 기록은 0건이다.

## 실행 원칙

- 최신 `origin/main`에서 이슈별 브랜치를 만들고, 구조 변경은 구현 전에 `docs/` 전략 문서를 작성한다.
- 한 PR은 독립적으로 머지·되돌릴 수 있는 한 가지 책임만 가진다.
- 자동 테스트와 CI가 통과한 뒤 다음 단계로 이동한다.
- 앱 버전 증가는 실제 Internal Testing checkpoint에서만 한다.
- 외부 SDK, Room schema, 권한, 플랫폼 lifecycle 변경은 작은 UI 기능과 섞지 않는다.

## 실행 순서

### 1. #209 KMP 테스트와 Circuit/Molecule 문서화 — 완료

PR #216에서 현재 test source set, dependency, Gradle task와 CI 범위를 문서로 고정했다. 이후 모든 PR의 검증 기준으로 사용한다.

### 2. #210 Circuit 상태 보존 정책과 날짜 피커 복원 — 완료

PR #218에서 `remember`, `rememberRetained`, `rememberSaveable`의 책임을 감사하고, 날짜 피커의 미확정 draft가 구성 변경에서 사라지는 문제를 수정했다. 신규 기능이 사용할 one-shot effect 기준도 함께 확인했다.

Compose UI restoration을 실제 UI tree에서 자동 검증하는 runner·CI 도입은 #217로 분리한다. #210의 blocker로 삼지 않고, gesture와 platform interaction 테스트가 누적될 때 별도 인프라 작업으로 진행한다.

### 3. #207 설정 화면 이메일 문의 — 완료

PR #220에서 설정 화면의 Android/iOS 메일 launcher, 메일 앱 부재 fallback과 URI encoding을 추가했다.

### 4. #213 햅틱 기반 태스크 셀 빠른 완료 — 완료

PR #227에서 task cell long click으로 바텀시트를 열지 않고 완료 처리하며, 성공 시 KMP 햅틱을 한 번 실행하도록 적용했다. 일반 탭 동작은 유지한다.

### 5. #212 Fluent UI Emoji 선택기 — Android/picker 구현 완료

Color 300개 catalog, pinned manifest와 resource pipeline, 공통 renderer, picker·최근 사용과 Android 출시 검증을 완료했고 PR #245에서 metadata category 탐색을 추가했다. iOS 실제 기기 렌더링·저장·artifact 크기는 #214 TestFlight 업데이트에서 검증한다.

### 6. #211 마감일 기반 로컬 알림 — L

공식 API와 저장소 접점을 조사해 제품 시간·집계·권한·reconcile 정책을 확정했고 예정 PR 2 공통 foundation까지 로컬 구현·검토·테스트했다. GitHub에 생성된 알림 PR은 아직 없다. 현재 남은 단계는 계획한 예정 PR 1 문서와 예정 PR 2 foundation 경계로 변경을 나눠 각각 commit·PR·CI·merge하는 것이다. 이후 Android와 iOS vertical slice를 플랫폼별로 구현하고 두 플랫폼이 준비된 뒤 설정 UX를 활성화한다. 생성·수정·완료·삭제, 권한, 시간대, 재부팅에 대한 idempotent reschedule을 검증한다. 구체적인 단계는 [로컬 알림 구현 전략](features/notifications/LOCAL_DEADLINE_NOTIFICATION_STRATEGY.md)을 따른다.

### 7. #206 AdMob Rewarded Ad 기반 추가 생성 — L

PR #241에서 기본 무료 슬롯 3개, 영구 슬롯 저장·보정과 Android GMA SDK 기반을 병합했다. PR #243에서 사전 안내, Rewarded 완료 뒤 정확히 한 번 생성, 광고 실패 fail-open과 process 복구를 연결했고 PR #247에서 홈 Anchored Adaptive Banner를 연결했다. 보관 AAB의 metadata·release note가 Play Internal의 `2.2.18 (20218)` release와 일치하며 Rewarded·Banner test unit ID도 확인했다. Play API로 binary SHA나 Git SHA를 대조할 수는 없다. 기본 노출은 설치본에서 확인했지만 전체 end-to-end acceptance와 운영·개인정보 설정은 아직 완료되지 않았다.

### 8. #208 목표 템플릿 기반 빠른 생성 — L

#212 아이콘 catalog와 #206 생성 gate 뒤에 통합한다. 템플릿 콘텐츠·도메인 검증은 미리 준비할 수 있지만 UI 출하는 두 선행 작업 후 진행한다.

### 9. #156 Android/iOS 위젯 — XL

앱 본체의 데이터·완료·알림 흐름이 안정된 뒤 Android Glance MVP, iOS App Group spike, WidgetKit 순으로 진행한다.

## 별도 운영 트랙

- #214: iOS 개발자 계정 복구 후 실제 기기, 저장소, 설정, navigation, 배포 검증
- #113: legacy Google key·Firebase token 폐기와 PR #248 Fastlane CD 복구 병합 완료, GitHub Environment·secret 구성과 첫 workflow 배포 검증 진행
- #206 외부 준비: AdMob app/rewarded/banner unit, UMP, app-ads.txt, Play Console 광고·Data safety 선언

이 운영 작업은 준비되는 즉시 병행할 수 있지만, 미완료 상태가 앞선 Android 기능 개발을 막지는 않는다.

## Internal Testing checkpoint

버전 증가와 Internal Testing은 아래처럼 실제 기기 검증 가치가 있는 묶음에서만 수행한다.

1. #210 + #207: 상태 복원과 platform mail launcher
2. #213: long click과 햅틱
3. #212: 이모지 에셋 용량·성능·다크 모드 — Android 완료, iOS는 #214로 이관
4. #211: 알림 권한·예약·재예약
5. #206: Rewarded 생성 flow·Home Banner test build — Internal 업로드와 기본 노출 확인, 전체 end-to-end acceptance는 미완료
6. #208: 템플릿 + 광고 생성 end-to-end

## 바로 이어갈 작업

#211 로컬 알림의 로컬 변경을 예정 PR 1 조사·전략과 예정 PR 2 공통 foundation 경계로 분리해 각각 commit·PR·CI·merge한다. 그다음 예정 PR 3 Android WorkManager vertical slice를 시작하고 플랫폼 권한·UI 활성화는 계획된 후속 PR로 유지한다. #214 iOS TestFlight와 #206 Android test-ad 전체 acceptance는 credential·배포 환경이 준비되는 즉시 별도 운영 트랙에서 병행한다.
