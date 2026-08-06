# Android 2.2.10 Internal Testing 배포 전략

## 배경

PR #220의 설정 이메일 문의 기능을 포함한 최신 `main`을 Internal Testing으로 배포한다.

Play Developer API 선검증 결과 전체 트랙의 최대 versionCode는 `20209`이며, 현재 소스도 `2.2.9 (20209)`여서 같은 코드로 다시 업로드할 수 없다.

- Production: `20206`
- Internal: `20209`
- 다음 사용 가능 versionCode: `20210`

## 목표

- 앱 버전을 `2.2.10 (20210)`으로 올린다.
- 현재 변경 사항에 맞는 한국어 Internal release notes를 제공한다.
- 별도 PR과 CI를 통과한 merge commit만 배포 소스로 사용한다.
- 서명된 release AAB를 Google Play Internal Testing에 업로드한다.

## 버전 계산

저장소의 Android application convention은 다음 공식을 사용한다.

`versionCode = major * 10000 + minor * 100 + patch`

따라서 `2.2.10`은 `20210`이며 Play API가 반환한 다음 사용 가능 코드와 일치한다.

## Release notes

Internal Testing의 한국어 release notes에는 이번 배포에서 사용자가 확인할 수 있는 변경만 포함한다.

- 설정 화면 이메일 문의하기
- 포그라운드 복귀 시 인앱 업데이트 확인 보강
- 화면 상태와 기존 로컬 데이터 복원 안정성 개선
- 다크 모드 가독성 개선

PR 템플릿 정리나 내부 테스트 구조처럼 사용자에게 직접 노출되지 않는 변경은 제외한다.

## 작업 순서

1. `gradle/libs.versions.toml`의 patch version을 `10`으로 변경한다.
2. `androidApp/src/main/play/release-notes/ko-KR/internal.txt`를 갱신한다.
3. 계산된 versionName/versionCode와 release notes 경로를 확인한다.
4. 변경 파일 포맷과 diff를 검증한다.
5. commit-push 후 `main` 대상 PR을 생성한다.
6. 모든 CI가 통과하면 관리자 우회 없이 merge한다.
7. 최신 `origin/main`과 clean 상태를 다시 확인한다.
8. Play 전체 트랙 최대 versionCode, 서명, 서비스 계정과 upload task를 재검증한다.
9. 최종 배포 정보를 확인받은 뒤 `publishReleaseBundle`을 configuration cache 없이 실행한다.

## 검증

- [ ] versionName이 `2.2.10`이다.
- [ ] versionCode가 `20210`이다.
- [ ] Internal release notes가 비어 있지 않고 사용자 변경 사항과 일치한다.
- [ ] PR CI의 Code quality, Unit tests, Android lint/build, iOS framework build가 통과한다.
- [ ] 배포 직전 `main`과 `origin/main`의 ahead/behind가 모두 0이다.
- [ ] Play 전체 트랙 최대 versionCode가 `20210`보다 작다.
- [ ] GPP 대상이 Internal track과 `COMPLETED` 상태다.

## 중단 조건

- Play 전체 트랙 최대 versionCode가 `20210` 이상이다.
- 배포 소스가 dirty, unpushed 또는 원격과 diverged 상태다.
- release keystore, service account credential 또는 release notes가 없다.
- CI가 실패했거나 관리자 권한 우회가 필요하다.
