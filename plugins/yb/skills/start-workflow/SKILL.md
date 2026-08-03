---
name: start-workflow
description: YeoBee new work setup workflow in the yb plugin. Use when the user says yb:start-workflow, start-workflow, or asks to create a GitHub issue, worktree, branch, and local IDE/session setup for a new task.
---

# YB Start Workflow

Codex-native migration of Claude `yb:start-workflow`. Keep `.claude/plugins/yb/skills/start-workflow/SKILL.md` unchanged as legacy reference.

## Inputs

- Required `--title` / `-t`: GitHub issue title.
- Optional `--issue` / `-i`: Notion issue id. Numeric values become `YB-{number}`. Default: `YB-00`.
- Optional `--base` / `-b`: base branch. Default: `develop`.
- Optional `--prompt` / `-p`: prompt to prefill in the new agent/session command.

## Workflow

1. Parse inputs. If title is missing, ask for it.
2. Infer label and issue type from title:
   - `feat:` -> `✨ feat`, Feature
   - `fix:` -> `🐛 fix`, Bug
   - `chore:` -> `⚙️ chore`, Task
   - `docs:` -> `📝 docs`, Task
   - `refactor:` -> `♻️ refactor`, Task
   - `test:` -> `🧪 test`, Task
   - `[클로드]` -> `claude`, Task
   - otherwise ask the user
3. Create the GitHub issue:
   - title: `[{YB issue}] {title}`
   - body: title without label keyword
   - assignee: `@me`
   - label: inferred label
4. Set GitHub issue type via `gh api graphql` when the type id is known.
5. Compute main repo root from `git rev-parse --git-common-dir`.
6. Build worktree path:
   - `YB-00` uses `YB-00-{GitHubIssueNumber}` to avoid collisions.
   - explicit YB issue uses that issue id.
   - path: `${MAIN_REPO_ROOT}.worktrees/{dir-name}`
7. Fetch base and create the worktree branch:
   - branch: `{YB issue}/#{GitHubIssueNumber}`
   - `git worktree add -b "{branch}" "{worktree-path}" origin/{base}`
   - `git -C "{worktree-path}" push -u origin "{branch}"`
8. Open Android Studio with `studio "{worktree-path}"` if available and user expects GUI setup.
9. For terminal/session setup, prefer Codex-friendly instructions over Claude-specific automation:
   - If the user asks for the original iTerm2 Claude flow, use the legacy skill as reference.
   - Otherwise report the command to start a new Codex/terminal session in the worktree.
10. Report issue URL, branch, base, worktree path, push result, and IDE/session status.

## Constraints

- Commands that open GUI apps may require user approval.
- Do not overwrite an existing worktree path without explicit user approval.
- Keep branch and issue naming compatible with `.claude/WORKFLOW.md`.
