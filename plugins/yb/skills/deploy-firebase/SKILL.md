---
name: deploy-firebase
description: YeoBee Firebase App Distribution workflow in the yb plugin. Use when the user says yb:deploy-firebase, deploy-firebase, deploy firebase, or asks to build and distribute an APK to Firebase with optional Discord notification.
---

# YB Deploy Firebase

Codex-native migration of Claude `yb:deploy-firebase`. Keep `.claude/plugins/yb/skills/deploy-firebase/` unchanged as legacy reference.

## Inputs

- Required one of `--debug` or `--release`.
- Optional `--groups <group1,group2>`. Default: read `firebase/testers.txt`.
- Optional `--notes-file <path>`. Default: `firebase/release-notes.txt`.
- Optional `--no-discord`. Default: Discord notification enabled.

## Workflow

1. Validate inputs. Stop if neither or both `--debug` and `--release` are present.
2. Validate environment without printing secrets.
   - Read `firebase/.env` conceptually or via shell source.
   - Required app IDs: `FIREBASE_APP_ID_DEBUG` for debug, `FIREBASE_APP_ID_RELEASE` for release.
   - If Discord is enabled, require `DISCORD_WEBHOOK_URL`.
   - Optional: `GOOGLE_APPLICATION_CREDENTIALS`.
3. Prepare release notes.
   - Use provided notes file or `firebase/release-notes.txt`.
   - If missing/empty, draft from commits since `origin/develop` and ask the user before writing.
   - If present, show the content and ask for approval before deploy.
4. Build APK only after user approval.
   - Debug: `./gradlew assembleDebug -PversionCodeOverride="$(date +%s)" --stacktrace`
   - Release: `./gradlew assembleRelease --stacktrace`
   - Expected APKs:
     - debug: `app/build/outputs/apk/debug/app-debug.apk`
     - release: `app/build/outputs/apk/release/app-release-unsigned.apk`
5. Distribute with Firebase CLI:
   - `firebase appdistribution:distribute <APK> --app "${FIREBASE_APP_ID}" --groups "${TESTER_GROUPS}" --release-notes-file "<notes-file>"`
6. If Discord is enabled, notify via `scripts/firebase-deploy.sh notify` when available, or use the existing webhook pattern.
7. Report build type, APK path, Firebase app id name used, tester groups, release notes file, and notification result.

## Constraints

- This workflow intentionally runs a build because deployment requires an APK.
- Never print `.env`, service account JSON, webhook URLs, or credentials.
- Stop immediately on failed environment validation, build, upload, or notification.
- For troubleshooting details, consult `.claude/plugins/yb/skills/deploy-firebase/reference.md`.
