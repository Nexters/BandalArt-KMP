---
name: commit-push
description: BandalArt 변경사항을 검토해 의도한 파일만 커밋하고 현재 브랜치를 push한다. 사용자가 commit-push, 커밋 푸시, 작업 커밋하고 올려줘라고 요청할 때 사용한다.
---

# Commit Push

1. `plugins/bandalart/skills/commit/SKILL.md` 절차로 상태, diff, 민감 파일을 확인하고 커밋한다.
2. commit hook이 실패하면 수정 결과를 보고하고 push하지 않는다.
3. upstream이 있으면 `git push`, 없으면 `git push -u origin {branch}`를 실행한다.
4. commit hash, 제목, branch, hook/push 결과와 남은 변경을 보고한다.

`--no-verify`, AI generated 문구와 AI co-author trailer를 사용하지 않는다.
