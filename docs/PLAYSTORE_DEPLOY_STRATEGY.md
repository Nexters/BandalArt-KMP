# Bandalart Play Store 내부 테스트 배포 전략

## 1. 목표

- Bandalart Android release AAB를 로컬에서 빌드해 Google Play 내부 테스트 트랙에 업로드한다.
- 복사된 YeoBee 배포 스킬과 설정을 Bandalart 저장소 구조 및 앱 ID에 맞춘다.
- 실패했던 Fastlane 설정은 사용하거나 참조하지 않는다.

## 2. 기준 정보

- Play Store 앱 ID: `com.nexters.bandalart`
- 배포 트랙: `internal`
- 배포 형식: Android App Bundle (`AAB`)
- 서비스 계정 키: 로컬 `playstore/service-account-key.json`
- 서명 설정: 로컬 `keystore.properties`가 가리키는 release keystore
- 버전 규칙: `major * 10000 + minor * 100 + patch`
- 현재 배포 후보: `2.2.2 (20202)`

## 3. 구현 범위

### 3.1 Gradle Play Publisher

- Bandalart의 Gradle 8.11.1과 호환되는 Gradle Play Publisher 3.13.0 플러그인을 버전 카탈로그에 추가한다. Gradle 9.1 이상이 필요한 4.0.0은 사용하지 않는다.
- 루트 프로젝트에는 `apply false`, `app` 모듈에는 실제 플러그인을 적용한다.
- 서비스 계정 키가 로컬에 있을 때만 credentials 파일을 Gradle에 연결한다.
- 기본 트랙은 `internal`, 릴리스 상태는 `COMPLETED`, 기본 산출물은 App Bundle로 설정한다.
- 업로드 명령은 `publishReleaseBundle --no-configuration-cache`로 고정한다.

### 3.2 배포 전 versionCode 검증

- Play Developer API에서 모든 트랙의 최대 versionCode를 조회한다.
- 코드에 설정된 versionCode가 Play Store 최대값보다 큰 경우에만 배포를 허용한다.
- 기존 최대값과 충돌할 때 versionCode를 자동으로 바꾸지 않고, 코드의 앱 버전을 먼저 올리도록 중단한다.
- 이 원칙으로 앱 내부 `BuildConfig.VERSION_CODE`, 생성된 AAB, Play Store versionCode를 동일하게 유지한다.

### 3.3 릴리스 노트

- `app/src/main/play/release-notes/ko-KR/internal.txt`에 500자 이하의 내부 테스트 릴리스 노트를 둔다.
- 실제 업로드 직전 사용자에게 내용을 표시하고 확인받는다.

### 3.4 배포 스킬

- YeoBee 명칭, `com.yeobee`, `yeobee.jks` 참조를 제거한다.
- release keystore 파일명을 하드코딩하지 않고 `keystore.properties` 존재 여부와 Gradle release 서명 설정으로 검증한다.
- `develop`이 원격과 정확히 일치할 때만 배포한다.
- 자격 증명, keystore 속성 및 서비스 계정 JSON 내용은 출력하지 않는다.

## 4. 보안 원칙

- `playstore/service-account-key.json`, `keystore.properties`, release keystore는 Git에 추가하지 않는다.
- 서비스 계정 JSON 원문이나 private key를 로그에 출력하지 않는다.
- 로컬 배포에서는 로컬 JSON을 사용하고, GitHub Actions 배포를 별도로 추가하기 전까지 GitHub Secret을 읽는 workflow를 만들지 않는다.
- 서비스 계정에는 Bandalart 앱의 내부 테스트 배포에 필요한 최소 Play Console 권한만 부여한다.

## 5. 검증 계획

- `git diff --check`
- Gradle 설정 로딩 성공 여부 확인
- `./gradlew tasks --group publishing`에서 `publishReleaseBundle` 생성 확인
- Python 스크립트의 문법 검사
- 자격 증명 파일이 Git에서 ignored 상태인지 확인
- 실제 Play Developer API 조회와 업로드는 사용자가 서비스 계정 초대 및 릴리스 노트를 확인한 뒤 별도 `deploy-playstore` 요청에서 실행

## 6. 완료 조건

- Bandalart 앱 ID와 내부 테스트 트랙만 참조한다.
- Fastlane 경로를 참조하지 않는다.
- 배포에 필요한 로컬 파일이 없거나 versionCode가 충돌하면 업로드 전에 중단한다.
- `publishReleaseBundle` task가 생성되고 배포 스킬의 모든 경로가 저장소에 존재한다.
