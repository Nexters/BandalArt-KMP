---
name: github-release
description: YeoBee Android GitHub Release 생성 workflow. 사용자가 yb:github-release, github-release, release v1.0.0을 말하거나 develop 변경분을 main 릴리즈 커밋으로 반영하고 버전 태그와 GitHub Release를 생성해달라고 요청할 때 사용합니다.
---

# YB GitHub Release 생성

YeoBee Android의 GitHub Release 절차를 자동화한다.

1. 릴리즈 버전 bump 성격을 사용자에게 확인한다.
2. `main`에서 `release/v{version}` 브랜치를 만들고 `develop`의 파일 스냅샷을 단일 release 커밋으로 적용한다.
3. PR 본문에 커밋 기반 릴리즈 노트를 남긴다.
4. `release/v{version}` -> `main` PR을 squash merge한다.
5. PR 머지 후 `main`의 release squash commit에 annotated tag를 생성한다.
6. 해당 태그로 GitHub Release를 생성한다.

## 입력

- 버전: 사용자가 명시하지 않았으면 반드시 질문한다.
  - `major`, `minor`, `patch` 중 어떤 bump인지 먼저 묻는다.
  - 사용자가 `1.0.0` 또는 `v1.0.0`을 직접 준 경우에도 현재 최신 태그 대비 bump 성격을 보여주고 확인받는다.
  - Git 태그명은 `v{major}.{minor}.{patch}`로 정규화한다.
- 소스 브랜치: 기본값 `develop`. 이 브랜치의 파일 스냅샷만 release 브랜치에 적용한다.
- 대상 브랜치: 기본값 `main`.
- 릴리즈 브랜치: 기본값 `release/v{versionName}`. 반드시 대상 브랜치에서 생성한다.
- PR 제목: 항상 `release v{versionName}` 형식으로 고정한다.
- 라벨: `skip-ci`를 포함하고, 사용자가 다르게 요청하지 않으면 `⚙️ chore`도 포함한다.

## 규칙

- 버전을 agent가 임의로 결정하지 않는다.
- 릴리즈 PR 제목은 임의 요약이나 prefix 없이 `release v{versionName}`만 사용한다.
- 요청 버전이 기존 태그와 같거나 이미 존재하면 warning을 표시하고 workflow를 중단한다. 같은 버전으로 PR 생성/수정/merge/tag/release를 진행하지 않는다.
- PR이 머지되기 전에는 태그를 만들지 않는다.
- 릴리즈 PR도 기존 main 이력 정책을 따르며 squash merge만 사용한다. merge commit 또는 rebase merge를 사용하지 않는다.
- `develop` 또는 source branch를 `main`에 직접 merge하지 않는다. source branch를 부모로 가진 release 브랜치를 만들지 않는다.
- release 브랜치는 항상 `origin/main`에서 생성하고, source branch의 파일 스냅샷만 단일 release 커밋으로 적용한다.
- release 브랜치의 HEAD는 부모가 1개여야 하며, 그 부모는 작업 시작 시점의 `origin/main`이어야 한다.
- release 커밋 제목과 squash merge 제목은 `release v{versionName}`로 맞춘다.
- 태그는 항상 머지된 `origin/main` 커밋에 붙인다. `develop`에 먼저 붙이지 않는다.
- 태그는 annotated tag로 만든다.
  - `git tag -a v1.0.0 origin/main -m "YeoBee Android 1.0.0"`
- GitHub auto-generated notes를 사용하지 않는다.
- 릴리즈 노트는 이전 버전 태그부터 현재 버전까지의 커밋 목록으로 작성한다.
- 릴리즈 노트의 각 커밋 항목에는 contributor mention을 `by @login` 형식으로 붙인다.
- 릴리즈 노트 하단에는 unique contributor mention 목록을 `## Contributors` 섹션으로 추가한다.
- 이전 버전 태그가 없으면 첫 릴리즈로 보고 PR 생성 시점에는 source branch의 전체 release 대상 PR/커밋을 사용한다.
- PR 머지 후에도 준비된 릴리즈 노트 파일을 기준으로 GitHub Release를 생성한다.
- GitHub Release 노트와 Play Store 릴리즈 노트는 별도다. 사용자가 명시하지 않으면 `app/src/main/play/release-notes/ko-KR/internal.txt`를 수정하지 않는다.
- secret, keystore, service account JSON 등 민감 파일 내용을 출력하지 않는다.

