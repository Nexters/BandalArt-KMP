# BandalArt 백로그 실행 우선순위

- 갱신일: 2026-08-07
- 기준 브랜치: `origin/main`
- 대상: KMP/Circuit/Metro 기반 앱의 안정화와 신규 기능 백로그

## 현재 기준점

- 계층별 KMP, Circuit, Metro 통합 작업인 #180, #181, #182는 코드 기준 완료되어 닫았다.
- 포그라운드 복귀 시 강제 업데이트를 다시 검사하는 #186 구현은 PR #215로 `main`에 병합했다.
- 태스크 셀 롱클릭 완료와 KMP 햅틱 #213은 PR #227로 `main`에 병합했다.
- Fluent UI Emoji #212는 PR #228에서 재현 가능한 resource spike 기반을 만들었고, 후속 검증에서 Color 300개를 v1 기준으로 확정했다.
- iOS 실제 기기·배포 검증은 계정이 복구될 때 수행할 후속 이슈 #214로 분리했다.
- 따라서 다음 작업은 남은 마이그레이션이 아니라 테스트·상태 정책을 고정한 뒤 작은 기능부터 확장하는 순서다.

## 실행 원칙

- 최신 `origin/main`에서 이슈별 브랜치를 만들고, 구조 변경은 구현 전에 `docs/` 전략 문서를 작성한다.
- 한 PR은 독립적으로 머지·되돌릴 수 있는 한 가지 책임만 가진다.
- 자동 테스트와 CI가 통과한 뒤 다음 단계로 이동한다.
- 앱 버전 증가는 실제 Internal Testing checkpoint에서만 한다.
- 외부 SDK, Room schema, 권한, 플랫폼 lifecycle 변경은 작은 UI 기능과 섞지 않는다.

## 실행 순서

### 1. #209 KMP 테스트와 Circuit/Molecule 문서화 — XS

현재 test source set, dependency, Gradle task와 CI 범위를 문서로 고정한다. 이후 모든 PR의 검증 기준으로 사용한다.

### 2. #210 Circuit 상태 보존 정책과 날짜 피커 복원 — S

`remember`, `rememberRetained`, `rememberSaveable`의 책임을 감사하고, 날짜 피커의 미확정 draft가 구성 변경에서 사라지는 문제를 수정한다. 신규 기능이 사용할 one-shot effect 기준도 함께 확인한다.

Compose UI restoration을 실제 UI tree에서 자동 검증하는 runner·CI 도입은 #217로 분리한다. #210의 blocker로 삼지 않고, gesture와 platform interaction 테스트가 누적될 때 별도 인프라 작업으로 진행한다.

### 3. #207 설정 화면 이메일 문의 — S

설정 화면에서 `mraz3068@gmail.com`으로 문의 메일 작성기를 여는 Android/iOS launcher를 추가한다. 메일 앱 부재 fallback과 URI encoding을 포함한다.

### 4. #213 햅틱 기반 태스크 셀 빠른 완료 — 완료

PR #227에서 task cell long click으로 바텀시트를 열지 않고 완료 처리하며, 성공 시 KMP 햅틱을 한 번 실행하도록 적용했다. 일반 탭 동작은 유지한다.

### 5. #212 Fluent UI Emoji 선택기 — M~L

전체 에셋을 바로 포함하지 않고 스타일·용량 spike, pinned manifest와 resource pipeline, 공통 renderer, picker UI 순으로 나눈다. 템플릿 기능의 공통 아이콘 catalog 선행 작업이다. 1단계 검증 결과 v1은 Color 300개로 진행하며, 2단계에서 공통 renderer와 기존 노출 화면 전환을 적용한다.

### 6. #211 마감일 기반 로컬 알림 — L

공통 scheduler 계약 뒤 Android와 iOS 로컬 알림을 플랫폼별로 구현한다. 생성·수정·완료·삭제, 권한, 시간대, 재부팅에 대한 idempotent reschedule을 검증한다.

### 7. #206 AdMob Rewarded Ad 기반 추가 생성 — L

모든 사용자 추가 생성을 사전 안내와 reward 완료 뒤 허용한다. 광고 실패가 기존 표 사용을 막지 않도록 creation credit과 정확히 한 번 생성 경계를 먼저 설계한다.

### 8. #208 목표 템플릿 기반 빠른 생성 — L

#212 아이콘 catalog와 #206 생성 gate 뒤에 통합한다. 템플릿 콘텐츠·도메인 검증은 미리 준비할 수 있지만 UI 출하는 두 선행 작업 후 진행한다.

### 9. #156 Android/iOS 위젯 — XL

앱 본체의 데이터·완료·알림 흐름이 안정된 뒤 Android Glance MVP, iOS App Group spike, WidgetKit 순으로 진행한다.

## 별도 운영 트랙

- #214: iOS 개발자 계정 복구 후 실제 기기, 저장소, 설정, navigation, 배포 검증
- #113: legacy Fastlane credential 폐기·회전과 Android Internal CD 보안 정리
- #206 외부 준비: AdMob app/rewarded unit, UMP, app-ads.txt, Play Console 광고·Data safety 선언

이 운영 작업은 준비되는 즉시 병행할 수 있지만, 미완료 상태가 앞선 Android 기능 개발을 막지는 않는다.

## Internal Testing checkpoint

버전 증가와 Internal Testing은 아래처럼 실제 기기 검증 가치가 있는 묶음에서만 수행한다.

1. #210 + #207: 상태 복원과 platform mail launcher
2. #213: long click과 햅틱
3. #212: 이모지 에셋 용량·성능·다크 모드
4. #211: 알림 권한·예약·재예약
5. #206: test/production ad unit end-to-end
6. #208: 템플릿 + 광고 생성 end-to-end

## 바로 이어갈 작업

#212의 Color 300개 catalog와 공통 renderer를 바탕으로 picker UI, 검색·카테고리, 최근 사용 순서로 진행한다.
