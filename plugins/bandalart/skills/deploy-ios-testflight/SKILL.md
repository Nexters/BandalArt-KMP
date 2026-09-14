---
name: deploy-ios-testflight
description: BandalArt iOS 앱을 운영 또는 테스트 광고 모드를 명시해 TestFlight에 배포한다. iOS TestFlight 빌드·재배포 요청에 사용하며 Android와 App Store 심사 제출은 다루지 않는다.
---

# Deploy iOS TestFlight

BandalArt iOS Release archive를 GitHub Actions의 `Release CD` workflow로 빌드하고 TestFlight에 업로드한다.

## 광고 모드 불변식

- 앱 업데이트, 출시, 심사, 릴리스 후보 또는 버전 번호가 지정된 배포는 `ios_ads_mode=production`을 사용한다.
- `TestFlight`, `내부 테스트`, `테스트 배포`라는 표현만으로 `test`를 선택하지 않는다.
- `ios_ads_mode=test`는 사용자가 Google 테스트 광고, 광고 클릭, 보상 완료 등 광고 동작 검증을 명시적으로 요청한 경우에만 사용한다.
- 업로드된 바이너리의 광고 모드는 바꿀 수 없다. 테스트 광고 빌드를 App Store 심사 후보로 사용하지 않는다.

## 절차

### 1. GitHub 계정과 배포 소스 고정

1. `gh auth switch --hostname github.com --user easyhooon`으로 활성 계정을 전환하고 `gh api user --jq .login`이 `easyhooon`인지 확인한다.
2. 배포 source는 `main`으로 고정한다. `git fetch --prune origin main` 후 workflow가 사용할 `origin/main` commit을 기록한다.
3. iOS `CFBundleShortVersionString`과 Xcode target의 `MARKETING_VERSION`이 사용자가 요청한 버전과 일치하는지 확인한다.
4. `main`이 아닌 ref, 미병합 변경 또는 로컬 산출물을 TestFlight 배포에 사용하지 않는다.

### 2. 배포 목적과 광고 모드 검증

1. 사용자의 요청을 `출시 후보` 또는 `광고 동작 테스트` 중 하나로 분류한다.
2. 출시 후보라면 광고 모드는 반드시 `production`이다. 광고 동작 테스트가 명시되지 않았는데 `test`를 선택하려 하면 중단한다.
3. `plugins/bandalart/skills/write-release-notes/SKILL.md`를 읽고 TestFlight 테스트 안내가 실제 변경과 선택한 광고 모드에 맞는지 확인한다.
4. 실행 직전에 다음 값을 사용자에게 표시하고 명시적 승인을 받는다.
   - source commit
   - iOS marketing version
   - 예상 다음 build number
   - 배포 대상 `TestFlight`
   - `ios_ads_mode`와 그 선택 이유
   - TestFlight 테스트 안내

### 3. iOS 전용 workflow 실행

승인된 값을 그대로 전달해 `main`에서 `Release CD`를 실행한다. iOS만 재배포할 때는 다음 입력을 사용한다.

```bash
gh workflow run release-cd.yml \
  --repo Nexters/BandalArt-KMP \
  --ref main \
  -f target=ios \
  -f confirm_release=true \
  -f ios_ads_mode=production \
  -f android_update_priority=0
```

광고 동작 테스트가 명시적으로 승인된 경우에만 `ios_ads_mode=test`로 바꾼다. `target=both`로 넓히거나 Android를 다시 배포하지 않는다.

### 4. 실행 입력과 업로드 결과 검증

1. 새 run ID와 source SHA가 방금 승인한 값인지 확인한다.
2. guard job 로그에서 `RELEASE_REF=refs/heads/main`, `RELEASE_CONFIRMED=true`, `IOS_ADS_MODE=<승인값>`을 확인한다.
3. guard 로그의 광고 모드가 승인값과 다르면 즉시 workflow를 취소하고 새 빌드를 기다리지 않는다.
4. 성공할 때까지 run을 감시한다. 실패하면 secret을 가린 실패 단계와 원인을 보고하고 임의 재실행하지 않는다.
5. 성공 후 workflow summary와 TestFlight 검증 로그에서 source SHA, 버전, 실제 build number, `Ads: <승인값>`, exact build 확인 결과를 검증한다.
6. 출시 후보는 `Ads: production`이 확인되어야 완료로 보고한다. 운영 광고 빌드에서는 광고를 직접 클릭하거나 반복 노출하지 않도록 안내한다.

## 제약

- iOS archive와 TestFlight 업로드 외 배포를 실행하지 않는다.
- App Store 버전 생성, 심사 제출 또는 공개 출시는 별도 명시적 요청 없이 실행하지 않는다.
- 인증서, provisioning profile, App Store Connect key 또는 secret 값을 출력하거나 Git에 추가하지 않는다.
- 예상 build number를 고정 입력하지 않는다. workflow가 App Store Connect의 해당 marketing version 최신 build를 조회해 다음 번호를 선택하게 한다.
- 사용자의 업로드 승인 없이 실제 workflow를 실행하지 않는다.
