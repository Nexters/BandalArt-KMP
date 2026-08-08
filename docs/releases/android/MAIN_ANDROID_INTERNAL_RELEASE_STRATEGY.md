# main Android 내부 테스트 출시 전략

## 배경

Circuit + Metro KMP 통합과 설정·테마 기능이 `main`에 반영됐지만, Android Play 내부 테스트 배포 설정은 기존 `develop`에만 남아 있다. `main`의 Android CI는 동작하지만 Gradle Play Publisher, Play versionCode 조회 스크립트와 Internal release notes가 없어 #182의 수동 출시 검증을 진행할 수 없다.

Play Developer API 조회 결과 2026-08-06 기준 전체 트랙의 최대 versionCode는 `20206`이며, 다음 유효 버전은 `2.2.7 (20207)`이다.

## 목표

- `main`의 `androidApp`에서 서명된 release AAB를 Play Internal Testing에 업로드할 수 있게 한다.
- Circuit + Metro KMP 통합 빌드를 기존 설치본 위에 업데이트해 로컬 데이터와 주요 기능을 검증한다.
- 앱 버전을 `2.2.7 (20207)`로 올려 Play versionCode 충돌을 방지한다.

## 변경 범위

- Gradle Play Publisher 4.0.0 플러그인을 루트에 `apply false`, `androidApp`에 적용한다.
- 업로드 트랙을 `internal`, 상태를 `COMPLETED`, 산출물을 App Bundle로 고정한다.
- 로컬 `playstore/service-account-key.json`이 있을 때만 credentials로 연결한다.
- 모든 Play 트랙의 최대 versionCode를 조회하는 스크립트를 `scripts/`에 둔다.
- Internal track 한국어 release notes를 `androidApp/src/main/play/release-notes/ko-KR/internal.txt`에 둔다.
- `patchVersion`을 7로 올린다.

## 비범위

- Fastlane 설정 복구 또는 참조
- GitHub Actions 기반 자동 Play 배포
- production 트랙 배포
- iOS/TestFlight/App Store 배포
- 앱 기능과 UI 변경

## 보안 원칙

- 서비스 계정 JSON, keystore, 비밀번호를 Git에 추가하거나 로그로 출력하지 않는다.
- `playstore/service-account-key.json`은 `.gitignore`로 보호한다.
- 업로드 전 source commit, 버전, 최대 versionCode, release notes와 실행 task를 다시 표시하고 사용자 승인을 받는다.

## 검증

- `git diff --check`
- Python 조회 스크립트 문법 검사 및 Play API 조회
- `./gradlew tasks --group publishing`에서 `publishReleaseBundle` 생성 확인
- `./gradlew :androidApp:processReleaseVersionCodes`로 `2.2.7 (20207)` 확인
- PR CI의 정적 분석, 단위 테스트, Android lint/build, iOS framework build 통과
- 머지 후 최신 `origin/main`으로 `publishReleaseBundle --no-configuration-cache` 실행

## 수동 출시 체크리스트

- 기존 2.2.6 설치 데이터가 2.2.7 업데이트 후 유지된다.
- Splash, Onboarding, Home, Complete 내비게이션이 정상 동작한다.
- 반다라트 생성·편집·삭제, 바텀시트, 공유·저장이 정상 동작한다.
- 시스템·라이트·다크 테마와 앱 재시작 후 설정 복원이 정상 동작한다.
- 인앱 업데이트 동작과 Crashlytics의 신규 치명적 오류를 확인한다.

## 완료 조건

- `main`에서 Internal track 업로드 task를 재현할 수 있다.
- `2.2.7 (20207)`이 Internal Testing에 업로드된다.
- 기존 설치본 업데이트 및 Android 주요 기능 회귀 검증을 완료한다.
- 결과를 #182에 기록하고 남은 iOS 수동 검증과 `develop` 역할 종료 여부를 갱신한다.
