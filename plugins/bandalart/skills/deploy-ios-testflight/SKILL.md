---
name: deploy-ios-testflight
description: BandalArt iOS 앱을 GitHub Actions Release CD(target=ios)로 TestFlight에 업로드한다. deploy-ios-testflight, iOS TestFlight 배포, iOS만 테스트 배포 요청에 사용한다. App Store 심사 제출, App Store Connect version 생성·출시, Android 배포, target=both 동시 배포와 Xcode Organizer 수동 업로드에는 사용하지 않는다.
---

# Deploy iOS TestFlight

BandalArt iOS 앱을 TestFlight 내부 테스트에 업로드한다. 실제 archive와 업로드는 `.github/workflows/release-cd.yml`의 `TestFlight` job이 `bundle exec fastlane ios beta`로 수행하며, 이 skill은 그 workflow를 `target=ios`로 실행하기 전후의 점검·승인·결과 확인을 맡는다. 범위는 iOS TestFlight 업로드뿐이며 App Store 심사 제출, Android Play Internal Testing과 공통 KMP release는 포함하지 않는다.

배포 구조와 secret 계약의 근거는 [FASTLANE_CD_RECOVERY_STRATEGY.md](../../../../docs/releases/automation/FASTLANE_CD_RECOVERY_STRATEGY.md)와 [FASTLANE_CD_FILE_GUIDE.md](../../../../docs/releases/automation/FASTLANE_CD_FILE_GUIDE.md)다.

## 배포 경로 요약

- 진입점: `Nexters/BandalArt-KMP`의 `Release CD` workflow (`workflow_dispatch`, `main` 전용)
- 빌드 source: 실행 시점의 `origin/main` 최신 commit. ios job의 `Require latest main` step과 lane의 `verify_ci_source!`(archive 전·업로드 직전 두 번)가 실행 commit이 `origin/main`과 같은지 다시 검사한다.
- marketing version: `iosApp/iosApp/Info.plist`의 `CFBundleShortVersionString`
- build number: lane이 App Store Connect에서 같은 marketing version의 최신 build를 조회해 `+1`을 runner checkout에만 적용한다. 저장소에 커밋하지 않는다.
- What to Test: `fastlane/metadata/ios/what_to_test.txt` 뒤에 `what_to_test_<ads_mode>_ads.txt`를 붙여 등록한다.
- 광고 모드: `ios_ads_mode=production`은 App Store 심사 후보, `test`는 `BANDALART_TEST_ADS` compile condition으로 Google 테스트 광고를 쓰는 QA 빌드다.
- 업로드 후 lane이 정확한 `version (build)`의 존재를 App Store Connect API로 확인하고 `TestFlight upload verified: <version> (<build>)`를 남긴다. `distribute_external: false`이므로 외부 테스터 배포는 하지 않는다.

## 절차

### 1. GitHub 계정 고정

1. `gh auth switch --hostname github.com --user easyhooon`을 실행한다.
2. `gh api user --jq .login` 결과가 `easyhooon`인지 확인한다.
3. 전환이나 검증에 실패하면 중단한다. 인증 token은 출력하지 않는다.

통과 조건: 활성 계정이 `easyhooon`이다.

### 2. 자동 사전 점검 실행

저장소 루트에서 실행한다.

```bash
plugins/bandalart/skills/deploy-ios-testflight/scripts/preflight.sh
```

script는 원격 상태를 바꾸지 않고 다음을 확인한다.

- 활성 계정과 `origin` remote
- `origin/main` 최신 commit, 그 commit의 marketing version과 세 What to Test 파일
- workflow 입력 `target`, `confirm_release`, `ios_ads_mode`, `android_update_priority` 존재
- `ios-testflight` Environment secret 6개와 repository secret `BANDALART_SUPABASE_URL`, `BANDALART_SUPABASE_PUBLISHABLE_KEY`의 이름 존재. 값은 읽지 않는다.
- `release-cd.yml`과 `play-store-screenshots.yml`에 진행 중 run이 없는지. 두 workflow는 같은 `release-cd` concurrency group을 쓴다.
- `origin/main` HEAD의 check-run 결과와 직전 성공 TestFlight 이후 들어온 commit 목록

