# Circuit Home 읽기 전환 전략

## 1. 목적

이 문서는 이슈 #182의 6-B 단계인 Home 읽기 책임의 Circuit Presenter 전환 범위와 검증 기준을 구현 전에 고정한다.

- 기준 브랜치: `main` (`4ff6de78c69526b7228d4a44931b6e72eacf8c21`)
- 작업 브랜치: `refactor/circuit-home-read`
- 대상: 반다라트 목록 구독, 최근 항목 선택, 빈 목록 초기 생성, 상세·셀 트리 조회, 완료 전환 감지
- 목표: repository 기반 Home 읽기 상태를 KMP Circuit Presenter와 Metro factory로 옮기고 공식 Presenter test로 동작을 고정
- 비목표: Home 런타임 화면 교체, 생성 버튼·편집·삭제, modal 상태, 이미지 capture/save/share, Android 선택 업데이트, Koin 제거

## 2. 관찰된 현재 동작

### `main`

- `LegacyHomeScreen` 안의 Compose Navigation이 `HomeRoute`를 열고 Koin `HomeViewModel`이 모든 상태를 소유한다.
- 목록을 구독한 뒤 이전 완료 스냅샷과 비교해 새로 완료된 첫 항목을 선택한다.
- 새 완료 항목이 없으면 목록의 완료 상태를 DataStore에 동기화한다.
- 목록이 비어 있으면 반다라트를 하나 생성하고 최근 ID와 완료 스냅샷을 기록한다.
- 목록이 있으면 최근 ID가 유효할 때 해당 항목을, 아니면 첫 항목을 연다.
- 선택한 항목의 main → sub → task 셀을 조회해 중첩 `BandalartCellEntity`를 만든다.
- 완료 전환 후 이미지 capture와 Complete 이동은 ViewModel UI event와 `LegacyHomePresenter` bridge를 거친다.

### `develop` 참고 구현

- 하나의 `HomePresenter`가 읽기, 편집/modal, 플랫폼 effect와 업데이트를 모두 소유한다.
- Android Hilt scope, `java.util.Locale`, Android resource와 과거 Circuit retained/coroutine 패턴이 섞여 있어 전체 파일을 그대로 이식하지 않는다.
- 목록·최근 항목·빈 목록 생성 흐름과 Presenter test의 시나리오만 동작 참고 자료로 사용한다.

## 3. 단계 분리 결정

### 3.1 이번 PR은 shadow Presenter 단계다

`6-B`에서는 새 Home Circuit Screen/Presenter를 구현하고 Metro에 등록하되 앱의 root BackStack에는 아직 연결하지 않는다.

```text
현재 runtime
  LegacyHomeScreen → HomeRoute → HomeViewModel

6-B에서 추가
  HomeScreen → HomePresenter(read only, runtime 미연결)
```

이유는 새 Presenter와 기존 ViewModel을 동시에 composition하면 둘 다 목록을 구독하고 빈 목록 생성·완료 스냅샷 갱신을 수행해 중복 쓰기와 중복 Complete 이동이 발생할 수 있기 때문이다.

- `LegacyHomeScreen`, `HomeViewModel`, Koin module과 Compose Navigation을 유지한다.
- 새 Presenter는 직접 Presenter test와 Metro graph test에서만 실행한다.
- `6-C`에서 편집/modal 책임을 추가하고 `6-D`에서 플랫폼 effect를 추가한 뒤, 완성된 Home Screen으로 runtime을 한 번에 교체한다.
- runtime 교체 PR에서만 legacy ViewModel/Koin/NavHost를 제거한다.

### 3.2 Screen과 읽기 State

- 기존 `HomeScreen` composable은 동작 변경 없이 `HomeContent`로 이름을 정리해 Circuit `HomeScreen` 타입과 구분한다.
- `HomeScreen`은 파라미터가 없는 `ParcelableScreen`과 `StaticScreen`으로 정의한다.
- 이번 단계의 State는 다음 읽기 값만 소유한다.
  - 반다라트 목록
  - 선택한 반다라트
  - main/sub/task 셀 트리
  - 초기 조회 여부
  - 새 완료 항목 여부
  - 읽기 event sink
- repository에서 다시 만들 수 있는 목록·상세 상태는 일반 `remember`와 `LaunchedEffect`로 관리한다.
- 사용자 입력 초안과 modal 같은 retained state는 `6-C`에서 `rememberRetained` 적용 대상을 별도로 판단한다.

### 3.3 읽기 이벤트

- 목록에서 항목 선택 시 최근 ID를 저장하고 해당 상세와 셀 트리를 다시 읽는다.
- 완료 전환 감지는 State에만 노출한다. 이미지 capture와 Complete navigation은 `6-D` 플랫폼 effect 전환 전까지 새 Presenter에서 실행하지 않는다.
- 생성 버튼, 편집, 삭제와 modal event는 이번 계약에 넣지 않고 `6-C`에서 추가한다.
- `InAppUpdateRepository`는 Android 플랫폼 effect 단계인 `6-D` 전까지 주입하지 않는다.

## 4. 읽기 알고리즘

