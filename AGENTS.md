@.claude/CODING.md

# YeoBee 프로젝트 작업 지침

## 코드 작성 행동 원칙

속도보다 신중함을 우선시합니다.

### 1. 코딩 전에 생각하기

- 가정을 명시적으로 표현하고, 불명확한 점이 있으면 질문한다
- 여러 해석이 가능하면 선택지를 제시하되, 조용히 하나를 골라 진행하지 않는다
- 더 간단한 접근이 있으면 먼저 언급한다
- 혼란스러운 부분이 있으면 멈추고 질문한다

### 2. 단순성 우선

- 요청된 것 이상의 기능을 추가하지 않는다
- 일회용 코드에 추상화를 만들지 않는다
- 요청되지 않은 유연성이나 설정 가능성을 넣지 않는다
- 발생할 수 없는 시나리오에 대한 에러 처리를 하지 않는다
- 200줄로 작성한 코드가 50줄로 가능하면 다시 작성한다
- 실제 관측된 edge case가 아니라면 과하게 사전 대응하는 로직을 넣지 않는다
- 끝없는 논리적 완결성보다 실무적으로 충분한 구현을 우선한다

### 3. 정밀한 변경

기존 코드 편집 시:
- 인접 코드, 주석, 포맷을 개선하지 않는다
- 깨지지 않은 코드를 리팩토링하지 않는다
- 기존 코드 스타일에 맞춘다
- 관련 없는 데드 코드를 발견하면 삭제하지 않고 언급만 한다

자신의 변경으로 인해 미사용된 import/변수/함수만 정리한다.

### 4. 목표 기반 실행

- 성공 기준을 먼저 정의하고, 검증될 때까지 반복한다
- 작업을 검증 가능한 목표로 변환한다
- 다단계 작업은 간단한 계획을 수립하고 각 단계마다 검증한다

### 5. 도구 호출 및 검증 효율

- 병렬화 가능한 도구 호출은 병렬 호출을 우선한다
- 파일 탐색도 순차 탐색이 필요하지 않다면 복수의 `rg`, `sed`, `Get-Content` 등을 한 번에 호출한다
- 수정 및 동작 검증은 작은 단위마다 반복하기보다 기능/파일 단위로 모아서 한 번에 처리하는 것을 우선한다
- 모든 작은 함수마다 테스트하지 말고, 기능 또는 파일 단위 검증을 먼저 고려한다
- 매 코드 수정마다 `git diff`를 확인하지 않고, 일반적으로 파일 전체 수정이 끝난 뒤 한 번 확인한다
- 단, 작성한 코드가 동작을 보장하기 어려울 정도로 복잡하거나 여러 함수/파일에 걸친 경우에는 필요한 시점에 즉시 검증한다

## 참조 문서

- 작업 워크플로우 (이슈 → 브랜치 → 커밋 → PR): @.claude/WORKFLOW.md
- 코딩 가이드 (ViewModel, Compose 컨벤션): @.claude/CODING.md
- Figma UI 구현/수정 가이드: @docs/FIGMA_DESIGN.md

## Figma 디자인 구현 관련

- Figma 기반 UI 작업 시 반드시 @docs/FIGMA_DESIGN.md를 따른다
- 구현 후 텍스트, 색상, 크기, 간격, 상태를 Figma와 최소 1회씩 대조한 뒤 완료로 판단한다
- `LazyColumn.spacedBy`, parent padding, item 내부 `Spacer`처럼 최종 화면 간격에 합산되는 값을 놓치지 않는다

## 빌드 관련

- **빌드는 사용자가 직접 수행합니다**
- 기능 작업 완료 후 빌드를 자동으로 실행하지 마세요 (시간이 오래 걸림)
- 빌드가 필요한 경우 사용자에게 알리고 사용자가 직접 실행하도록 합니다

## 브랜치 관련

- **브랜치명에 `codex/` 접두사를 사용하지 않습니다**

## 커밋 관련

- **커밋 메시지에서 Codex 관련 문구를 제거합니다**
- 다음 문구들을 커밋 메시지에 포함하지 마세요:
    - `🤖 Generated with ...` (Codex/Claude Code 등 AI 도구 서명 문구)
    - `Co-Authored-By: ...` (AI 모델 서명, 모델명 무관)
