---
name: create-pr
description: BandalArt PR 템플릿을 보존해 GitHub PR을 생성한다. 사용자가 create-pr, create pr, PR 만들어줘, PR 생성해줘라고 요청할 때 사용한다.
---

# Create PR

## 입력

- head: 기본값 현재 브랜치
- base: 명시값 우선, 없으면 merge-base와 branch history로 추론, fallback `main`
- reviewer: 사용자가 명시한 경우에만 지정

## 절차

1. head/base와 원격 존재 여부를 확인한다.
2. head가 push되지 않았으면 사용자에게 알린 뒤 `git push -u origin {head}`를 실행한다.
3. base를 fetch하고 commit 목록과 `git diff origin/{base}...{head} --stat`을 확인한다.
4. branch, commit, 문서와 대화에서 관련 GitHub issue를 찾는다. 불확실하면 issue 번호를 만들지 않는다.
5. `.github/PULL_REQUEST_TEMPLATE.md`를 완전히 읽고 HTML 주석과 section 구조를 유지한다.
6. 작업 설명과 실제 검증 결과만 작성한다. umbrella issue의 일부 단계라면 `Close` 대신 `#번호`로 연결한다.
7. 다음 중 실제 존재하는 label 하나를 선택한다.
   - `feat:` → `feature`
   - `fix:` → `hotfix`
   - `chore:` → `⚙️ chore`
   - `docs:` → `documentation`
   - `refactor:` → `refactoring`
   - `test:` → `test-coding`
8. base/head, title, label, assignee, reviewer와 전체 본문을 미리보기로 보여주고 확인받는다.
9. `gh pr create`로 assignee `@me`를 지정해 PR을 만든다. reviewer는 명시된 경우에만 추가한다.
10. PR 번호, URL, title, label, assignee, base/head를 보고한다.

`ship-next`가 이 skill을 호출한 경우에는 `ship-next` 호출 자체를 승인으로 보고 별도 확인을 반복하지 않는다.
