# 인앱 업데이트 완료 흐름 보강 전략

## 재현 결과

- `2.2.4`에서 Flexible Update를 시작하면 다운로드 완료 Snackbar가 표시된다.
- Snackbar의 `재시작`을 눌러도 즉시 설치 UI나 앱 재시작이 보이지 않는다.
- 앱을 완전히 종료한 뒤 다시 실행하면 Play 설치 UI가 나타나고 `2.2.5`로 업데이트된다.

현재 테스트 기기는 이미 `2.2.5`로 업데이트되어 동일 Play 업데이트를 다시 제공할 수 없다.
따라서 실제 Play 설치 UI를 자동화한 회귀 테스트는 다음 상위 버전 없이 구성할 수 없다.

## 목표

- Home 진입과 foreground 복귀 시 다운로드 완료 상태를 놓치지 않는다.
- 사용자가 `재시작`을 누르면 완료 요청의 성공과 실패를 관측할 수 있게 한다.
- 완료 요청 실패 시 UI가 사라지거나 무반응 상태로 남지 않게 한다.
- `HandleAppUpdate`의 완료 요청과 lifecycle 복구 흐름은 단위 테스트로 고정한다.
- PR CI에서 debug unit test 전체를 실행해 회귀 테스트 누락을 방지한다.

## 조사 순서

1. Home의 바텀시트·Snackbar 이벤트 흐름을 확인한다.
2. `HandleAppUpdate`의 Play Core listener 등록/해제와 `completeUpdate()` 호출을 확인한다.
3. Activity lifecycle에서 `InstallStatus.DOWNLOADED` 재확인 여부를 확인한다.
4. 테스트 가능한 `HandleAppUpdate` 경계에 회귀 테스트를 추가한다.

## 검증

- 관련 `HandleAppUpdate` 테스트를 실행한다.
- Android CI에서 `testDebugUnitTest`가 실행되는지 확인한다.
- 코드 스타일 검사를 실행한다.
- 다음 versionCode 또는 Internal App Sharing 환경에서 실제 설치 UI 전환을 확인한다.

## 적용 결과

- `RESUMED`마다 Play Core의 `InstallStatus.DOWNLOADED`를 재확인한다.
- 완료 이벤트를 놓친 뒤 Home이 다시 구성되어도 `재시작` Snackbar를 복구한다.
- `재시작` 선택 후 설치 진행 상태를 표시한다.
- `completeUpdate()` 실패를 기록하고 재시도 안내를 제공한다.
- Android CI에 unit test 단계를 추가한다.
