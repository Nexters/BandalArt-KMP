---
name: push
description: BandalArt 현재 브랜치를 안전하게 push한다. 사용자가 push, 푸시, 원격에 올려줘라고 요청할 때 사용한다.
---

# Push

1. `git status --short --branch`와 현재 브랜치를 확인한다.
2. detached HEAD이면 중단한다.
3. upstream이 있으면 `git push`, 없으면 `git push -u origin {branch}`를 실행한다.
4. branch, remote와 남은 로컬 변경을 보고한다.

파일을 stage하거나 commit하지 않는다.
