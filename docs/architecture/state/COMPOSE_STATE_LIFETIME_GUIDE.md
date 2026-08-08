# Compose와 Circuit 상태 수명 가이드

- 작성 기준: 2026-08-08, Circuit 0.35.1
- 적용 범위: Compose Multiplatform UI, Circuit Presenter, Android saved state, repository persistence

## 한 줄 원칙

상태 API부터 고르지 않는다. 먼저 **누가 소유하는 상태인지**, 다음으로 **얼마나 오래 살아야 하는지**, 마지막으로 **안전하게 저장할 수 있는지**를 결정한다.

## 결정 순서

1. 다른 값에서 계산할 수 있는가?
   - 저장하지 않고 입력에서 계산한다. 계산 비용이 클 때만 `remember(inputs)`로 결과를 캐시한다.
2. 앱 재시작 뒤에도 데이터 정합성이 필요하거나 exactly-once 동작인가?
   - Room·DataStore 같은 durable store를 사용하는 repository를 source of truth로 삼고, transaction과 idempotency 경계를 설계한다. 어떤 `remember*` API도 영속 저장을 대체하지 않는다.
3. 현재 composition에서만 필요하고 사라져도 안전한가?
   - `remember`를 사용한다.
4. Circuit 화면 record가 back stack에 남아 있는 동안과 Android configuration change를 넘어야 하지만 process 복원은 필요 없는가?
   - `rememberRetained`를 사용한다.
5. Android process recreation 뒤에도 복원할 가치가 있는 작은 UI 값인가?
   - `rememberSaveable` 또는 컴포넌트가 제공하는 `remember…State`를 사용한다.
6. 같은 복합 state holder identity를 live process에서 유지하면서 process recreation에는 작은 snapshot으로 재구성해야 하는가?
   - `rememberRetainedSaveable`을 예외적으로 검토한다.

상태가 여러 UI에서 필요하면 가장 가까운 공통 소유자까지 hoist한다. 화면의 비즈니스 상태는 Presenter가, 앱 재시작을 견뎌야 하는 데이터는 durable store를 사용하는 repository가 소유한다. 저장 API를 바꾸는 것만으로 소유권이 바뀌지는 않는다.

## 수명 비교

| 수단 | recomposition | Circuit back stack 복귀 | Android 구성 변경 | Android process 복원 | 대표 용도 |
| --- | ---: | ---: | ---: | ---: | --- |
| `remember` | O | X | X | X | animation, runtime 객체, coroutine-bound guard, one-shot effect |
| `rememberRetained` | O | O | O | X | process 복원이 필요 없는 modal draft와 화면 진행 상태 |
| `rememberSaveable` | O | O | O | O | 작은 입력, 선택 ID, 필터, 날짜, 스크롤 위치 |
| `rememberRetainedSaveable` | O | O | O | O | identity 유지와 snapshot 복원이 모두 필요한 복합 state holder |
| Room/DataStore 기반 repository | O | O | O | O | 사용자 데이터, entitlement, transaction·idempotency를 갖춘 grant, 재시작 복구 |

Circuit back stack 보존은 해당 화면 record가 stack에 남아 있을 때만 성립한다. 화면이 pop되어 record가 제거되면 retained와 saveable state도 함께 제거된다.

`rememberSaveable`의 process 복원은 Android saved-state 동작이다. iOS 앱 종료·재실행까지 보장하는 KMP 영속성으로 해석하지 않는다. 플랫폼을 넘어 복구해야 하는 값은 repository에 저장한다.

현재 프로젝트는 Circuit 0.35.1을 사용하며 Android에서는 기본 ViewModel-backed retained registry를 사용하고 `CircuitRetainedSettings.useFirstParty`를 켜지 않았다. Compose의 first-party retain API와 Circuit retained API를 한 화면에서 임의로 섞지 않고, 전환이 필요하면 별도 마이그레이션으로 플랫폼별 수명과 테스트 계약을 다시 고정한다.

