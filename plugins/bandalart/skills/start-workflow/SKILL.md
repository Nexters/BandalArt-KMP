---
name: start-workflow
description: BandalArt 새 작업을 GitHub issue, branch와 worktree로 시작한다. 사용자가 start-workflow, 새 작업 시작, 이슈 만들고 worktree 만들어줘라고 요청할 때 사용한다.
---

# Start Workflow

## 입력

- title: 새 작업 제목
- issue: 기존 GitHub issue 번호. 없으면 필요 시 생성
- base: 기본값 `main`
- branch: 없으면 title에서 `{type}/{slug}` 형식으로 생성

## 절차

1. base를 fetch하고 현재 remote 상태를 확인한다.
2. 기존 issue가 없으면 title, 설명, assignee `@me`와 저장소에 존재하는 label로 issue를 생성한다.
3. branch 이름에 `codex/`를 사용하지 않는다.
4. main repo의 common git dir을 기준으로 충돌하지 않는 worktree 경로를 정한다.
5. `origin/{base}`에서 branch와 worktree를 생성하고 upstream으로 push한다.
6. 다단계 migration/refactor라면 구현 전 `docs/` 전략 MD를 작성한다.
7. issue URL, base, branch, worktree와 push 상태를 보고한다.

기존 worktree나 branch를 덮어쓰지 않는다. GUI 앱은 사용자가 요청한 경우에만 연다.
