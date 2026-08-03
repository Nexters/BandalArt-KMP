---
name: create-pr
description: YeoBee Android PR creation workflow in the yb plugin. Use when the user says yb:create-pr, create-pr, create pr, PR 만들어줘, or asks Codex to create a GitHub PR while preserving the project template.
---

# YB Create PR

Codex-native migration of Claude `yb:create-pr`. Keep `.claude/plugins/yb/skills/create-pr/SKILL.md` unchanged as legacy reference.

## Inputs

- Optional `--branch` / `-b`: source branch. Default: current branch.
- Optional `--base`: target branch. Default: infer from branch history, fallback `develop`.
- Required reviewer: the working counterpart who should review the PR.

## Workflow

1. Determine source branch and base branch.
   - If no base is provided, inspect history/merge-base and fallback to `develop`.
   - Verify base exists with `git ls-remote --heads origin {base}`.
2. Verify the source branch is pushed.
   - If missing on origin, run `git push -u origin {branch}` after telling the user.
3. Fetch and analyze:
   - `git fetch origin {base}`
   - `git log origin/{base}..{branch} --oneline --no-merges`
   - `git diff origin/{base}...{branch} --stat`
4. Derive issue identifiers from branch pattern `(YB-\d+)/#(\d+)`.
   - If no Notion issue is found, use `YB-00`.
5. Read `.github/PULL_REQUEST_TEMPLATE.md` and preserve it exactly.
   - Preserve all HTML comments and section headers.
   - Fill only `관련 이슈` and `작업 설명` unless the user asks otherwise.
6. Choose exactly one label from commit prefixes:
   - `feat:` -> `✨ feat`
   - `fix:` -> `🐞 fix`
   - `chore:` -> `⚙️ chore`
   - `docs:` -> `📃 docs`
   - `refactor:` -> `🔨 refactor`
   - `test:` -> `✅ test`
7. Determine the required reviewer.
   - Reviewer assignment is mandatory and must be the working counterpart for the task.
   - If the user explicitly named a reviewer, use that reviewer.
   - Otherwise, determine the current GitHub user with `gh api user --jq .login` and use the YeoBee counterpart mapping:
     - `easyhooon` -> `LeeOhHyung`
     - `LeeOhHyung` -> `easyhooon`
   - If the current user is not in the mapping, ask for the reviewer before showing the preview.
8. Show a preview with base/head, label, assignee, reviewer, title, and full body. Ask for confirmation before creating the PR.
9. Create the PR with `gh pr create`.
   - Use `--assignee @me`.
   - Always include `--reviewer {reviewer}`.
10. Report PR number, URL, title, label, assignee, reviewer, base, and head branch.

## Constraints

- Do not remove or reshape the PR template.
- Do not include Codex/Claude boilerplate in the PR body.
- If `gh` auth or network fails, stop with the failing command context.