- 커밋 작업은 사용자가 직접 수행하는 경우가 많으므로, 요청받지 않은 경우 커밋하지 마세요
- Codex CLI에서는 `/` 접두사가 내장 명령으로 처리되므로 `yb:commit`처럼 `/` 없이 입력합니다
- `yb:commit`, `commit` 또는 커밋 요청은 `plugins/yb/skills/commit/SKILL.md`의 Codex-native workflow를 따릅니다
- `yb:commit-push`, `commit-push` 요청이 들어오면 `plugins/yb/skills/commit-push/SKILL.md`의 Codex-native workflow를 따릅니다
- `yb:push`, `push` 또는 push 요청은 `plugins/yb/skills/push/SKILL.md`의 Codex-native workflow를 따릅니다
- 기존 Claude skill(`.claude/plugins/yb/skills/commit/SKILL.md`, `.claude/plugins/yb/commands/commit.md`, `.claude/plugins/yb/commands/commit-push.md`, `.claude/plugins/yb/commands/push.md`)은 legacy/reference로 유지합니다

## PR 생성 관련

- **PR 생성 요청 시 반드시 `plugins/yb/skills/create-pr/SKILL.md` workflow를 사용합니다**
- `yb:create-pr`, `create pr`, `create-pr`, "PR 만들어줘", "PR 생성해줘" 등의 요청이 들어오면 수동으로 처리하지 말고, Codex-native create-pr 절차를 따릅니다
- 수동으로 PR을 생성하면 템플릿 양식 보존, 미리보기 확인, base 브랜치 자동 추론 등이 누락됩니다
- 기존 Claude skill(`.claude/plugins/yb/skills/create-pr/SKILL.md`, `.claude/plugins/yb/commands/create-pr.md`)은 legacy/reference로 유지합니다

## Firebase 배포 관련

- **Firebase 배포 요청 시 반드시 `plugins/yb/skills/deploy-firebase/SKILL.md` workflow를 사용합니다**
- `yb:deploy-firebase`, `deploy firebase`, `deploy-firebase` 등의 요청이 들어오면 debug/release, release notes, Firebase 환경, Discord 알림 조건을 확인한 뒤 절차를 따릅니다
- 배포 workflow는 APK 생성이 목적이므로 예외적으로 Gradle build를 실행할 수 있습니다
- `.env`, service account JSON, Discord webhook URL 등 민감 정보는 출력하지 않습니다
- 기존 Claude skill(`.claude/plugins/yb/skills/deploy-firebase/SKILL.md`, `.claude/plugins/yb/commands/deploy-firebase.md`)은 legacy/reference로 유지합니다

## 로컬 리뷰 관련

- **로컬 코드 리뷰 요청 시 `plugins/yb/skills/review/SKILL.md` workflow를 사용합니다**
- `yb:review`, `review`, "리뷰해줘", "로컬 리뷰" 요청은 base 브랜치 기준 Kotlin diff와 `.claude/CODING.md`를 기준으로 리뷰합니다
- 변경된 코드(diff의 추가/수정 라인)만 리뷰하고, 기존 코드 문제는 지적하지 않습니다
- 기존 Claude skill(`.claude/plugins/yb/skills/review/SKILL.md`, `.claude/plugins/yb/commands/review.md`)은 legacy/reference로 유지합니다

## Gemini Code Assist 리뷰 관련

- **Gemini Code Assist 리뷰 확인/반영 요청 시 반드시 `plugins/yb/skills/resolve-gemini-review/SKILL.md` workflow를 사용합니다**
- "Gemini 리뷰 확인해줘", "리뷰 반영해줘" 등의 요청이 들어오면 수동으로 처리하지 말고, Codex-native resolve-gemini-review 절차를 따릅니다
- 반복 리뷰 패턴의 단일 소스는 `.yeobee/shared/yb/learned-patterns.md`이며, Claude/Codex workflow가 같은 파일을 갱신합니다
- 수동으로 리뷰를 처리하면 대댓글 형식, PR 요약 코멘트, learned-patterns.md 업데이트 등이 누락됩니다
- 기존 Claude skill(`.claude/plugins/yb/skills/resolve-gemini-review/SKILL.md`, `.claude/plugins/yb/commands/resolve-gemini-review.md`)은 legacy/reference로 유지합니다

## 새 작업 시작 관련

- **새 작업 시작 요청 시 `plugins/yb/skills/start-workflow/SKILL.md` workflow를 사용합니다**
- `yb:start-workflow`, `start-workflow`, "새 작업 시작", "이슈 만들고 워크트리 만들어줘" 요청은 GitHub Issue, branch, worktree 흐름을 따릅니다
- GUI 앱 실행이나 iTerm/Android Studio 조작은 필요 시 사용자 승인 후 진행합니다
- 기존 Claude skill(`.claude/plugins/yb/skills/start-workflow/SKILL.md`, `.claude/plugins/yb/commands/start-workflow.md`)은 legacy/reference로 유지합니다

## MCP 설정 관련

- **Codex CLI의 MCP 설정 파일 위치**
    - MCP 설정은 `~/.codex/config.toml`의 `mcp_servers` 섹션에서 관리됩니다
    - Claude Code용 설정 파일(`~/.claude.json`)이나 Claude Desktop 설정 파일을 수정하지 마세요
