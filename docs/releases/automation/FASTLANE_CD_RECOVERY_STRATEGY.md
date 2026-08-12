# Fastlane Android/iOS CD 복구 전략

현재 CD 파일별 책임과 수정 기준은 [Fastlane CD 파일 가이드](FASTLANE_CD_FILE_GUIDE.md)를 따른다.

## 배경

현재 `fastlane/` 설정은 오래된 Android 단일 모듈 구조와 Firebase App Distribution 플러그인을 전제로 하며, 실제 KMP 모듈·Play Internal 배포 절차와 맞지 않는다. 또한 추적 중인 Fastlane 파일에 장기 자격증명이 포함된 이력이 있어 현재 설정을 배포 근거로 재사용할 수 없다.

이번 작업은 GitHub Actions의 수동 실행 한 번으로 Android Play Internal Testing, iOS TestFlight 또는 둘 다를 선택해 배포할 수 있는 최소 CD 경로를 복구한다. Fastlane은 플랫폼별 사전 검증과 배포를 조율하고, Android 업로드 자체는 저장소 표준인 Gradle Play Publisher를 유지한다.

## 목표

- GitHub Actions `workflow_dispatch`에서 `android`, `ios`, `both`를 선택한다.
- 배포 소스는 `main`의 최신 커밋으로 제한한다.
- 상태를 변경하는 Android/iOS lane은 GitHub Actions에서만 실행하고, 로컬에서는 iOS read-only preflight만 허용한다.
- Android는 공식 Google 테스트 Rewarded·Banner 광고 ID가 포함된 AAB만 Play Internal Testing에 올린다.
- iOS는 App Store Connect Individual API Key와 수동 배포 서명 자산으로 TestFlight에 올린다.
- 버전 충돌, 서명 자산 누락, 잘못된 광고 ID 또는 잘못된 브랜치는 업로드 전에 실패시킨다.
- 자격증명은 GitHub Secrets에서 runner 임시 파일로만 복원하고 항상 삭제한다.

## 비범위

- Android Production 배포 또는 Internal artifact의 Production 승격
- iOS App Store 심사 제출·자동 출시
- Fastlane Match 저장소 구축
- Android version 자동 증가나 소스 자동 커밋
- 서명된 AAB·IPA의 GitHub Actions artifact 별도 보관. 스토어 업로드 외 바이너리 보관은 별도 승인 후 추가한다.
- 과거 Git 이력의 비밀 제거. 현재 파일 제거 후 별도 보안 작업으로 이력 정리와 키 교체를 수행한다.

## 배포 구조

`.github/workflows/release-cd.yml`은 `target` 입력을 받아 두 개의 독립 job을 조건부 실행한다.

`workflow_dispatch`에서 target과 배포 확인 checkbox를 직접 선택하는 행위를 배포 skill의 최종 명시적 승인으로 간주한다. lane은 실제 업로드 전에 source SHA, package, version, 전체 Play track 최대값, 3개 locale release notes와 실행 task를 로그에 남긴다. 조직 정책상 2인 승인이 필요하면 `android-internal`과 `ios-testflight` Environment에 required reviewer를 추가한다.

1. Android job (`ubuntu-latest`, `android-internal` environment)
   - `main` 소스와 필수 secret을 검증한다.
   - Android keystore와 Play service account JSON을 runner 임시 위치에 복원한다.
   - Fastlane `android internal`을 실행한다.
   - lane은 전체 Play track의 versionCode를 조회하고 현재 버전이 더 큰지 검증한다.
   - Play API Python 의존성은 exact version과 uv script lock으로 고정하고 `--frozen`으로 실행한다.
   - clean release AAB를 `bandalart.useTestAds=true`로 생성한다.
   - bundletool manifest의 package·version, ZIP 무결성, Compose 리소스 namespace, 테스트/운영 Rewarded·Banner 광고 ID를 검사한다.
   - 같은 속성으로 Gradle Play Publisher의 `publishReleaseBundle`을 실행하고 Internal track 반영을 확인한다.

