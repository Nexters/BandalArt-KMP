# BandalArt 백로그 실행 우선순위

- 갱신일: 2026-08-08
- 기준 브랜치: `origin/main`
- 대상: KMP/Circuit/Metro 기반 앱의 안정화와 신규 기능 백로그

## 현재 기준점

- 계층별 KMP, Circuit, Metro 통합 작업인 #180, #181, #182는 코드 기준 완료되어 닫았다.
- 포그라운드 복귀 시 강제 업데이트를 다시 검사하는 #186 구현은 PR #215로 `main`에 병합했다.
- KMP 테스트 가이드 #209, Circuit 상태 보존과 날짜 피커 복원 #210, 설정 화면 이메일 문의 #207은 각각 PR #216, #218, #220으로 `main`에 병합해 완료했다.
- 태스크 셀 롱클릭 완료와 KMP 햅틱 #213은 PR #227로 `main`에 병합했다.
- Fluent UI Emoji #212는 Color 300개 resource pipeline, 공통 renderer, picker·최근 사용과 Android 출시 검증을 완료했고 PR #245에서 metadata category 탐색을 추가했다. iOS 실제 기기·artifact 검증은 #214로 이관했다.
- AdMob #206은 PR #241에서 무료 슬롯 정책과 Android GMA SDK 기반을, PR #243에서 사전 안내→Rewarded→정확히 한 개 생성과 process 복구를 병합했다. 현재 후속 작업에서 홈 Banner와 test-ad Internal 검증을 진행한다.
- 따라서 다음 작업은 남은 마이그레이션이 아니라 테스트·상태 정책을 고정한 뒤 작은 기능부터 확장하는 순서다.

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

공통 scheduler 계약 뒤 Android와 iOS 로컬 알림을 플랫폼별로 구현한다. 생성·수정·완료·삭제, 권한, 시간대, 재부팅에 대한 idempotent reschedule을 검증한다.

### 7. #206 AdMob Rewarded Ad 기반 추가 생성 — L

PR #241에서 기본 무료 슬롯 3개, 영구 슬롯 저장·보정과 Android GMA SDK 기반을 병합했다. PR #243에서 사전 안내, Rewarded 완료 뒤 정확히 한 번 생성, 광고 실패 fail-open과 process 복구를 연결했다. 현재 후속 작업에서 홈 Anchored Adaptive Banner를 연결하고 Rewarded·Banner 테스트 광고를 함께 Internal 검증한다.

### 8. #208 목표 템플릿 기반 빠른 생성 — L

#212 아이콘 catalog와 #206 생성 gate 뒤에 통합한다. 템플릿 콘텐츠·도메인 검증은 미리 준비할 수 있지만 UI 출하는 두 선행 작업 후 진행한다.

### 9. #156 Android/iOS 위젯 — XL

앱 본체의 데이터·완료·알림 흐름이 안정된 뒤 Android Glance MVP, iOS App Group spike, WidgetKit 순으로 진행한다.

## 별도 운영 트랙

- #214: iOS 개발자 계정 복구 후 실제 기기, 저장소, 설정, navigation, 배포 검증
- #113: legacy Fastlane credential 폐기·회전과 Android Internal CD 보안 정리
- #206 외부 준비: AdMob app/rewarded/banner unit, UMP, app-ads.txt, Play Console 광고·Data safety 선언

이 운영 작업은 준비되는 즉시 병행할 수 있지만, 미완료 상태가 앞선 Android 기능 개발을 막지는 않는다.

## Internal Testing checkpoint

버전 증가와 Internal Testing은 아래처럼 실제 기기 검증 가치가 있는 묶음에서만 수행한다.

1. #210 + #207: 상태 복원과 platform mail launcher
2. #213: long click과 햅틱
3. #212: 이모지 에셋 용량·성능·다크 모드 — Android 완료, iOS는 #214로 이관
4. #211: 알림 권한·예약·재예약
5. #206: Rewarded 생성 flow — 구현 완료, Home Banner와 Android test ad end-to-end는 Internal Testing에서 확인
6. #208: 템플릿 + 광고 생성 end-to-end

## 바로 이어갈 작업

#214 iOS `1.1.0` TestFlight 업데이트 검증을 진행한다. CD 환경이 준비되면 GitHub workflow를 우선 사용하고, 준비 전에는 Xcode Organizer를 fallback으로 사용한다. 기존 App Store `1.0.1` 위 업데이트, 데이터 보존, Circuit/Metro 흐름, Fluent Emoji category와 App Thinning 결과를 검증한다. Firebase 콘솔 관측은 비차단 후속 검증으로 분리하며, 구체적인 절차는 `releases/ios/IOS_TESTFLIGHT_UPDATE_STRATEGY.md`를 따른다.
