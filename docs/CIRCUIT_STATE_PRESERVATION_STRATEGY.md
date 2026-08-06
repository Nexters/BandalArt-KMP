# Circuit 상태 보존 정책과 날짜 피커 복원 전략

- 작성일: 2026-08-06
- 기준 브랜치: `refactor/circuit-state-preservation`
- 관련 이슈: [#210](https://github.com/Nexters/BandalArt-KMP/issues/210)
- 후속 테스트 인프라: [#217](https://github.com/Nexters/BandalArt-KMP/issues/217)

## 목표

1. Presenter와 UI의 `remember`, `rememberRetained`, `rememberSaveable` 사용 근거를 고정한다.
2. 날짜 피커에서 완료 전 선택한 연·월·일을 configuration change 뒤에도 복원한다.
3. 새 due date나 다른 cell을 열 때 이전의 미확정 선택값을 재사용하지 않는다.
4. 기존 navigation, repository 작업과 one-shot effect의 생명주기를 바꾸지 않는다.

## 상태 보존 기준

| API | 보존 범위 | 이 프로젝트에서의 용도 |
| --- | --- | --- |
| `remember` | 현재 composition의 recomposition | repository snapshot, in-flight guard, 일시 UI, one-shot effect |
| `rememberRetained` | recomposition, Circuit back stack, Android configuration change | 저장 전 modal draft와 노출 상태 |
| `rememberSaveable` | recomposition, back stack, configuration change, process recreation | primitive/saveable UI 입력 중 process 복원 가치가 있는 값 |

Circuit 공식 문서는 `rememberRetained`가 Android의 hidden ViewModel을 이용해 back stack과 configuration change를 넘지만 process death를 복원하지 않는다고 설명한다. `rememberSaveable`은 save 가능한 값에 한해 saved state registry에도 참여한다.

- [Circuit Presenter retention](https://slackhq.github.io/circuit/docs/presenter/#retention)
- [Circuit `rememberRetained` API](https://slackhq.github.io/circuit/api/0.x/circuit-retained/com.slack.circuit.retained/remember-retained.html)

`Navigator`, `Context`, `View`처럼 누수 가능한 객체는 retained/saveable 상태에 넣지 않는다. coroutine 작업과 중복 실행 방지 flag는 같은 composition 생명주기를 유지하고 flag만 따로 retain하지 않는다.

## 감사 결과

### Presenter: `remember` 유지

- `OnboardingPresenter.isCompleting`, `SplashPresenter.isChecking`
  - composition scope coroutine과 함께 사라져야 하는 중복 실행 방지 flag다.
  - flag만 retain하면 구성 변경으로 coroutine은 취소됐지만 `true`가 남아 재시도를 막을 수 있다.
- `CompletePresenter.sideEffect`
  - 저장·공유 외부 action을 retain하면 복원 시 중복 실행할 위험이 있다.
- `HomePresenter`의 repository 조회 결과, loading/completion snapshot
  - repository가 source of truth이며 새 composition에서 다시 읽는다.
- `HomePresenter.isCreatingEmptyBandalart`
  - composition scope의 생성 작업과 생명주기를 맞춘다.
- dropdown 상태
  - 화면 재생성 시 닫혀도 되는 일시 UI다.
- image request, update request, effect, requested completion id
  - 처리·소비 event와 한 단위인 one-shot 상태다. 일부만 retain하면 중복 또는 고착이 생길 수 있다.

기존 Presenter 테스트가 Onboarding/Splash의 빠른 중복 event를 repository/navigation 1회로 제한하고, Complete/Home effect를 명시적으로 consume하는 동작을 검증한다. 이 상태들의 저장 방식을 이번 작업에서 바꾸지 않는다.

### Presenter: `rememberRetained` 유지

- `HomePresenter.bottomSheet`
- `HomePresenter.dialog`

사용자가 작성 중인 cell/bandalart draft와 modal 노출 상태는 back stack/configuration change를 넘길 가치가 있다. 현재 payload에는 `Navigator`나 platform object가 없다.

### UI: `remember` 유지

- `SnackbarHostState`, `GraphicsLayer`, update manager/listener, animation progress
  - composition 또는 platform owner에 결합된 객체다.
- bottom sheet title/description `TextFieldValue`
  - 실제 문자열 draft는 매 입력마다 retained `bottomSheet.cellData`로 올라간다. 재생성 시 텍스트는 복원되며 cursor/selection만 초기화되는 현재 동작은 허용한다.
- emoji/color picker local selection
  - 선택 시 retained bottom sheet draft가 즉시 갱신되므로 재생성 시 입력값에서 복원된다.

## 확인된 복원 공백

`BandalartDatePicker`의 `chosenYear`, `chosenMonth`, `chosenDay`는 완료를 누르기 전까지 Presenter draft로 올라가지 않는다. 현재 단순 `remember`라 picker가 열린 상태에서 configuration change가 발생하면 선택값이 기존 due date로 초기화된다.

### 구현

- 세 문자열을 `rememberSaveable(draftKey)`로 변경한다.
- `draftKey`는 cell id와 현재 확정 due date로 만든 안정적인 문자열을 UI 호출부에서 전달한다.
- configuration change에서는 같은 key로 saved 값을 복원한다.
- 다른 cell 또는 새로 확정된 due date는 key가 달라져 과거 값을 복원하지 않는다.
- picker를 닫으면 composition에서 제거되므로 같은 cell을 다시 열 때 미확정 값은 새로 초기화된다.
- 연·월 변경 뒤 존재하지 않는 날짜를 월말로 보정하는 `selectedDateWithValidate` 동작은 유지한다.

날짜 draft를 `HomeScreen.BottomSheetState`로 hoist하는 방식은 이번 범위에 포함하지 않는다. 다른 화면에서 draft를 공유하거나 Presenter가 날짜 휠 event를 검증해야 할 필요가 생길 때 재검토한다.

## 변경 범위

- `BandalartDatePicker` saveable state와 안정적인 key
- 날짜 validation 단위 테스트
- 상태 분류와 수동 복원 검증 문서

## 비범위

- 기존 Presenter 상태의 일괄 `rememberRetained` 전환
- bottom sheet의 모든 local UI object 저장
- 새 Compose UI test framework 또는 `androidDeviceTest` 도입
- 날짜 피커 디자인, 날짜 범위, reset 의미 변경
- 앱 버전 증가와 Internal Testing 배포

## 검증 기준

자동 검증:

- 윤년 2월 29일 유지
- 평년 2월 29/31일은 28일로 보정
- 30일 월의 31일은 30일로 보정
- 기존 Presenter 중복 event와 effect consume 테스트 유지
- `:feature:home:testAndroidHostTest`
- Spotless/Detekt와 전체 CI

수동 검증:

1. 날짜 피커에서 연·월·일을 변경하고 완료하지 않은 채 Android 화면을 회전한다.
2. 선택한 세 값이 유지되는지 확인한다.
3. 닫은 뒤 같은 cell을 다시 열면 미확정 선택이 남지 않고 확정 due date에서 시작하는지 확인한다.
4. 다른 cell을 열었을 때 이전 cell의 선택이 섞이지 않는지 확인한다.
5. 완료 후 bottom sheet에 보정된 due date가 표시되고 저장되는지 확인한다.

현재 `androidHostTest`에는 Compose state restoration harness가 없다. 이 한 건을 위해 JUnit runner와 UI test dependency를 추가하지 않고, 순수 날짜 validation은 host test로 고정하며 실제 configuration restoration은 위 수동 항목과 CI compile로 검증한다.

Compose UI restoration과 interaction test가 반복해서 필요해질 때의 host/device/KMP harness 선택은 #217에서 별도로 진행한다.

## CI 후속 수정

직전 #215에서 `MainActivity.registerForActivityResult`를 사용하기 시작하면서, Play Core와 Firebase 경유로 들어오던 `androidx.fragment:fragment:1.1.0`이 Android lint의 최소 요구 버전 1.3.0보다 낮다는 문제가 드러났다. 이번 상태 복원 변경과 직접 관련된 오류는 아니지만 현재 브랜치 CI를 막으므로, 앱 모듈이 Fragment 1.8.9를 직접 선언해 런타임 의존성을 정렬한다. 기존 전이 의존성을 제외하거나 lint를 억제하지 않는다.
