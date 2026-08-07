---
name: create-pr
description: Bandalart Android PR creation workflow. Use when the user says create-pr, create pr, PR 만들어줘, or asks Codex to create a GitHub PR while preserving the project template.
---

# Bandalart Create PR

## Inputs

- Optional `--branch` / `-b`: source branch. Default: current branch.
- Optional `--base`: target branch. Default: infer from branch history, fallback `develop`.
- Optional `--reviewer`: reviewer to request when the user explicitly names one.

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
7. Use a reviewer only when the user explicitly names one. Do not infer or require a reviewer.
8. Show a preview with base/head, label, assignee, optional reviewer, title, and full body. Ask for confirmation before creating the PR.
9. Create the PR with `gh pr create`.
   - Use `--assignee @me`.
   - Include `--reviewer {reviewer}` only when a reviewer was explicitly provided.
10. Report PR number, URL, title, label, assignee, optional reviewer, base, and head branch.

## Constraints

- Do not remove or reshape the PR template.
- Do not include Codex/Claude boilerplate in the PR body.
- Do not invent or require a reviewer for this personal project.
- If `gh` auth or network fails, stop with the failing command context.