## 실행 절차

### 1단계: 상태 확인

아래를 실행한다.

```bash
git status --short --branch
git fetch origin {base}
git fetch origin {source}
git fetch origin --tags
git tag --list "v*" --sort=-version:refname
```

- 관련 없는 로컬 변경은 stage/revert하지 않는다.
- 최신 태그를 기준으로 현재 버전을 파악한다.

### 2단계: 버전 bump 확인

사용자가 버전을 명시하지 않았으면 반드시 질문한다.

- major: 호환되지 않는 큰 변경
- minor: 기능 추가
- patch: 버그 수정/운영 수정

사용자가 bump 종류만 답하면 최신 태그 기준으로 다음 버전을 계산한다. 최신 태그가 없으면 첫 릴리즈 버전을 직접 확인한다.

사용자가 버전을 명시했으면 다음을 보여주고 확인받는다.

- 최신 태그
- 요청 버전
- 추정 bump 종류
- 생성할 태그명

같은 태그가 이미 있으면 warning을 표시하고 즉시 중단한다. 이 경우 release PR 생성/수정도 하지 않는다.

```bash
git tag --list v{version}
git ls-remote --tags origin v{version}
```

### 3단계: 릴리즈 노트 범위 결정

이전 릴리즈 태그를 확인한다.

```bash
git tag --merged origin/{base} --list "v[0-9]*" --sort=-version:refname
```

가장 최신 태그를 `previous_tag`로 사용한다.

PR 머지 전:

- 이전 GitHub Release가 있으면 이전 Release 생성/게시 시각 이후 source branch에 머지된 PR/커밋을 사용한다.
- 이전 GitHub Release가 없으면 첫 릴리즈로 보고 source branch의 전체 release 대상 PR/커밋을 사용한다.
- 이전 릴리즈가 이미 별도 기준으로 준비된 노트 파일을 가지고 있으면 그 기준을 이어받는다.

PR 머지 후:

- 준비된 릴리즈 노트 파일이 있으면 그 파일을 재사용한다.
- 이전 태그가 있더라도 `{previous_tag}..origin/main`만으로 source branch의 개별 PR/커밋 목록을 복원하려고 하지 않는다.
- 이전 태그가 없고 준비된 노트 파일이 있으면 그 파일 재사용
- 이전 태그와 준비된 노트 파일이 모두 없으면 첫 tagged release로 보고 사용자에게 릴리즈 노트 기준을 확인한다.

주의: 이 repository는 main 이력을 release squash commit 중심으로 유지한다. `main`은 source branch의 개별 커밋 이력을 포함하지 않으므로 `{previous_tag}..origin/{source}` 같은 ancestry 기반 범위는 과거 커밋을 과다 포함할 수 있다. 릴리즈 노트는 GitHub PR merge 시각, 이전 Release 생성/게시 시각, 또는 준비된 릴리즈 노트 파일을 기준으로 작성한다.

커밋은 오래된 순서로 작성한다.

```bash
git log {range} --oneline --reverse
```

각 커밋 contributor는 아래 순서로 결정한다.

1. 커밋 제목에 `(#123)` 같은 PR 번호가 있으면 해당 PR author login을 사용한다.
   - `gh pr view 123 --json author --jq '.author.login'`
   - 많은 PR을 처리할 때는 `gh api --paginate 'repos/YeoBee-official/YeoBee-Android/pulls?state=all&per_page=100'`로 PR number -> author login map을 만들어 사용한다.
2. PR 번호가 없으면 commit author email/name을 GitHub login으로 매핑한다.
3. login을 확정할 수 없으면 추측하지 말고 사용자에게 확인한다.

