# Android 2.2.13 Clean Release 복구 전략

## 배경

2.2.12에서 `composeApp/src/commonMain/composeResources`를 제거해 Compose 리소스의 단일 소스를 `core:designsystem`으로 정리했다.

하지만 기존 빌드 산출물이 남아 있던 로컬 배포 worktree에서 `publishReleaseBundle`을 실행하자, Compose resource task가 제거된 source set을 `NO-SOURCE`로 판단하면서 예전 생성물을 삭제하지 않았다. 그 결과 Play Internal Testing에 업로드된 2.2.12 AAB에는 다음 두 리소스 namespace가 모두 포함됐다.

- `bandalart.composeapp.generated.resources`
- `bandalart.core.designsystem.generated.resources`

Play에 등록된 versionCode `20212`는 재사용할 수 없으므로 clean AAB는 2.2.13 (`20213`)으로 복구 배포한다.

## 목표

- Android 앱 버전을 2.2.13 (`20213`)으로 올린다.
- release 업로드 전에 clean AAB를 별도로 생성한다.
- AAB에 `bandalart.composeapp.generated.resources`가 없고 `bandalart.core.designsystem.generated.resources`만 있는지 확인한다.
- 검증된 AAB와 동일한 소스에서 Play Internal Testing 업로드를 실행한다.
- 이후 Android Play 배포에서도 같은 stale generated resource 문제가 재발하지 않도록 workflow를 보강한다.

## 범위

### 포함

- patch version `12`에서 `13`으로 상향
- Android Play 배포 workflow에 clean release AAB 사전 생성 및 구성 검사 단계 추가
- 기존 2.2.12 출시 노트 유지

### 제외

- UI와 기능 코드 변경
- Compose 리소스 추가 삭제
- Production 트랙 배포
- 2.2.12 Internal 릴리스 삭제 또는 수정

## 검증 순서

1. 최신 `main`과 동일한 clean worktree인지 확인한다.
2. `./gradlew clean :androidApp:bundleRelease --no-configuration-cache`를 실행한다.
3. AAB 크기와 Compose resource namespace를 확인한다.
4. versionName `2.2.13`, versionCode `20213`을 확인한다.
5. CI의 code quality, unit tests, Android lint/build, iOS framework build가 통과한 뒤 병합한다.
6. 최신 `main`에서 Play 전체 트랙 최대 versionCode가 `20212`인지 다시 확인한다.
7. 검증된 clean 산출물 상태에서 `publishReleaseBundle`을 실행한다.
8. Play Internal 트랙의 최대 versionCode가 `20213`으로 갱신됐는지 확인한다.

## 중단 조건

- clean AAB에 `bandalart.composeapp.generated.resources`가 남아 있다.
- clean AAB에 `bandalart.core.designsystem.generated.resources`가 없다.
- Play 전체 트랙 최대 versionCode가 `20213` 이상이다.
- CI 실패, credential 누락 또는 관리자 권한 우회가 필요하다.
