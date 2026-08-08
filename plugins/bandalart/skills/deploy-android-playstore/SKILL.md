---
name: deploy-android-playstore
description: BandalArt Android release AAB를 Play Store Internal Testing에 배포한다. deploy-android-playstore 또는 Android 내부 테스트 배포 요청에 사용하며 iOS 배포는 다루지 않는다.
---

# Deploy Android Play Store

BandalArt Android release AAB를 Google Play Internal Testing에 업로드한다. 이 skill의 범위는 Android Play Store 배포뿐이며 iOS, TestFlight, App Store와 공통 KMP release는 포함하지 않는다.

## 절차

### 1. 배포 소스 고정

1. 사용자가 지정한 release source branch를 우선한다. 없으면 현재 release 계획과 저장소 기본 branch에서 추론하며 최종 fallback은 `main`이다.
2. `git fetch --prune origin <branch>` 후 로컬과 `origin/<branch>`의 ahead/behind가 모두 0인지 확인한다.
3. dirty, unpushed, diverged 상태면 중단한다. 배포 과정에서 branch를 임의로 merge/rebase하지 않는다.

### 2. Android Play 배포 설정 검증

다음 항목의 실제 존재와 Git ignore 상태를 확인한다.

- Android application module과 release signing 설정
- `keystore.properties`와 그 설정이 가리키는 release keystore
- `playstore/service-account-key.json` 또는 저장소가 명시한 동등한 service account credential
- Play Developer API에서 최대 versionCode를 조회하는 저장소 script
- Android Play release notes 파일
- Gradle Play Publisher 등 저장소가 채택한 Android AAB upload task

service account는 `client_email`만 기대 계정과 일치하는지 검사하고 credential 전체를 출력하지 않는다. 한 항목이라도 없으면 필요한 설정과 예상 책임만 보고하고 중단한다. 실패했던 Fastlane 설정이나 수동 API 구현을 fallback으로 사용하지 않는다.

### 3. release notes와 버전 검증

1. 실제 Android Play upload 설정이 참조하는 Internal track의 한국어 release notes를 찾는다. legacy module 경로를 추측하지 않는다.
2. 파일이 없거나 비어 있으면 초안을 제안하고 사용자 확인 전에는 쓰거나 배포하지 않는다. Play release notes의 길이 제한도 확인한다.
3. 저장소 script로 Play Developer API의 모든 track 최대 versionCode와 다음 사용 가능한 값을 조회한다.
4. Android application module에서 versionName/versionCode를 읽고 AAB에 들어갈 값과 일치하는지 확인한다.
5. 현재 versionCode가 이미 사용된 최대값 이하면 build와 upload를 실행하지 않는다.

### 4. 최종 확인

실제 업로드 직전에 아래 내용을 표시하고 `Y` 또는 명시적 승인 응답을 받는다.

- release source commit
- package name
- versionName/versionCode
- Play 전체 track의 기존 최대 versionCode
- 배포 대상 `Internal Testing`
- release notes
- 실행할 Android clean bundle 및 upload task

### 5. Android clean AAB 검증과 업로드

1. `./gradlew clean :androidApp:bundleRelease -Pbandalart.useTestAds=true --no-configuration-cache`로 기존 생성물을 제거하고 공식 Google 테스트 광고 ID가 포함된 release AAB를 먼저 생성한다.
2. 생성된 AAB의 versionName/versionCode, 크기와 Compose resource namespace를 검사한다.
   - 제거된 source set의 generated resource namespace가 남아 있으면 업로드하지 않는다.
   - 저장소가 사용하는 resource namespace가 없으면 업로드하지 않는다.
   - 공식 Google Rewarded 테스트 광고 ID가 포함됐는지 확인하고 production 광고 ID가 포함됐으면 업로드하지 않는다.
3. 사전 검증이 통과하면 `./gradlew publishReleaseBundle -Pbandalart.useTestAds=true --no-configuration-cache`로 검증된 소스와 산출물 상태에서 업로드한다.
4. 배포 산출물 생성이 목적이므로 이 workflow에서는 Android release build를 실행할 수 있다.
5. upload target이 Internal track이고 의도한 release status인지 task 설정과 결과에서 확인한다.
6. 실패하면 즉시 중단하고 secret을 가린 실패 단계와 원인을 보고한다.
7. 성공하면 AAB, package, versionName/versionCode, track과 결과를 보고한다.
8. 테스트 광고 ID가 포함된 Internal artifact는 production으로 promote하지 않는다. production 배포는 실제 광고 ID로 새 AAB와 새 versionCode를 생성한다.

## 제약

- Android AAB와 Google Play 외 배포를 실행하지 않는다.
- credential, signing password, keystore 내용 또는 service account JSON을 출력하거나 Git에 추가하지 않는다.
- versionCode를 추측하거나 자동 override하지 않는다.
- 사용자가 Android Internal Testing 배포를 요청하지 않은 상태에서 실제 upload를 실행하지 않는다.
- 일반 검증 목적의 별도 release build를 이 workflow에 덧붙이지 않는다.