2. iOS job (`macos-latest`, `ios-testflight` environment)
   - JDK/Android SDK/Gradle/Ruby를 준비한다. Xcode shell phase가 KMP framework를 만들기 위해 필요하다.
   - Apple Distribution `.p12`와 App Store provisioning profile을 runner 임시 위치에 복원하고, Individual App Store Connect `.p8` base64는 Fastlane에 직접 전달한다.
   - Fastlane `ios beta`를 실행한다.
   - lane은 임시 keychain과 profile을 설치하고 `Info.plist`의 version/build를 읽는다.
   - Individual key는 `issuer_id` 없이 `sub=user` 방식으로 Fastlane에 전달한다.
   - 같은 version의 최신 TestFlight build를 조회해 runner checkout의 `CFBundleVersion`만 다음 번호로 올린다.
   - 공유 scheme으로 manual signing archive/IPA를 만들고 TestFlight에 업로드한다.
   - 내부 테스트용 `What to Test`를 함께 등록하고 업로드한 정확한 build 번호의 존재를 확인한다.

두 job은 `release-cd` concurrency group을 공유하고 진행 중 배포를 자동 취소하지 않는다. `both`는 두 플랫폼을 병렬 배포하므로 한 플랫폼 실패가 다른 플랫폼의 이미 시작된 업로드를 롤백하지 않는다.

## GitHub Secrets 계약

### 기존 Android secrets 재사용

- `APP_RELEASE_KEY_STORE_BASE_64`: release JKS의 base64
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `PLAY_SERVICE_ACCOUNT_JSON`: Play Console 서비스 계정 JSON 원문
- `SERVER_BASE_URL`

기존 `STORE_FILE`은 runner가 안전한 임시 경로를 직접 쓰므로 CD에서는 사용하지 않는다.

### 신규 iOS secrets

