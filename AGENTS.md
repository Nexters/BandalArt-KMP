# BandalArt 프로젝트 작업 지침

## 작업 원칙

- 속도보다 신중함을 우선한다.
- 코딩 전에 가정과 성공 기준을 명확히 한다.
- 다단계 마이그레이션이나 구조 변경은 구현 전에 `docs/` 아래 전략 MD를 작성한다.
- 요청하지 않은 기능, 추상화, 방어 로직과 인접 리팩터링을 추가하지 않는다.
- 기존 코드 스타일을 따르고 자신의 변경으로 생긴 미사용 코드만 정리한다.
- 병렬화 가능한 탐색과 검증은 묶어서 실행한다.
- 관련 파일 수정이 끝난 뒤 대표 테스트와 정적 검사를 실행한다.

## 빌드

- 일반 기능 작업의 전체 Android/iOS 빌드는 사용자가 직접 수행한다.
- 전체 빌드가 필요하면 실행 명령과 이유를 사용자에게 알린다.
- 배포 workflow처럼 산출물 생성이 목적인 경우에만 해당 skill의 명시적 절차에 따라 빌드한다.

## 브랜치

- 브랜치명에 `codex/` 접두사를 사용하지 않는다.
- base는 명시된 값이 우선이며, 없으면 브랜치 이력과 이슈 계획에서 추론하고 최종 fallback은 `main`이다.
- 관련 없는 변경을 같은 브랜치나 PR에 섞지 않는다.

## 커밋과 push

- 요청받지 않은 경우 커밋하거나 push하지 않는다.
- 커밋 메시지에 AI generated 문구나 AI co-author trailer를 넣지 않는다.
- `commit` 요청은 `plugins/bandalart/skills/commit/SKILL.md`를 따른다.
- `commit-push` 요청은 `plugins/bandalart/skills/commit-push/SKILL.md`를 따른다.
- `push` 요청은 `plugins/bandalart/skills/push/SKILL.md`를 따른다.

## PR

- `create-pr`, `create pr`, PR 생성 요청은 `plugins/bandalart/skills/create-pr/SKILL.md`를 따른다.
- reviewer는 사용자가 명시한 경우에만 지정한다.
- PR 템플릿의 HTML 주석과 section 구조를 유지한다.
- umbrella issue는 일부 단계 PR에서 `Close`로 닫지 않는다.
- 관리자 권한으로 branch protection이나 review 조건을 우회해야 하면 실행 전에 사용자에게 명시하고 승인을 받는다.

## 연속 출하

- `ship-next`, “CI 통과 후 머지하고 다음 작업 시작” 요청은 `plugins/bandalart/skills/ship-next/SKILL.md`를 따른다.
- `ship-next` 호출은 정상 경로의 commit, push, PR 생성, CI 감시, 일반 merge와 다음 브랜치 생성을 한 번에 승인한 것으로 본다.
- CI 실패, merge 차단, 관리자 우회 필요, secret/권한 누락, 다음 단계 불명확 시에는 멈추고 보고한다.

## Android 배포

- Android Firebase App Distribution 배포 요청은 `plugins/bandalart/skills/deploy-android-firebase/SKILL.md`를 따른다.
- Android Play Store Internal Testing 배포 요청은 `plugins/bandalart/skills/deploy-android-playstore/SKILL.md`를 따른다.
- 배포 skill 이름은 Android 대상임을 드러내는 `deploy-android-*` 형식만 사용한다.
- 두 workflow는 Android APK/AAB만 다룬다. iOS, TestFlight, App Store 또는 공통 KMP release를 포함하지 않는다.
- 실패했던 Fastlane 설정은 배포 근거 또는 fallback으로 사용하지 않는다.
- credential 파일과 값은 출력하거나 Git에 추가하지 않는다.

## 리뷰와 새 작업

- 로컬 리뷰 요청은 `plugins/bandalart/skills/review/SKILL.md`를 따른다.
- Gemini Code Assist 리뷰 반영 요청은 `plugins/bandalart/skills/resolve-gemini-review/SKILL.md`를 따른다.
- 새 작업 시작이나 이슈·worktree 생성 요청은 `plugins/bandalart/skills/start-workflow/SKILL.md`를 따른다.

## 테스트

- KMP 테스트 source set 선택, Circuit Presenter 테스트, 로컬·CI 실행 범위는 `docs/architecture/kmp/KMP_TESTING_GUIDE.md`를 따른다.

## 상태 관리

- Compose와 Circuit 상태의 소유권·수명, `remember*` API와 영속 저장 선택은 `docs/architecture/state/COMPOSE_STATE_LIFETIME_GUIDE.md`를 따른다.
- 새 문서는 `docs/README.md`의 분류 규칙에 따라 주제 폴더에 두고, `docs/` 루트에는 인덱스와 백로그만 둔다.

## MCP 설정

- Codex MCP 설정은 `~/.codex/config.toml`의 `mcp_servers`에서 관리한다.
- Claude Code나 Claude Desktop 설정 파일을 수정하지 않는다.
