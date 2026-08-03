---
name: push
description: YeoBee Android push workflow in the yb plugin. Use when the user says yb:push, push, or asks to push the current branch.
---

# YB Push

Push the current branch while preserving local working-tree changes.

## Workflow

1. Inspect `git status --short --branch` and `git branch --show-current`.
2. If the branch tracks an upstream, run `git push`.
3. If no upstream exists, run `git push -u origin {current-branch}`.
4. Report the branch, remote destination, and any remaining local changes.

## Constraints

- Do not stage or commit anything.
- Do not push if the current branch name is empty or detached; explain the blocker.
