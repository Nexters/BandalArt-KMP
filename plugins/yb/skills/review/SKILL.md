---
name: review
description: YeoBee local code review workflow in the yb plugin. Use when the user says yb:review, review, or asks for a local review of current branch changes against CODING.md.
---

# YB Review

Codex-native migration of Claude `yb:review`. Keep `.claude/plugins/yb/skills/review/SKILL.md` unchanged as legacy reference.

## Inputs

- Optional `--base <branch>`. Default: `develop`.

## Workflow

1. Fetch base: `git fetch origin {base}`.
2. Collect changed Kotlin files and diff:
   - `git diff origin/{base}...HEAD --name-only -- '*.kt'`
   - `git diff origin/{base}...HEAD -- '*.kt'`
3. If no Kotlin changes exist, say so and stop.
4. Read review standards:
   - `.claude/CODING.md`
   - `.yeobee/shared/yb/learned-patterns.md` if present
5. Review only added/modified lines in the diff.
   - Do not flag unchanged existing code.
   - Follow CODING.md over general Kotlin preferences.
   - Avoid learned rejected patterns.
6. Report findings first, ordered by severity:
   - Must Fix: convention violation or likely bug
   - Should Fix: safety/performance concern
   - Suggestion: optional readability/structure improvement
7. If no issues are found, say that clearly and note residual test/build gaps.
8. If Must Fix items exist, ask whether to apply fixes before editing.

## Constraints

- Default to code-review stance: findings first, concise summary second.
- Do not run a full build unless the user asks.
