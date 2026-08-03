---
name: commit
description: YeoBee Android commit workflow in the yb plugin. Use when the user says yb:commit, commit, or asks Codex to analyze current changes and create a commit without pushing unless requested.
---

# YB Commit

Codex-native migration of Claude `yb:commit`. Keep `.claude/plugins/yb/skills/commit/SKILL.md` unchanged as legacy reference.

## Inputs

- Optional `--push`: push after a successful commit.
- Optional `-m "message"`: use this exact commit title.
- No message: infer a concise Korean title from the diff.

## Workflow

1. Inspect `git status --short --branch`, staged/unstaged stats, and relevant diffs.
2. Stage only intended files with explicit paths.
   - Do not stage local secrets/config such as `.codex/config.toml`, `.claude/settings.local.json`, `.mcp.json`, `firebase/.env`, service account keys, or unrelated local state.
   - Leave ambiguous untracked files unstaged and mention them.
3. Commit with `git commit -m "{message}"`.
   - Never use `--no-verify`.
   - Never add Codex/Claude generated-by text or co-author trailers.
   - Prefer `{type}: {Korean summary}`.
4. If `--push` was requested, push after hooks pass:
   - tracking branch exists: `git push`
   - otherwise: `git push -u origin {current-branch}`
5. Report commit hash/title, hook result, push result if any, and remaining unstaged/untracked files.

## Constraints

- Do not run a full Android build for this workflow unless the user explicitly asks.
- If commit hooks fail, stop and report the failing check.