- `IOS_DISTRIBUTION_CERTIFICATE_BASE64`: Apple Distribution 인증서와 private key가 포함된 `.p12`의 base64
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`: `.p12` 암호
- `IOS_PROVISIONING_PROFILE_BASE64`: App Group이 포함된 `com.nexters.bandalart.iosApp` App Store profile의 base64
- `IOS_WIDGET_PROVISIONING_PROFILE_BASE64`: App Group이 포함된 `com.nexters.bandalart.iosApp.widget` App Store profile의 base64
- `APP_STORE_CONNECT_KEY_ID`: Individual API Key ID
- `APP_STORE_CONNECT_KEY_P8_BASE64`: Individual `.p8`의 base64

App Group, App ID, 두 profile과 GitHub Environment secret 등록 절차는 [iOS WidgetKit MVP 전략](../../features/widgets/IOS_WIDGETKIT_MVP_STRATEGY.md#10-testflight용-app-group과-서명-자산을-등록하는-방법)을 따른다.

현재 보유한 Individual API Key에는 issuer ID를 연결하지 않는다. Team Key를 새로 발급하는 경우에만 별도 설계 변경 후 issuer ID를 사용한다.

## 보안 정리

- 추적 중인 legacy service account JSON, Fastlane report, plaintext Firebase token을 현재 tree에서 제거한다.
- Fastlane Firebase 플러그인과 obsolete lane을 제거한다.
- 민감 파일과 Fastlane 생성물은 `.gitignore`에 추가한다.
- workflow는 `umask 077`로 임시 파일을 만들고 `if: always()` cleanup에서 삭제한다.
- legacy Firebase refresh token과 추적된 service account key는 이미 노출된 것으로 간주해 발급처에서 폐기·교체한다.
- 기존 legacy Firebase refresh token과 추적된 service account key는 발급처에서 폐기 완료했다.
- Git 이력 정리는 강제 push가 필요한 별도 작업으로 분리하고 명시적 승인 없이 수행하지 않는다.

## 버전 관리와 실패 처리

### SemVer와 업데이트 강제 여부

제품 버전은 변경 범위만 표현하고 업데이트 강제 여부를 포함하지 않는다.

- `MAJOR`: 호환되지 않는 데이터·동작 변경이나 제품 단위의 큰 전환
- `MINOR`: 기존 사용자 흐름과 호환되는 기능 추가
- `PATCH`: 버그 수정, 문구·레이아웃 같은 작은 UX 개선과 내부 변경

강제 업데이트를 피하려고 신기능 release를 patch로 낮추는 등 버전 의미를 바꾸지 않는다. 반대로 긴급 patch도 필요하면 강제할 수 있다. Android 강제 여부는 Release CD의 `android_update_priority`만 결정하며 일반 배포는 `0`, 긴급 차단 release만 `4` 또는 `5`를 사용한다. 이미 게시한 Play release의 priority는 바꿀 수 없으므로 잘못 게시했다면 새 versionCode로 다시 배포한다.

### Android

- 단일 source는 `gradle/libs.versions.toml`의 `majorVersion`, `minorVersion`, `patchVersion`이다.
- `versionName`은 `MAJOR.MINOR.PATCH`, `versionCode`는 `(MAJOR * 10000) + (MINOR * 100) + PATCH`로 계산한다. 예를 들어 `2.2.21`은 `20221`이다.
- 계산 충돌을 막기 위해 `MINOR`와 `PATCH`는 각각 `0..99` 범위에서 관리한다.
- 모든 AAB는 Play 전체 track의 기존 최댓값보다 큰 새 versionCode를 사용해야 한다.
- workflow는 버전을 자동 증가시키거나 source를 커밋하지 않는다. 배포 PR에서 버전과 3개 locale release notes를 함께 변경한다.
- Internal Testing의 테스트 광고 AAB를 Production으로 승격하지 않는다. 운영 광고 빌드는 새 versionCode로 별도 생성한다.

### iOS

- marketing version의 단일 source는 `iosApp/iosApp/Info.plist`의 `CFBundleShortVersionString`이다.
- 저장소의 `CFBundleVersion`은 plist의 유효한 기본값일 뿐 CD build 번호의 seed가 아니다. TestFlight 배포 lane은 같은 marketing version의 ASC 최신 build를 조회해 빈 train이면 `1`, 기존 build가 있으면 `최신값 + 1`을 runner checkout에만 적용한다.
- TestFlight build 번호는 배포할 때마다 저장소에 다시 커밋하지 않는다. 동시에 같은 다음 번호를 선택한 업로드가 충돌하면 source를 수정하거나 자동 재시도하지 않고 실패한다.
- Android와 iOS 버전은 현재 자동 동기화되지 않는다. 같은 제품 버전으로 동시 출시하려면 두 source를 PR에서 명시적으로 같은 값으로 맞춘다.

### 표준 배포 순서

1. 변경 범위로 `MAJOR.MINOR.PATCH`를 결정한다. 강제 여부는 이 결정에 영향을 주지 않는다.
2. 실제 version source와 배포 안내를 변경한다.
   - Android Internal: `androidApp/src/main/play/release-notes/{ko-KR,en-US,ja-JP}/internal.txt`
   - iOS TestFlight: `fastlane/metadata/ios/what_to_test.txt`
3. PR CI를 통과시켜 `main`에 merge한다.
4. `.github/workflows/release-cd.yml`의 Release CD에서 target을 선택한다. Android 일반 배포는 priority `0`, 긴급 차단만 `4` 또는 `5`를 선택한다.
5. 업로드 후 Android는 exact versionCode·track·status·priority, iOS는 exact marketing version·build 존재를 스토어 API로 검증한다.

Android versionCode가 이미 사용됐거나 iOS가 선택한 다음 build number가 동시 업로드와 충돌하면 source를 자동 수정하지 않고 배포를 실패시킨다. iOS 서명 profile의 bundle ID, Team ID, 배포 유형 또는 만료가 맞지 않아도 archive 전에 실패한다. 업로드 성공 후 스토어 조회가 일시적으로 늦으면 제한된 횟수만 재확인하고 실패 로그를 남긴다.

## 검증과 완료 조건

- Ruby syntax, workflow YAML, shell 문법, Git diff whitespace 검증이 통과한다.
- PR CI에서 actionlint, Ruby helper tests, Python release tests, Fastlane Android/iOS lane load가 통과한다.
- 공유 iOS scheme과 provisioning/signing override가 정적으로 일치한다.
- 실제 secret을 등록하기 전에는 업로드를 실행하지 않는다.
- GitHub Environments와 secrets를 등록한 뒤 `main`에서 플랫폼별 첫 수동 배포를 각각 수행한다.
- 첫 배포에서 Play Internal versionCode와 TestFlight build가 서버에서 확인되면 CD 복구를 완료한다.