통과 조건: `RESULT: PASS`. `FAIL` 항목이 하나라도 있으면 해당 항목과 필요한 조치만 보고하고 중단한다.

이 단계를 건너뛰려는 핑계와 틀린 이유:

- "지난번에 성공했으니 secret과 workflow는 그대로다." → secret은 Environment에서 언제든 교체·삭제될 수 있고, 실패는 macOS runner에서 수십 분을 쓴 뒤에야 드러난다. 이름 확인은 몇 초면 끝난다.
- "로컬 `main`을 봤으니 source는 안다." → workflow는 로컬이 아니라 실행 시점의 `origin/main`을 빌드한다. 다른 세션이 공유 폴더에서 작업 중일 수 있으므로 로컬 working tree는 근거가 아니다.

### 3. 배포 대상 변경 확인

1. preflight의 "직전 TestFlight 이후 commit" 목록을 사용자에게 보여준다. 사용자가 기대한 PR(예: 이번 release 대상 기능)이 목록에 없으면 아직 `main`에 병합되지 않은 것이므로 중단한다.
2. `NO_NEW_COMMITS`가 나오면 이미 같은 source가 TestFlight에 있다. 광고 모드 변경 등 재업로드 이유를 사용자가 명시한 경우에만 계속한다.
3. preflight가 `NO_PREVIOUS_TESTFLIGHT`를 출력하면 commit 목록이 없는 것이므로 마지막 iOS 배포 이후 변경을 `git log`로 직접 확인해 보여준다.
4. preflight가 `CHECKS_NOT_GREEN`을 출력하면 HEAD를 만든 병합 PR의 CI 결과를 `gh pr checks <number> -R Nexters/BandalArt-KMP`로 확인한다. 실패한 required check가 있으면 중단한다.

통과 조건: 배포할 변경이 `origin/main`에 있고 CI가 실패 상태가 아니다.

### 4. 버전 확인

1. preflight가 보고한 marketing version을 사용자에게 보여준다.
2. 그 version이 App Store Connect에서 이미 출시됐거나 심사 승인된 version이면 새 build를 받을 수 없다. 이 상태는 저장소에서 확인할 수 없으므로 사용자에게 App Store Connect 상태를 확인받는다.
3. version을 올려야 하면 업로드하지 않고 `Info.plist`의 `CFBundleShortVersionString`과 What to Test를 함께 바꾸는 배포 준비 PR이 필요하다고 보고한다. PR은 사용자가 요청한 경우 `create-pr` skill로 만들고, 병합된 뒤 2단계부터 다시 시작한다.

NEVER: build number를 직접 정하거나 `Info.plist`의 `CFBundleVersion`을 올리는 PR을 만들지 않는다. 이유: lane이 App Store Connect 최신 build `+1`을 runner에서 계산하며, 저장소 값은 seed로 쓰이지 않는다. 직접 정하면 동시 업로드나 기존 build와 충돌해도 원인이 가려진다.

통과 조건: 사용자가 이 marketing version으로 새 build를 올리는 것을 확인했다.

### 5. What to Test 확인

1. preflight가 출력한 `what_to_test.txt` 내용과 마지막 변경 commit을 보여준다.
2. 내용이 이번 배포의 변경(3단계 commit 목록)을 설명하지 않으면 업로드하지 않는다. 최근 변경을 바탕으로 초안을 제안하고, 사용자 확인 뒤 배포 준비 PR로 `main`에 병합한 다음 2단계부터 다시 시작한다.
3. 선택할 광고 모드의 `what_to_test_<mode>_ads.txt`가 함께 붙는다는 점을 표시한다.

이 단계를 건너뛰려는 핑계와 틀린 이유:

