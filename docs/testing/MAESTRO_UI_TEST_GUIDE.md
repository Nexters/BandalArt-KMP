# Maestro UI 테스트 가이드

이 문서는 BandalArt의 Maestro 기반 UI 테스트 범위와 기기 선택 기준을 정한다. Maestro 자동화가 지원하지 않는 환경은 별도 수동 검증으로 보완한다.

## 플랫폼별 지원 범위

Maestro는 Android 실기기와 에뮬레이터, iOS Simulator에서 로컬 UI 테스트를 지원한다. iOS 실기기 실행은 아직 지원하지 않는다.

| 플랫폼 | 에뮬레이터·Simulator | 실기기 |
| --- | --- | --- |
| Android | 지원 | 지원 |
| iOS | 지원 | 미지원 |

iPhone이 Xcode에 연결되어 있어도 Maestro 또는 Maestro MCP의 실행 대상으로 사용할 수 없다. iOS 실기기에서 앱을 빌드하고 실행하는 작업은 Xcode로 진행한다.

지원 범위가 바뀔 수 있으므로 도구를 업데이트할 때 [Maestro 지원 플랫폼](https://docs.maestro.dev/getting-started/build-and-install-your-app)과 [iOS 제약 사항](https://docs.maestro.dev/platform-support/ios-uikit)을 다시 확인한다.

## 프로젝트의 테스트 조합

일반 UI 회귀 검사는 다음 조합을 사용한다:

- **Android 실기기**: 제조사별 화면, 시스템 권한, 알림, 위젯을 포함한 동작 검사
- **iOS Simulator**: 화면 진입, 텍스트 노출, 입력, 내비게이션, 앱 재실행 검사
- **iOS 실기기 수동 검사**: 서명, TestFlight 설치, 알림, 위젯과 실기기에서만 확인할 수 있는 동작 검사

두 플랫폼의 공통 화면은 Android 실기기와 iOS Simulator에서 같은 Maestro flow 또는 같은 검증 항목으로 비교한다. iOS 실기기 결과를 Maestro 자동화 결과로 기록하지 않는다.

완료 화면 검사는 다음 flow를 사용한다:

- Android: `.maestro/android/completion-screen.yaml`
- iOS: `.maestro/ios/completion-screen.yaml`
- Android 알림 설정: `.maestro/android/notification-settings.yaml`

Android flow는 제목이 `완료 화면 테스트`이고 `last task`를 제외한 실행 목표가 모두 완료된 반다라트를 전제로 한다. 실행 전 `HOME` 키로 알림창 같은 시스템 오버레이를 닫고 앱을 시작한다. 달성률이 이미 100%이면 `last task`를 먼저 미완료로 되돌리고 앱을 재실행한다. 마지막 실행 목표와 상위 서브목표·메인목표가 함께 미완료로 집계되므로 이때 달성률은 `88%`다. flow는 이 값을 확인해 fixture 오류를 완료 문구 assertion 실패와 구분한 뒤, 마지막 목표를 길게 눌러 완료 화면으로 이동하고 내용을 검사한다. Android Maestro 입력은 한글 조합을 지원하지 않으므로 fixture 이름은 영문으로 유지한다.

iOS flow는 완료 화면에 수동 진입한 상태에서 내용과 뒤로가기만 자동 검사한다. 현재 Maestro의 iOS `longPressOn`은 Compose Multiplatform의 `combinedClickable` 셀에서 길게 누르기 대신 일반 탭으로 처리되어 수정 시트를 열기 때문에 완료 화면 진입 단계에는 사용하지 않는다. 이 제한은 텍스트가 Maestro Viewer에서 생략되어 보이는 현상과 무관하다.

접근성 문구는 화면 텍스트처럼 조회하며 `id` 선택자로 조회하지 않는다. 완료 화면의 뒤로가기는 `tapOn: "뒤로 가기"`로 선택한다.

Android 알림 설정 flow는 실기기에서 활성화 확인창, 앱 재실행 후 설정 유지와 비활성 상태 복원을 검사한다. 설정 화면의 테스트 알림 UI는 제품에서 숨긴 상태이므로 시스템 알림 수신 자체는 마감일이 있는 fixture와 실기기 수동 검사로 보완한다.

## 결과 내보내기와 알림

Maestro MCP Viewer의 누적 목록은 테스트를 탐색하고 실패 지점을 확인하는 용도로 사용한다. 현재 MCP `run` 호출은 성공 여부와 실행 명령 수를 반환하지만, 보고서 출력 경로를 받지 않는다. Viewer 목록을 공식 QA 증빙으로 직접 내보낼 수는 없다.

### 실행 방식별 결과물

실행 목적에 따라 다음 방식을 선택한다:

| 실행 방식 | 용도 | 저장 결과 | 완료 알림 |
| --- | --- | --- | --- |
| Maestro MCP Viewer | 로컬 탐색과 flow 수정 | Viewer 세션 기록 | 없음 |
| Maestro CLI | 로컬 회귀 검사 | HTML, JUnit, 스크린샷, 명령 기록, 디버그 로그 | 프로세스 종료 코드 |
| GitHub Actions | 반복 실행과 결과 보관 | workflow 로그와 artifact | GitHub 상태 알림, 외부 webhook |
| Maestro Cloud | 원격 기기 실행 | Console 결과, flow 상태, 영상과 로그 | Cloud Action 출력값, 외부 webhook |

### 로컬 결과 생성

아래 명령은 상세 HTML 보고서, 실패 스크린샷, 명령 기록과 디버그 로그를 한 디렉터리 아래에 생성한다:

```bash
mkdir -p build/reports/maestro
maestro test \
  --format html-detailed \
  --output build/reports/maestro/report.html \
  --test-output-dir build/reports/maestro/artifacts \
  --debug-output build/reports/maestro/debug \
  .maestro/android/completion-screen.yaml
```

CI가 읽을 JUnit 보고서가 필요하면 출력 형식과 파일명만 바꾼다:

```bash
maestro test \
  --format junit \
  --output build/reports/maestro/report.xml \
  --test-output-dir build/reports/maestro/artifacts \
  --debug-output build/reports/maestro/debug \
  .maestro/android/completion-screen.yaml
```

각 옵션은 다음 결과를 저장한다:

- `--output`: 사람이 읽는 HTML 또는 CI가 읽는 JUnit 보고서
- `--test-output-dir`: 실행 스크린샷과 `commands-*.json` 명령 기록
- `--debug-output`: `maestro.log` 디버그 로그

옵션을 생략하면 Maestro는 기본 실행 산출물을 macOS와 Linux의 `~/.maestro/tests` 아래에 저장한다. 보고서와 산출물의 차이는 [Maestro 테스트 보고서와 산출물](https://docs.maestro.dev/troubleshooting/debug-output)에서 확인한다.

PR에 QA 증빙을 남길 때는 통과한 HTML 또는 JUnit과 요약만 `reports/maestro/YYYY-MM-DD/`에 저장한다. 알림 내용이나 개인정보가 포함될 수 있는 원본 스크린샷, 기기 로그와 전체 명령 artifact는 프로젝트에 커밋하지 않는다.

### GitHub Actions 보관과 알림

GitHub Actions는 Maestro CLI의 종료 코드를 workflow 성공 여부로 사용한다. 테스트 단계의 성공 여부와 무관하게 결과를 남기려면 artifact 업로드 단계에 `if: always()`를 적용하고 `build/reports/maestro`를 업로드한다.

완료 알림은 다음 정보를 포함한다:

- workflow 이름과 commit SHA
- 성공, 실패 또는 취소 상태
- HTML 또는 JUnit artifact 링크
- 실패한 flow 이름

GitHub 알림만 사용할 때는 별도 secret이 필요 없다. Slack이나 다른 webhook을 연결할 때는 URL을 `SLACK_WEBHOOK_URL` 같은 GitHub Actions secret에 저장한다. 알림 단계에도 `if: always()`를 적용해야 실패 결과를 전송할 수 있다.

GitHub-hosted runner는 로컬 USB Android 실기기에 접근할 수 없다. 실기기 검사가 필요하면 기기가 연결된 self-hosted runner를 사용한다. 그렇지 않으면 Android 에뮬레이터, iOS Simulator 또는 Maestro Cloud에서 flow를 실행한다.

### Maestro Cloud 결과 알림

Maestro Cloud 공식 GitHub Action은 완료 후 다음 출력값을 제공한다:

- `MAESTRO_CLOUD_UPLOAD_STATUS`: 전체 실행 상태
- `MAESTRO_CLOUD_FLOW_RESULTS`: flow별 상태와 오류
- `MAESTRO_CLOUD_CONSOLE_URL`: 영상과 로그를 확인할 Console URL

동기 실행에서만 완료 상태와 flow 결과를 바로 사용할 수 있다. `async: true`는 workflow가 테스트 완료를 기다리지 않으므로 완료 알림 용도로 사용하지 않는다. Cloud 실행에는 Maestro Cloud Plan과 API key가 필요하다. 자세한 출력 형식은 [Maestro Cloud Action 출력값과 trigger](https://docs.maestro.dev/maestro-cloud/ci-cd-integration/github-actions/outputs-and-triggers)에서 확인한다.

### QA 완료 기준

QA 자동화 완료 기록에는 다음 항목을 남긴다:

1. 실행한 commit SHA와 앱 버전
2. 대상 플랫폼과 기기 또는 Simulator 버전
3. flow별 성공, 실패 상태
4. HTML 또는 JUnit 보고서와 실패 산출물
5. iOS 실기기처럼 자동화하지 못한 수동 검사 결과

## 실행 전 확인 사항

1. 테스트할 최신 빌드를 대상 기기에 설치한다.
2. Android 실기기의 잠금을 해제하거나 iOS Simulator를 부팅한다.
3. Maestro 기기 목록에서 대상이 연결 상태인지 확인한다.
4. 기존 데이터를 유지할 테스트는 `clearState: false`로 실행한다.
5. 알림과 위젯처럼 앱 밖에서 발생하는 동작은 해당 플랫폼의 실기기에서 별도로 확인한다.
