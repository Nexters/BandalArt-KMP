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

## 실행 전 확인 사항

1. 테스트할 최신 빌드를 대상 기기에 설치한다.
2. Android 실기기의 잠금을 해제하거나 iOS Simulator를 부팅한다.
3. Maestro 기기 목록에서 대상이 연결 상태인지 확인한다.
4. 기존 데이터를 유지할 테스트는 `clearState: false`로 실행한다.
5. 알림과 위젯처럼 앱 밖에서 발생하는 동작은 해당 플랫폼의 실기기에서 별도로 확인한다.