## API별 기준

### `remember`

다음 값은 composition과 함께 사라지는 편이 안전하다.

- `SnackbarHostState`, animation/graphics/focus/interaction 객체
- coroutine scope, `Job`, callback과 같은 runtime 객체
- 실행 중인 coroutine과 수명을 같이해야 하는 중복 실행 guard
- 소비 뒤 없어지는 navigation, snackbar, image picker 같은 one-shot request/effect
- 입력이 바뀌면 다시 계산할 수 있는 캐시

in-flight flag만 retain하고 작업 coroutine은 composition에 묶으면, 구성 변경 때 작업은 취소됐는데 flag만 `true`로 남을 수 있다. flag와 작업은 같은 owner와 수명을 가져야 한다.

### `rememberRetained`

다음 조건을 모두 만족할 때 사용한다.

- Circuit 화면을 잠시 떠났다가 돌아오거나 Android 회전 뒤에도 이어져야 한다.
- process death 뒤에는 초기값 또는 repository 상태에서 안전하게 다시 시작할 수 있다.
- 값이 platform/lifecycle 객체를 품지 않는다.

현재 예시는 `HomePresenter`의 bottom sheet/dialog draft와 보상형 광고 런타임 coordinator/request 연결 상태다. 광고 보상의 실제 복구 기준은 retained state가 아니라 DataStore의 pending/granted record다.

### `rememberSaveable`

작고 직렬화 가능한 UI-owned 값에 사용한다.

- primitive, `String`, enum 이름, entity ID, index
- 검색어, 선택 탭·카테고리, 날짜 picker의 미확정 연·월·일
- 컴포넌트가 공식 Saver를 제공하는 scroll/state 객체

상태가 특정 cell, draft, item에 속하면 `rememberSaveable(identity)`의 `inputs`로 안정적인 identity를 전달해 live composition에서 대상이 바뀔 때 초기화한다. 다만 saved-state 복원은 이전 inputs와 복원값을 대조하지 않는다. process recreation 뒤에도 identity 정합성이 중요하면 저장 payload에 identity를 함께 담아 검증하거나 ID를 소유한 상위 상태/durable store로 hoist한다. entity graph, 대규모 목록, repository snapshot 전체를 Bundle에 넣지 않는다.

### `rememberRetainedSaveable`

현재 저장소에는 사용처가 없다. 다음 조건일 때만 도입한다.

- live process에서는 동일한 복합 state holder identity를 유지해야 한다.
- process recreation에는 작은 saveable snapshot으로 재구성할 수 있다.
- 저장 snapshot이 최신 값보다 조금 늦어도 안전하다.

일반 primitive에는 `rememberSaveable`이면 충분하다. 과금, 보상, entitlement, 정확히 한 번 처리처럼 stale snapshot이 위험한 상태에는 사용하지 않는다.

## 저장하거나 retain하지 않는 값

- `Navigator`, `Context`, `Activity`, `View`
- `Flow`, coroutine scope, `Job`, callback, listener
- SDK manager, platform launcher, graphics 객체
- repository의 원본 entity graph나 대용량 목록
- 소비형 one-shot effect와 navigation event

이 값들은 composition owner가 `remember`로 만들거나 DI/platform owner에서 주입하고, 복원에는 값 자체가 아니라 필요한 최소 ID와 repository 데이터를 사용한다.

## 현재 코드의 기준 사례

