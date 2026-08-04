---
name: deploy-playstore
description: Bandalart Play Store 내부 테스트 release 배포 workflow. 사용자가 deploy-playstore, deploy playstore, 내부 테스트 배포를 말하거나 release AAB 빌드와 Play Store Internal Testing 업로드를 요청할 때 사용합니다.
---

# Bandalart Play Store 배포

Bandalart release AAB를 로컬에서 빌드하고 Play Store Internal Testing 트랙에 업로드한다. Fastlane 설정은 사용하거나 참조하지 않는다.

## 실행 절차

### 1단계: 배포 소스 상태 검증

```bash
git fetch --prune origin develop
git branch --show-current
git rev-list --left-right --count develop...origin/develop
```

- 현재 브랜치가 `develop`인지 확인한다.
- `git rev-list --left-right --count develop...origin/develop` 결과가 `0\t0`일 때만 진행한다.
- 로컬 `develop`이 `origin/develop`보다 앞서 있거나, 뒤처져 있거나, diverge 된 경우 즉시 중단한다.
- 중단 시 사용자가 `git pull --ff-only origin develop` 등으로 최신 원격 `develop`과 동일하게 맞춘 뒤 다시 실행하도록 안내한다.

### 2단계: 환경 검증

```bash
test -f keystore.properties
test -f playstore/service-account-key.json
jq -e '.client_email == "bandalart-play-publisher@bandalart-396909.iam.gserviceaccount.com"' playstore/service-account-key.json >/dev/null
```

- `keystore.properties`가 없으면 안내 후 중단한다. release keystore 파일명은 하드코딩하지 않고 Gradle 서명 설정에 위임한다.
- Play Store 서비스 계정 키가 없거나 예상한 Bandalart 서비스 계정이 아니면 안내 후 중단한다.
- `keystore.properties`, service account JSON 등 민감 파일의 내용은 출력하지 않는다.
- 두 파일과 release keystore가 Git에서 ignored 상태인지 확인한다.

### 3단계: Play Store 릴리스 노트 확인

```bash
cat app/src/main/play/release-notes/ko-KR/internal.txt
```

- 파일이 없거나 비어 있으면 사용자에게 작성을 요청하고 중단한다.
- 내용이 있으면 표시하고 확인 요청한다.
- Play Store 릴리스 노트는 500자 제한임을 함께 안내한다.

### 4단계: Play Store versionCode 조회

Play Developer API로 모든 트랙의 최대 versionCode와 다음 사용 가능한 최소값을 조회한다.

```bash
uv run scripts/play_next_version_code.py
```

스크립트 출력 예시:

```text
CURRENT_MAX=10004
NEXT=10005
TRACKS=alpha:0,beta:0,internal:10004,production:10004
```

- 스크립트 실패 시 에러를 표시하고 중단한다.
- versionCode를 임의로 추측하지 않는다.
- `uv`가 설치되어 있지 않으면 사용자에게 `brew install uv`를 안내하고 중단한다.

### 5단계: versionName/versionCode 검증

`gradle/libs.versions.toml`의 버전 값을 읽고 배포 후보를 계산한다.

```text
versionName = major.minor.patch
versionCode = major * 10000 + minor * 100 + patch
```

예:

```text
2.2.5 -> 20205
```

- 계산한 versionCode가 `CURRENT_MAX`보다 클 때만 진행한다.
- 계산한 versionCode는 `NEXT`와 정확히 같을 필요가 없다. Play Store 기존 최대값보다 크면 유효하다.
- 계산한 versionCode가 `CURRENT_MAX` 이하이면 배포를 중단하고 앱 버전을 먼저 올리도록 안내한다.
- `versionCodeOverride` 또는 Gradle Play Publisher의 자동 versionCode 변경을 사용하지 않는다. `BuildConfig.VERSION_CODE`, AAB, Play Store versionCode를 동일하게 유지한다.

### 6단계: 사용자 확인

아래 정보를 표시하고 명시적으로 확인받는다.

```text
현재 Play Store 최대 versionCode: {CURRENT_MAX}
트랙별 최대값: {TRACKS}
현재 versionName: {VERSION_NAME}
배포할 versionCode: {VERSION_CODE}
다음 사용 가능한 최소 versionCode: {NEXT}
배포 대상: Play Store Internal Testing
패키지: com.nexters.bandalart

이대로 진행할까요? (Y/n)
```

- 사용자가 `Y`, `y`, 또는 엔터를 입력하면 진행한다.
- 그 외 입력은 중단한다.

### 7단계: 빌드 및 업로드

```bash
./gradlew publishReleaseBundle --no-configuration-cache
```

- 배포에는 release AAB가 필요하므로 이 workflow에서는 예외적으로 빌드를 실행한다.
- 업로드 대상은 `internal`, 릴리스 상태는 `COMPLETED`이다.
- configuration cache는 배포 task에서 사용하지 않는다.
- 빌드 또는 업로드 실패 시 에러 로그를 표시하고 중단한다.

### 8단계: 결과 보고

아래 항목을 보고한다.

- 환경: release
- 빌드: release AAB
- versionName: `{VERSION_NAME}`
- versionCode: `{VERSION_CODE}`
- 이전 최대 versionCode: `{CURRENT_MAX}`
- 배포 대상: Play Store Internal Testing
- 패키지: `com.nexters.bandalart`
- 상태: 완료 또는 실패

## 제약

- `keystore.properties`, release keystore, service account JSON 등 credentials를 출력하지 않는다.
- Fastlane 설정과 Fastlane credentials를 사용하거나 참조하지 않는다.
- 배포 소스 상태, 환경, 릴리스 노트, versionCode, 빌드, 업로드 중 하나라도 실패하면 즉시 중단한다.
- 사용자가 release/Internal Testing 배포를 요청한 경우에만 실제 업로드를 실행한다.
- 이 배포 workflow 밖에서 검증 목적의 일반 release 빌드를 따로 실행하지 않는다.
