---
name: commit
description: BandalArt 변경사항을 검토하고 의도한 파일만 한국어 Conventional Commit으로 커밋한다. 사용자가 commit, 커밋, 커밋해줘라고 요청할 때 사용한다.
---

# Commit

1. `git status --short --branch`, staged/unstaged stat과 관련 diff를 확인한다.
2. secret, local config, service account key와 관련 없는 파일을 제외한다.
3. 의도한 파일만 명시적 경로로 stage한다.
4. 사용자 지정 메시지가 없으면 `{type}: {한국어 요약}` 형식으로 작성한다.
5. `git commit`을 실행하고 hook을 우회하지 않는다.
6. commit hash, 제목, hook 결과와 남은 변경을 보고한다.

AI generated 문구와 AI co-author trailer를 넣지 않는다. 전체 빌드는 이 workflow에서 실행하지 않는다.
