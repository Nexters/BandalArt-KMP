---
name: ship-next
description: BandalArt 현재 변경을 commit/push하고 PR을 생성한 뒤 CI 성공 시 일반 merge하고 계획된 다음 작업 branch와 전략 문서까지 연속으로 시작한다. 사용자가 ship-next, CI 통과 후 머지하고 다음 작업 시작, 이 작업 올리고 다음 단계 진행이라고 요청할 때 사용한다.
---

# Ship Next

호출 한 번으로 현재 변경의 출하와 다음 계획 단계 시작을 연결한다. 호출 자체를 정상 경로의 commit, push, PR 생성, CI 감시, 일반 merge와 다음 branch 생성에 대한 승인으로 본다.

## 입력

- base: 명시값 우선, 없으면 branch history와 issue 계획에서 추론, fallback `main`
- next: 다음 작업 제목 또는 issue 단계. 없으면 연결된 roadmap/strategy 문서에서 순서를 찾는다.
- merge method: 기본값 저장소의 최근 merged PR과 동일한 일반 merge 방식
- `--dry-run`: 변경 없이 1~3단계의 사전 점검과 이후 실행 계획만 보고한다.

## 절차

1. 현재 branch, worktree, diff, 관련 issue, 전략 문서와 검증 결과를 확인한다.
2. unrelated/untracked/secret 파일을 제외하고 현재 작업의 완료 조건을 점검한다.
3. 필수 검증이 빠졌으면 실행 가능한 test/static check를 수행한다. 전체 Android/iOS 빌드는 사용자가 명시적으로 맡긴 경우에만 실행한다.
   - `--dry-run`이면 검증 명령을 실제 실행하지 않고 필요한 명령, 예상 base/PR/merge/next branch와 중단 조건만 보고한 뒤 종료한다.
4. `plugins/bandalart/skills/commit-push/SKILL.md`에 따라 commit하고 push한다.
5. `plugins/bandalart/skills/create-pr/SKILL.md`로 PR 본문 미리보기를 commentary에 보여주되 추가 승인을 기다리지 않고 PR을 생성한다.
6. required check를 `gh pr checks --watch` 또는 run 단위로 추적한다.
7. CI 실패 시 로그와 변경 원인의 연관성을 확인한다.
   - 현재 작업 범위의 재현 가능한 실패면 최소 수정, 검증, commit/push 후 다시 기다린다.
   - secret/권한/외부 장애, 새 설계 결정 또는 범위 확장이 필요하면 중단하고 보고한다.
8. 모든 required check가 성공하면 PR의 `mergeable`, `mergeStateStatus`, `reviewDecision`을 확인한다.
9. 관리자 우회 없이 정상 merge가 가능할 때만 merge한다.
   - branch protection 또는 review 조건 우회가 필요하면 실행 전에 사용자에게 명시하고 승인을 받는다.
10. merge를 확인하고 `origin/{base}`를 fetch한다.
11. 다음 단계가 명확하면 branch/worktree 존재 여부를 먼저 확인한다.
   - 없으면 최신 base에서 `codex/` 없는 새 branch/worktree를 만든다.
   - 이미 있으면 dirty 변경을 보존하고 같은 작업인지 확인한 뒤 재사용한다. 임의 reset/rebase나 중복 branch 생성은 하지 않는다.
12. migration/refactor의 다음 단계라면 코드보다 먼저 `docs/` 전략 MD를 작성하고 범위·비범위·검증 기준을 고정한다.
13. merged PR, CI 결과, 새 branch/worktree, 다음 목표와 남은 수동 검증을 보고한다.

## 중단 조건

- CI 실패가 현재 변경만으로 안전하게 해결되지 않음
- merge conflict 또는 branch protection/review 관리자 우회 필요
- GitHub auth, secret, 외부 서비스 권한 누락
- umbrella issue에서 다음 미완료 단계가 둘 이상이라 우선순위가 불명확함
- dirty worktree의 관련 없는 변경과 작업 변경을 분리할 수 없음

umbrella issue의 일부 단계 PR에는 `Close #issue`를 사용하지 않는다. 전체 완료 조건을 충족했을 때만 issue를 닫는다.
