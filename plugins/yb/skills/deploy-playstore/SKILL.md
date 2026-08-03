---
name: deploy-playstore
description: YeoBee Play Store 내부 테스트 release 배포 workflow. 사용자가 deploy-playstore, deploy playstore, yb:deploy-playstore, 내부테스트 배포를 말하거나 release AAB 빌드와 Play Store Internal Testing 업로드를 요청할 때 사용합니다.
---

# YB Play Store 배포

YeoBee release AAB를 로컬에서 빌드하고 Play Store Internal Testing 트랙에 업로드한다.

## 실행 절차

### 1단계: 배포 소스 상태 검증

```bash
git fetch --prune origin develop
git branch --show-current
git rev-list --left-right --count develop...origin/develop
```

- 현재 브랜치가 `develop`인지 확인한다.
- `git rev-list --left-right --count develop...origin/develop` 결과가 `0	0`일 때만 진행한다.
- 로컬 `develop`이 `origin/develop`보다 앞서 있거나, 뒤처져 있거나, diverge 된 경우 즉시 중단한다.
- 중단 시 사용자가 `git pull --ff-only origin develop` 등으로 최신 원격 `develop`과 동일하게 맞춘 뒤 다시 실행하도록 안내한다.

### 2단계: 환경 검증

```bash
ls yeobee.jks
ls keystore.properties
ls playstore/service-account-key.json
```

- keystore가 없으면 안내 후 중단한다.
- Play Store 서비스 계정 키가 없으면 안내 후 중단한다.
- `keystore.properties`, service account JSON 등 민감 파일 내용은 출력하지 않는다.

### 3단계: Play Store 릴리스 노트 확인

```bash
cat app/src/main/play/release-notes/ko-KR/internal.txt
```

- 파일이 없거나 비어 있으면 사용자에게 작성을 요청하고 중단한다.
- 내용이 있으면 표시하고 확인 요청한다.
- Play Store 릴리스 노트는 500자 제한임을 함께 안내한다.

### 4단계: 다음 versionCode 조회

Play Developer API로 모든 트랙의 최대 versionCode를 조회하여 다음 값(+1)을 계산한다.

```bash
uv run scripts/play_next_version_code.py
```

스크립트 출력 예시:

```text
CURRENT_MAX=11
NEXT=12
TRACKS=alpha:0,beta:0,internal:11,production:11
```

- 스크립트 실패 시 에러를 표시하고 중단한다.
- versionCode를 임의로 추측하지 않는다.
- `uv`가 설치되어 있지 않으면 사용자에게 `brew install uv` 안내 후 중단한다.

### 5단계: versionName/versionCode 매핑 검증

`gradle/libs.versions.toml`의 `versionName`을 읽고 아래 규칙으로 기대 versionCode를 계산한다.

```text
major * 10000 + minor * 100 + patch
```

예:

```text
1.0.2 -> 10002
1.1.0 -> 10100
```

- Play Store 조회 결과의 `NEXT`와 versionName 기반 기대 versionCode가 같으면 정상 진행한다.
- `NEXT`와 기대 versionCode가 다르면 일반 배포 경로를 중단하고, 두 값을 사용자에게 명확히 보여준다.
- 불일치가 의도된 경우에만 사용자가 정확한 `versionName`/`NEXT` 조합을 확인한 뒤 진행한다.
- 의도된 불일치 배포는 `BuildConfig.VERSION_CODE`와 Play Store 업로드 versionCode가 같도록 `-PversionCodeOverride={NEXT}`를 함께 사용한다.

### 6단계: 사용자 확인

아래 정보를 표시하고 명시적으로 확인받는다.

```text
현재 Play Store 최대 versionCode: {CURRENT_MAX}
트랙별 최대값: {TRACKS}
현재 versionName: {VERSION_NAME}
versionName 기반 기대 versionCode: {EXPECTED_VERSION_CODE}
배포할 versionCode: {NEXT}

이대로 진행할까요? (Y/n)
```

- 사용자가 `Y`, `y`, 또는 엔터를 입력하면 진행한다.
- 그 외 입력은 중단한다.

### 7단계: 빌드 및 업로드

```bash
./gradlew publishReleaseBundle --no-configuration-cache
```

- `NEXT`와 versionName 기반 기대 versionCode가 같은 일반 배포에서는 위 명령을 사용한다.
- 의도된 불일치 배포에서는 아래 명령을 사용한다.

```bash
./gradlew publishReleaseBundle -PversionCodeOverride={NEXT} --no-configuration-cache
```

- 배포에는 release AAB가 필요하므로 이 workflow에서는 예외적으로 빌드를 실행한다.
- Gradle Play Publisher publish task에서 `resolutionStrategy.set(AUTO)`가 켜져 Play Store의 다음 versionCode를 자동 결정한다.
- configuration cache는 GPP AUTO versionCode 파일 생성/읽기 순서와 충돌할 수 있어 이 task에서 끈다.
- 빌드 또는 업로드 실패 시 에러 로그를 표시하고 중단한다.

### 8단계: 결과 보고

아래 항목을 보고한다.

- 환경: release
- 빌드: release AAB
- versionName: `{VERSION_NAME}`
- versionName 기반 기대 versionCode: `{EXPECTED_VERSION_CODE}`
- versionCode: `{NEXT}`
- 이전 최대 versionCode: `{CURRENT_MAX}`
- 배포 대상: Play Store Internal Testing
- 패키지: `com.yeobee`
- 상태: 완료 또는 실패

## 제약

- `keystore.properties`, service account JSON 등 credentials를 출력하지 않는다.
- 배포 소스 상태 검증, 환경 검증, 릴리스 노트 검증, versionCode 조회, versionName/versionCode 매핑 검증, 빌드, 업로드 중 하나라도 실패하면 즉시 중단한다.
- 사용자가 release/Internal Testing 배포를 요청한 경우에만 실행한다.
- 이 배포 workflow 밖에서 검증 목적의 일반 빌드를 따로 실행하지 않는다.