1. Presenter composition당 목록 collector를 하나 시작한다.
2. repository 목록을 `BandalartUiModel`의 immutable list로 변환한다.
3. 이전 완료 스냅샷과 비교해 `false → true`로 바뀐 항목을 찾는다.
4. 새 완료 항목이 있으면 첫 항목을 선택하고 `isBandalartCompleted = true`로 상세와 셀 트리를 읽는다.
5. 새 완료 항목이 없으면 현재 목록의 완료 상태를 스냅샷에 동기화한다.
6. 목록이 비어 있으면 정확히 한 번 새 반다라트를 생성한다.
   - 생성 ID를 최근 ID로 저장한다.
   - 완료 스냅샷을 초기화한다.
   - 생성된 상세와 셀 트리를 읽는다.
7. 목록이 있으면 유효한 최근 ID를 우선하고, 없으면 첫 항목을 선택한다.
8. 상세 조회 후 main, sub, task 순서로 셀을 읽어 기존 UI와 동일한 중첩 구조를 만든다.

완료 항목을 선택한 경로에서는 Complete 진입 전에 완료 스냅샷을 `true`로 덮어쓰지 않는다. 실제 완료 처리는 이미 전환된 `CompletePresenter`가 담당하며, `6-D`에서 navigation을 연결할 때 이 경계를 회귀 테스트한다.

## 5. 구현 순서

1. Home feature에 Circuit/Metro/Parcelable 및 Presenter test 의존성을 추가한다.
2. KMP `HomeScreen` 읽기 State/Event 계약을 작성하고 기존 UI composable 이름 충돌을 정리한다.
3. `HomePresenter`에 목록, 최근 선택, 빈 목록 초기 생성과 상세·셀 트리 조회를 구현한다.
4. 완료 전환을 읽기 State로 노출하되 capture/navigation은 실행하지 않는다.
5. fake repository와 Circuit 공식 `Presenter.test` 기반 테스트를 작성한다.
6. Metro graph에서 Home Presenter factory 생성을 확인한다.
7. 마이그레이션 맵과 트러블슈팅 문서를 갱신한다.

## 6. 테스트 및 완료 기준

### Presenter test

- 목록을 받고 유효한 최근 ID의 항목을 선택한다.
- 최근 ID가 목록에 없으면 첫 항목을 선택한다.
- 빈 목록이면 하나만 생성하고 최근 ID·완료 스냅샷·상세 상태를 갱신한다.
- main/sub/task 셀을 기존과 동일한 중첩 트리로 만든다.
- `false → true` 완료 항목이 여러 개면 기존과 같이 첫 항목을 선택한다.
- 완료 전환 State를 노출하는 동안 완료 스냅샷을 먼저 덮어쓰거나 Navigator를 호출하지 않는다.
- 목록 항목 선택 event가 최근 ID와 상세 상태를 갱신한다.

### 통합 검증

- Metro graph가 Home Presenter factory를 생성한다.
- 기존 `LegacyHomeScreen` UI/Presenter와 Home ViewModel test가 계속 통과한다.
- Home feature 및 composeApp Android host test가 통과한다.
- 이번 변경 Kotlin 파일의 Spotless 포맷과 Home/composeApp Detekt가 통과한다.
- CI의 Android/iOS build가 통과한다.

### 수동 검증

이번 단계는 runtime 미연결이므로 새 Presenter에 대한 별도 UI 수동 검증은 없다. 기존 Home 동작이 바뀌지 않았는지만 확인한다.

## 7. 롤백 경계

- 새 Home Screen/Presenter와 Metro binding은 runtime에서 참조되지 않으므로 해당 파일과 build 설정만 제거하면 된다.
- repository, DB, DataStore 형식과 legacy ViewModel을 변경하지 않는다.
- `6-C/6-D` 진행 중 설계 문제가 발견돼도 현재 앱의 Home 경로는 그대로 동작한다.

## 8. 참고 자료

- [Circuit Presenter testing](https://slackhq.github.io/circuit/docs/testing/)
- [Circuit code generation](https://slackhq.github.io/circuit/docs/code-gen/)
- [Metro Circuit integration](https://zacsweers.github.io/metro/1.1.1/circuit/)
- [`develop` HomePresenter 참고 구현](https://github.com/Nexters/BandalArt-KMP)

## 9. 구현 및 로컬 검증 결과

- Home feature에 Circuit/Metro codegen과 Presenter test 구성을 추가했다.
- 기존 runtime composable을 `HomeContent`로 이름만 변경하고 `LegacyHomeScreen → HomeRoute → HomeViewModel` 경로는 유지했다.
- `HomeScreen` 읽기 계약과 Metro assisted `HomePresenter`를 추가했다.
- 목록·최근 항목·빈 목록 생성·상세와 main/sub/task 트리·완료 전환·선택 event를 Circuit 공식 `Presenter.test` 5개로 검증했다.
- composeApp Metro graph에서 Home Presenter factory 생성을 검증했다.
- `:feature:home:testAndroidHostTest`, `:composeApp:testAndroidHostTest`가 통과했다.
- 이번 변경 Kotlin 파일의 Spotless IDE hook과 `:feature:home:detekt`, `:composeApp:detekt`가 통과했다.
- 전체 Home Spotless check는 기존 파일들의 상수 naming/format baseline 위반도 함께 검사하므로 이번 PR에서 일괄 수정하지 않았다. 필수 CI에는 Spotless가 포함돼 있지 않으며, 새 파일과 변경 라인은 별도로 포맷했다.
- Android/iOS build와 전체 회귀 테스트는 PR CI에서 확인한다.