| 상태 | 선택 | 이유 |
| --- | --- | --- |
| `BandalartApp` navigation back stack | `rememberSaveableBackStack` | Circuit navigation record를 saved state와 함께 복원 |
| `HomePresenter` repository snapshot·loading·effect queue | `remember` | repository에서 재수집하거나 composition 작업과 함께 종료 |
| `HomePresenter.bottomSheet`, `dialog` | `rememberRetained` | 화면 왕복·회전 중 draft와 modal을 유지, process 복원은 불필요 |
| 보상형 광고 coordinator/request/recovery flag | `rememberRetained` + DataStore | retained는 회전 연결, DataStore는 process death와 exactly-once 복구 담당 |
| `BandalartDatePicker` 연·월·일 | `rememberSaveable(draftKey)` | `draftKey`를 identity input으로 사용해 작은 미확정 UI 입력을 저장. cross-process identity 보장은 별도 검증 필요 |
| 이모지 picker 선택 | `rememberSaveable` 사용 중 | 현재 상위 draft와 의미가 중복될 수 있어 stateless 또는 `remember(currentEmoji)`로 정리할 후보 |
| 이모지 picker 카테고리 | `rememberSaveable` 예정 | 작은 UI filter이며 카테고리 기능 브랜치에서 적용 예정 |
| `LazyGridState` 등 공식 `remember…State` | 해당 컴포넌트 API | framework의 Saver와 상태 계약을 재사용 |

## inputs와 identity 검증 규칙

- `remember(input)`과 `rememberSaveable(input)`의 positional argument는 live composition에서 상태를 재계산하는 `inputs`다. saved-state registry의 고유 key나 복원값의 identity 검증 수단이 아니다.
- 대상이 바뀌면 이전 상태를 재사용하면 안 되는 경우에만 identity input을 추가한다.
- mutable object 전체보다 안정적인 ID와 확정값을 조합한다.
- process recreation 때 다른 대상의 값이 복원되면 안 되는 경우에는 payload의 ID를 현재 ID와 대조하고 불일치 시 폐기하거나, 상위 owner/durable store가 ID별 상태를 관리한다.
- repository 값과 같은 의미를 중복 저장하지 않는다. caller가 즉시 값을 올린다면 local copy를 없애거나 `remember(currentValue)`처럼 입력 변경에 맞춰 reset한다.

## 테스트 기준

| 선택 | 최소 검증 |
| --- | --- |
| `remember` | 빠른 중복 event, coroutine 취소, effect 1회 consume |
| `rememberRetained` | Circuit back stack 왕복과 Android configuration recreation |
| `rememberSaveable` | state restoration/process recreation과 identity 변경 시 reset |
| durable persistence | 앱 재시작/relaunch, idempotent replay, 중복 callback·중단 지점 |

현재 Compose restoration test harness 도입은 #217에 남아 있다. harness가 없는 변경은 순수 transition 테스트와 수동 회전·복원 절차를 전략 문서에 명시한다.

## 리뷰 체크리스트

- 이 값의 source of truth와 가장 낮은 owner가 명확한가?
- 파생할 수 있는 값을 중복 저장하지 않았는가?
- 필요한 수명보다 강한 API를 선택하지 않았는가?
- process death 정합성을 `rememberRetained`에 기대지 않는가?
- saveable snapshot은 작고 직렬화 가능하며 조금 stale해도 안전한가?
- live reset inputs가 대상 identity에 맞고, process 복원값은 필요한 경우 payload identity로 별도 검증하는가?
- coroutine, guard, effect consumer의 수명이 서로 일치하는가?
- Android와 iOS의 복원 보장을 구분했는가?

## 근거

- [Circuit 0.35.1 `NavigableCircuitContent`의 record 정리](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-foundation/src/commonMain/kotlin/com/slack/circuit/foundation/NavigableCircuitContent.kt#L533-L575)
- [Circuit 0.35.1 retained 구현](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-retained/src/commonMain/kotlin/com/slack/circuit/retained/RememberRetained.kt#L25-L195)
- [Circuit Presenter retention](https://slackhq.github.io/circuit/docs/presenter/#retention)
- [Android Compose state saving](https://developer.android.com/develop/ui/compose/state-saving)
- [Compose `rememberSaveable` inputs와 복원 제한](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/rememberSaveable)