- "테스터가 알아서 확인한다." → TestFlight 안내는 업로드 후 수정할 수 있지만 lane이 등록한 문구가 그대로 테스터에게 먼저 보인다. 지난 release 문구가 남으면 엉뚱한 기능을 검증하게 된다.
- "로컬에서 고치면 된다." → workflow는 `origin/main`의 파일을 읽는다. 로컬 수정은 업로드에 반영되지 않는다.

통과 조건: `origin/main`의 What to Test가 이번 변경을 설명한다.

### 6. 광고 모드 선택

사용자에게 매번 `production` 또는 `test`를 묻고 답을 받는다.

- `production`: App Store 심사 후보. 테스터는 광고 배치만 보고 클릭하지 않는다.
- `test`: 광고 상호작용 QA용. 이 build는 심사에 제출하지 않는다.

ALWAYS: 광고 모드는 사용자 답으로만 정한다. 이유: workflow 기본값이 `production`이라 입력을 빼면 의도와 다른 광고 SDK 설정으로 빌드되고, `test` build를 심사에 올리거나 `production` build에서 실제 광고를 반복 클릭하면 계정 정책 문제가 생긴다.

통과 조건: 사용자가 광고 모드를 명시했다.

### 7. 최종 확인

실제 실행 직전에 아래 내용을 표시하고 `Y` 또는 명시적 승인 응답을 받는다.

- source: `origin/main` commit SHA와 제목
- bundle ID `com.nexters.bandalart.iosApp`, marketing version, build number는 lane이 ASC 최신값 `+1`로 결정
- 배포 대상 `TestFlight` (내부 테스트, 외부 배포 없음)
- 광고 모드
- What to Test 본문과 붙을 광고 안내
- 실행할 명령 (8단계)

NEVER: 채팅 승인 없이 8단계를 실행하지 않는다. 이유: workflow의 `confirm_release` checkbox는 사람이 직접 체크하라고 둔 gate인데, CLI에서 `-f confirm_release=true`를 넘기면 이 채팅 승인이 유일한 사람 확인으로 남는다.

통과 조건: 사용자의 명시적 승인. 이전 배포나 다른 플랫폼 배포에 대한 승인은 이번 실행의 승인이 아니다.

### 8. workflow 실행

1. 실행 직전에 `origin/main`을 다시 fetch해 7단계에서 보여준 SHA와 같은지 확인한다. 다르면 새 commit을 보여주고 7단계부터 다시 승인받는다.
2. 아래 명령을 그대로 실행한다.

```bash
gh workflow run release-cd.yml -R Nexters/BandalArt-KMP --ref main -f target=ios -f confirm_release=true -f ios_ads_mode=<production|test> -f android_update_priority=0
```

이 명령에서 빠뜨리면 안 되는 부분과 이유:

- `-f target=ios`: 기본값이 `both`라 생략하면 Android Play Internal Testing까지 배포된다.
- `-R Nexters/BandalArt-KMP`: 이 checkout에는 개인 fork를 가리키는 `fork` remote가 있어 `gh`가 다른 저장소를 고를 수 있다.
- `-f ios_ads_mode`: 6단계 이유와 같다.
- `-f android_update_priority=0`: 필수 입력이지만 `target=ios`에서는 사용되지 않는다. guard job의 형식 검사만 통과시킨다.

3. `gh run list --workflow release-cd.yml -R Nexters/BandalArt-KMP --event workflow_dispatch --limit 3 --json databaseId,createdAt,headSha,url`로 방금 생성된 run을 찾는다. `createdAt`이 실행 시각 이후이고 `headSha`가 승인한 SHA인 run만 대상이다.
4. run URL을 사용자에게 알린다.

통과 조건: `headSha`가 승인한 SHA와 같은 run ID를 확보했다.

1번 SHA 재확인을 건너뛰려는 핑계와 틀린 이유:

- "방금 7단계에서 확인했다." → 승인을 기다리는 사이 다른 PR이 `main`에 병합될 수 있다. workflow는 실행 시점의 `origin/main`을 빌드하므로 사용자가 승인하지 않은 commit이 올라간다.

### 9. 결과 감시와 검증