GitHub Release 본문 형식:

```markdown
## Release v1.0.0

- Version: 1.0.0
- Previous tag: {previous_tag or none}
- Commit range: {range description}
- Target commit: {origin/main sha after merge}

## Commits

- {hash} {subject} by @{login}

## Contributors

- @{login}
```

### 4단계: release 브랜치 생성 및 PR 생성/확인

기존 PR을 확인한다.

```bash
gh pr list --base {base} --head release/v{versionName} --state open --json number,title,url,labels
```

기존 PR이 없으면 release 브랜치를 만든다.

```bash
git checkout -B release/v{versionName} origin/{base}
git diff --binary origin/{base} origin/{source} > /tmp/yb-release-v{versionName}.patch
git apply --index /tmp/yb-release-v{versionName}.patch
git status --short
git commit -m "release v{versionName}"
git push -u origin release/v{versionName} --force-with-lease
```

검증:

```bash
git rev-list --count origin/{base}..HEAD
git show -s --pretty=%P HEAD
git diff --stat HEAD origin/{source}
```

- `git rev-list --count origin/{base}..HEAD`는 `1`이어야 한다.
- `git show -s --pretty=%P HEAD`는 작업 시작 시점의 `origin/{base}` 커밋 1개만 출력해야 한다.
- `git diff --stat HEAD origin/{source}`는 비어 있어야 한다.
- release 브랜치가 source branch를 부모로 가지면 안 된다.

기존 PR이 없으면 `.github/PULL_REQUEST_TEMPLATE.md`를 읽어 원본 주석과 섹션을 유지한 채 본문을 만든다.

필수 작성 내용:

- `## 🔗 관련 이슈`: `- 릴리즈: v{version}`
- `## 📙 작업 설명`: source branch 파일 스냅샷을 base branch 기반 release 브랜치에 단일 커밋으로 반영하는 릴리즈 PR임을 설명
- `## 💬 추가 설명 or 리뷰 포인트 (선택)`: `### 릴리즈 노트`와 전체 커밋 목록 작성

PR 생성:

```bash
gh pr create \
  --base {base} \
  --head release/v{versionName} \
  --title "release v{versionName}" \
  --label "⚙️ chore" \
  --label skip-ci \
  --assignee @me \
  --body-file {prepared_pr_body}
```

기존 PR이 있으면 필요한 경우에만 제목/본문/라벨을 수정한다.

### 5단계: 머지 확인 및 squash merge

아래로 머지 상태를 확인한다.

```bash
gh pr view {number} --json state,mergedAt,mergeStateStatus,mergeable,url
gh pr checks {number}
```

- 이미 머지되어 있으면 6단계로 진행한다.
- 머지 전이면 `mergeStateStatus`와 checks가 merge 가능한 상태인지 확인한다.
- 사용자에게 최종 확인을 받은 뒤 squash merge한다.
- merge commit 또는 rebase merge로 머지하지 않는다.
- source branch는 삭제하지 않는다.
- source branch에 태그를 붙이는 방식으로 우회하지 않는다.

```bash
gh pr merge {number} --squash --subject "release v{versionName}"
```

### 6단계: main에 annotated tag 생성

PR 머지 후 실행한다.

```bash
git fetch origin {base}
git fetch origin --tags
git tag -a v{version} origin/{base} -m "YeoBee Android {version}"
git push origin v{version}
```

검증:

```bash
git show v{version} --no-patch
```

### 7단계: GitHub Release 생성

태그와 준비된 릴리즈 노트로 Release를 생성한다.

```bash
gh release create v{version} \
  --title "v{version}" \
  --notes-file {prepared_release_notes}
```

검증:

```bash
gh release view v{version} --json tagName,name,url,isDraft,isPrerelease
```

## 결과 보고

아래 항목을 보고한다.

- 릴리즈 PR URL과 머지 상태
- 태그명과 태그 대상 커밋
- GitHub Release URL
- 릴리즈 노트에 사용한 커밋 범위
- 남아 있는 로컬 미커밋 변경
