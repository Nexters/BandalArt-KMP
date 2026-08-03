---
name: resolve-gemini-review
description: YeoBee Gemini Code Assist review resolution workflow in the yb plugin. Use when the user says yb:resolve-gemini-review, resolve-gemini-review, Gemini 리뷰 확인해줘, or asks to address Gemini Code Assist PR comments.
---

# YB Resolve Gemini Review

Codex-native migration of Claude `yb:resolve-gemini-review`. Keep `.claude/plugins/yb/skills/resolve-gemini-review/` unchanged as legacy reference.

## Inputs

- Optional PR number. Default: detect current branch PR with `gh pr view`.
- Optional `--brief`: omit detailed code-change explanation in final summary.

## Workflow

1. Resolve PR number.
   - Use argument if present.
   - Otherwise run `gh pr view --json number,headRefName,baseRefName,url,isDraft`.
   - Stop for missing PR or draft PR.
2. Load learned patterns from `.yeobee/shared/yb/learned-patterns.md` if present.
   - Treat this file as the single shared review memory used by both Claude and Codex workflows.
   - Use repeated accepted/rejected patterns as a default judgment signal.
3. Collect unresolved Gemini Code Assist review threads using GitHub GraphQL.
   - Include unresolved threads whose first comment author is `gemini-code-assist[bot]` or `gemini-code-assist`.
   - Exclude PR summary comments.
   - Exclude already processed threads containing `✅` + `반영완료` or `해당 제안은 검토 결과 현재 반영하지 않았습니다`.
4. Sort comments by risk:
   - security/crash
   - bug/logic
   - quality/readability
   - style/convention
5. For each comment, inspect the referenced file and nearby code.
6. Decide whether to apply or reject.
   - Apply for real crash, security, threading, missing error handling, clear quality, hardcoded UI text, or repeated accepted patterns.
   - Reject for overengineering, excessive scope, intended design, TODO/future work, already handled behavior, client-side public app keys, or repeated rejected patterns.
7. Applying a comment:
   - Edit only the necessary code.
   - Commit one comment at a time with `fix:` or `refactor:` title.
   - Never use `--no-verify`.
   - Push after each successful commit.
   - Reply to the review comment with `✅ <commit-url> 에 반영완료!`.
   - Resolve the review thread with GraphQL.
8. Rejecting a comment:
   - Batch planned rejections and ask the user for approval before posting any rejection reply.
   - Show each planned rejection with file, line, severity, Gemini summary, and concrete rejection reason.
   - After approval, reply with a concise Korean reason and leave an audit trail.
   - If the user asks to apply a planned rejection instead, switch that item to the applying flow.
9. Optionally update the PR body when the applied change affects core behavior.
10. Post or update a cumulative PR comment headed `🤖 Gemini Code Assist 리뷰 반영 결과`.
   - Search issue comments for an existing comment containing that header.
   - If one exists, merge previous rows with this run's rows, update the totals, post the merged comment, then delete the old summary comment.
   - If none exists, post a new summary comment.
   - Include:
     - total unresolved Gemini comments processed
     - accepted count and rejected count
     - accepted table: severity, file, comment summary, change summary, commit link
     - rejected table: severity, file, comment summary, rejection reason
11. Update learned patterns after processing.
   - Update `.yeobee/shared/yb/learned-patterns.md` when this run reveals a reusable accepted or rejected pattern.
   - For each reusable pattern, update count, PR list, and latest date.
   - If a pattern reaches 3 or more occurrences, mention it in the final summary as a "삼진아웃" candidate and propose whether to add it to this skill's apply/reject criteria.
   - Do not silently edit skill criteria just because a count reached 3. Ask the user first unless the user explicitly requested workflow documentation updates.
   - Commit and push learned-pattern changes only if the file changed.
12. Final summary:
   - PR number, total unresolved comments processed, applied count, rejected count.
   - Tables for applied/rejected comments.
   - Include code-change details unless `--brief` was used.
   - Include PR summary comment status.
   - Include learned-patterns update status and any 삼진아웃 candidates.

## Constraints

- Do not run a full build; the user runs builds manually.
- Rejection replies require user approval first.
- Preserve project style and keep code edits minimal.
- Do not include Codex/Claude boilerplate in commits or PR comments.