1. `gh run watch <run-id> -R Nexters/BandalArt-KMP --exit-status`를 background로 실행한다. job timeout은 90분이다. 짧은 간격으로 반복 polling하지 않는다.
2. 완료되면 아래 script로 검증한다.

```bash
plugins/bandalart/skills/deploy-ios-testflight/scripts/verify_run.sh <run-id> <production|test> <승인한-SHA>
```

3. 통과 조건: exit 0. script는 TestFlight job success, `TestFlight upload verified: <version> (<build>)` 로그, 승인한 광고 모드, 승인한 SHA, `Play Internal Testing` job skipped를 모두 만족할 때만 exit 0을 낸다.
4. 실패하면 `gh run view <run-id> -R Nexters/BandalArt-KMP --log-failed`에서 실패 step과 원인만 요약한다. secret 값, 인증서·profile 내용, token은 인용하지 않는다.
5. 실패 후 자동으로 다시 실행하지 않는다. 이유: 원인이 secret 만료, profile 불일치, build number 충돌처럼 재실행으로 풀리지 않는 경우가 많고, 매 실행이 macOS runner 시간을 수십 분 쓴다. 원인과 조치를 보고하고 사용자 판단을 받는다.

2번 검증을 건너뛰려는 핑계와 틀린 이유:

- "`gh run watch`가 exit 0이었으니 끝났다." → run 성공은 job 결론만 말한다. 정확한 `version (build)`가 App Store Connect에 있는지, 광고 모드와 source SHA가 승인값과 같은지, Android job이 실행되지 않았는지는 별도로 확인해야 보고할 수 있다.

### 10. 보고

- run URL, source SHA, `version (build)`, 광고 모드
- 외부 테스터 배포와 App Store 심사 제출은 하지 않았다는 점
- `production` build면 테스트 중 실제 광고를 클릭하지 않도록 안내한다. `test` build면 심사 제출 대상이 아니라고 안내한다.

통과 조건: 위 항목을 모두 보고했다.

## 제약

- App Store Connect version 생성, 심사 제출, 출시와 외부 TestFlight 그룹 배포는 실행하지 않는다. 이 skill이 끝난 뒤 사용자가 별도로 요청해야 한다.
- `target=both`나 `target=android`로 실행하지 않는다. Android 배포는 `deploy-android-playstore`를 따른다.
- 로컬에서 `bundle exec fastlane ios beta`, Xcode archive 또는 Organizer 업로드로 우회하지 않는다. 이유: `ios beta`는 `CI=true`와 `GITHUB_SHA == origin/main`을 요구해 로컬에서 실패하도록 설계됐고, 우회 업로드는 source·버전·광고 모드 검증을 건너뛴다.
- credential 파일과 값(`.p12`, `.mobileprovision`, `.p8`, 인증서 암호, Supabase 값)을 출력하거나 Git에 추가하지 않는다. secret은 이름만 확인한다.
- 사용자가 TestFlight 배포를 요청하지 않은 상태에서 8단계를 실행하지 않는다. 이유: 업로드된 build는 삭제할 수 없고 build 번호를 소모하며, 내부 테스터에게 바로 노출된다.
- 이 skill 실행 중에는 요청받지 않은 파일 수정, 정리, rename을 하지 않는다. 배포 준비 PR이 필요하면 무엇을 바꿀지 보고하고 사용자 요청을 받는다.

## 관리 정보

- owner: easyhooon
- 검토일: 2026-11-12
- 다음 검토: `release-cd.yml`의 iOS 입력·secret 계약 또는 `fastlane/Fastfile`의 `ios beta` lane이 바뀔 때

### 변경 이력

| 날짜 | 변경 |
| --- | --- |
| 2026-11-12 | 최초 작성. Release CD `target=ios` 경로, preflight·verify script 추가. preflight는 실제 저장소에서 PASS, verify는 실패 run `38582292448`(exit 1)과 성공 run `42479169122`로 확인. `42479169122`는 `target=both` run이라 Android job 검사에서 exit 1이 기대값이다. `target=ios` 성공 경로와 실제 workflow 실행은 아직 검증하지 않음 |
