# Circuit 첫 출하 준비 전략

## 배경

- `develop`의 Circuit 기반 화면을 Android에서 처음 출하한다.
- Play Console 전체 트랙의 최대 `versionCode`는 `20204`이며, 다음 배포 버전은 `2.2.5 (20205)`다.
- 기존 ViewModel 테스트는 현재 Presenter 구조와 맞지 않으므로 테스트 의도만 선별해 Circuit Presenter 테스트로 옮긴다.

## 목표

1. 첫 출하를 막는 확인된 Presenter 회귀를 수정한다.
2. 상태 복원과 코루틴 사용에서 불안정한 API를 제거한다.
3. 핵심 화면 흐름을 Presenter 공개 인터페이스로 검증한다.
4. 앱 버전을 `2.2.5 (20205)`로 올려 Play Console 업로드 조건을 충족한다.

## 작업 범위

### 코드 수정

- 완료 화면이 `CompleteScreen`의 ID, 제목, 이모지, 이미지 URI를 상태에 반영하도록 수정한다.
- 이미지 저장 이벤트가 토스트 이벤트에 덮어써지는 문제를 제거한다. 저장 성공 토스트는 기존 UI 효과 처리부가 담당한다.
- Home 화면의 바텀시트와 다이얼로그 상태는 Bundle 저장 대상이 아니므로 Circuit retained 상태로 관리한다.
- Circuit 내부 코루틴 API 대신 Compose 공개 API를 사용한다.

### 테스트

- Circuit의 `Presenter.test()`와 가짜 Navigator/Repository를 사용한다.
- 완료 화면: 초기 상태, 완료 처리, 뒤로 가기, 저장·공유 효과를 검증한다.
- 온보딩: 완료 상태 저장 후 Home으로 이동하는지 검증한다.
- 스플래시: 온보딩 완료 여부에 따른 이동을 검증한다.
- Home은 기존 ViewModel 테스트의 모든 세부 구현을 복제하지 않고, 생성·제한·업데이트 판단 등 대표 사용자 흐름을 우선 검증한다.
- 인앱 업데이트 테스트의 가상 신규 버전은 새 앱 버전보다 큰 값으로 변경한다.

#### 테스트 환경

- 앱의 `compileSdk`와 `targetSdk`는 36을 유지한다.
- Presenter JVM 테스트는 Android 16 API 동작을 검증하지 않으므로 Robolectric SDK 35에서 실행한다. 프로젝트 Java toolchain 17에서는 Robolectric SDK 36이 요구하는 JDK 21 테스트 worker를 사용할 수 없기 때문이다.
- Android 16 고유 동작은 SDK 36 기기 또는 에뮬레이터에서 별도 수동 검증한다.
- 구현 기준: [Circuit 공식 Testing 문서](https://slackhq.github.io/circuit/docs/testing/)

### 버전 및 문서

- `patchVersion`을 `5`로 변경해 `versionName=2.2.5`, `versionCode=20205`를 생성한다.
- 배포 전략 문서의 후보 버전과 실제 패키지 ID를 현재 값으로 정리한다.

## 제외 범위

- 첫 출하 직전의 대규모 Home Presenter 분할이나 상태 모델 재설계
- CMP/iOS 구현 변경
- Fastlane 도입
- 이 브랜치에서 Play Store 업로드 실행

## 완료 조건

- 변경 모듈의 Presenter 단위 테스트가 통과한다.
- `ktlintCheck`와 `detekt`가 통과한다.
- 생성되는 앱 버전이 `2.2.5 (20205)`임을 확인한다.
- 머지 후 `develop`에서 Play Store Internal 트랙 배포를 다시 실행할 수 있다.
