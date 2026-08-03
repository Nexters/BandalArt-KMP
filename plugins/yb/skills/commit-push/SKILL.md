---
name: commit-push
description: YeoBee Android changeset commit and push workflow in the yb plugin. Use when the user says yb:commit-push, commit-push, or asks to commit and push current changes while preserving existing Claude skills.
---

# YB Commit Push

Use this workflow for `/commit-push` in the YeoBee Android repo. It is the Codex-native migration of the existing Claude `yb:commit --push` flow; keep the Claude skill files unchanged as the legacy reference.

## Goal

Commit the intended current changes with a clean Korean commit message, run the repository commit hooks normally, and push the current branch.

## Inputs

- Optional `-m "message"`: use this exact commit title, without Codex/Claude boilerplate.
- No explicit message: infer a concise Korean title from the staged and unstaged diffs.

## Workflow

1. Inspect state:
   - `git status --short --branch`
   - `git diff --stat`
   - `git diff --cached --stat`
   - Read relevant diffs before staging.
2. Protect local-only files:
   - Do not stage `.codex/config.toml`, `.claude/settings.local.json`, `.mcp.json`, secrets, tokens, or unrelated local config.
   - Do not automatically stage untracked instruction/workflow files unless this request is specifically about those files.
   - If an untracked file is ambiguous, mention it and leave it unstaged.
3. Stage only the intended files with explicit paths.
4. Commit:
   - Use `git commit -m "{message}"`.
   - Never use `--no-verify`.
   - Do not include Codex/Claude generated-by text or co-author trailers.
   - Prefer `{type}: {Korean summary}` unless the repo hook or user-provided message already supplies a prefix.
5. Let commit hooks finish:
   - Hooks such as `ktlint` and `detekt` may run as part of commit.
   - If hooks fail, stop, report the failing check, and do not push.
6. Push:
   - If the branch already tracks an upstream, use `git push`.
   - If no upstream exists, use `git push -u origin {current-branch}`.
7. Final report:
   - Include commit hash, commit title, pushed branch, verification hook result, and any remaining unstaged/untracked files.

## YeoBee Constraints

- The user normally runs full Android builds manually; do not run a build just for this workflow.
- Existing Claude skill files under `.claude/plugins/yb/` are retained as legacy/reference material.
- Keep commit messages free of Codex/Claude boilerplate.
