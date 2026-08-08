# Circuit Home 편집/modal 전환 전략

## 1. 목적

이 문서는 이슈 #182의 6-C 단계인 Home 생성·편집·삭제와 modal 상태의 Circuit Presenter 전환 범위와 검증 기준을 구현 전에 고정한다.

- 기준 브랜치: `main` (`596859a884560cebce9aa006e5892d344a3f3bd8`)
- 작업 브랜치: `refactor/circuit-home-edit`
- 선행 작업: PR #196의 Home 읽기 shadow Presenter
- 대상: 반다라트 생성 제한, 셀 편집 draft, 목록/셀/emoji bottom sheet, 삭제 dialog, dropdown, repository mutation
- 목표: 사용자 편집 상태를 `rememberRetained`로 보존하고 기존 KMP repository mutation을 Circuit Event로 연결한다.
- 비목표: Home runtime 교체, 공유·저장·capture, Complete 이동, Android 선택 업데이트, 앱 버전 effect, Koin/ViewModel/Compose Navigation 제거

## 2. 기준 구현과 우선순위

### `main`은 제품 동작 기준이다

- KMP `HomeViewModel`, `HomeUiState/Action/Event`, `HomeBottomSheets`, `HomeDialogs`와 현재 repository entity를 기준으로 한다.
- 이미 KMP로 이전된 `Locale`, Compose resources, Room/DataStore와 Android/iOS UI를 재구현하지 않는다.
- 색상/emoji draft가 실제 Home state에 반영되는 시점처럼 `develop`과 동작이 다르면 `main` 동작을 보존한다.

### `develop`은 Circuit 구조 참고다

- Screen 내부 `BottomSheetState`, `DialogState`, Event 계약과 Presenter의 `rememberRetained` 사용 방식을 참고한다.
- Hilt `ActivityRetainedComponent`, `java.util.Locale`, Android resource, `InAppUpdateRepository`와 플랫폼 image effect는 가져오지 않는다.
- `develop`의 모든 Presenter state를 retained로 두는 방식은 복사하지 않는다.

## 3. 단계 경계

6-C에서도 새 Home Presenter는 runtime에 연결하지 않는다.

```text
현재 runtime
  LegacyHomeScreen → HomeRoute → HomeViewModel

6-C shadow graph
  HomeScreen → HomePresenter(read + edit/modal, runtime 미연결)
```

새 Presenter와 기존 ViewModel을 동시에 composition하면 repository collector와 mutation이 중복 실행될 수 있다. runtime 교체는 6-D 플랫폼 effect와 Home UI factory까지 준비된 뒤 한 번에 수행한다.

## 4. State 보존 정책

### 일반 `remember`

repository에서 다시 만들 수 있는 아래 상태는 PR #196과 같이 일반 `remember`를 유지한다.

- 반다라트 목록과 선택 상세
- main/sub/task 셀 트리
- loading과 완료 전환 여부
- 빈 목록 생성 guard

### `rememberRetained`

사용자가 modal에서 작성 중이며 화면 재생성 시 잃으면 안 되는 상태만 retained 처리한다.

- 현재 `BottomSheetState`
- 현재 `DialogState`
- cell 편집 draft에 포함된 title, description, due date, completion
- main cell 편집 draft에 포함된 emoji와 theme color
- date picker/emoji picker 펼침 상태

dropdown은 사용자 입력 draft가 아닌 일시적 UI이므로 일반 `remember`로 둔다. repository state 전체나 one-shot effect를 retained로 두지 않는다.

## 5. Screen 계약 확장

### State

PR #196의 읽기 State에 다음을 추가한다.

- `bottomSheet`
- `dialog`
- `isDropDownMenuOpened`
- 클릭한 cell type/data 또는 삭제 dialog에 필요한 동등 정보
- UI가 한 번 소비할 snackbar/toast effect

### Bottom sheet

- `Cell`: 원본 cell/Bandalart와 편집 draft, date/emoji picker 펼침 상태
- `BandalartList`: 현재 목록과 선택 ID
- `Emoji`: 현재 반다라트/셀 ID와 emoji

### Dialog

- 반다라트 삭제 확인
- cell type/title/id를 포함한 셀 삭제 확인

삭제 실행에 필요한 값을 dialog 자체에 포함할 수 있으면 별도 `clickedCellData` 복제를 줄인다. 다만 기존 Home UI 변경이 커지면 현재 State 형태를 우선 유지한다.

### Event

- modal/dropdown 열기·닫기
- 목록 항목 선택과 최대 5개 제한이 있는 새 반다라트 생성
- cell 클릭과 main goal 선행 검증
- title/description/due date/completion/emoji/color draft 갱신
- main/sub/task update 확정
- 반다라트/cell 삭제 확정
- main emoji 빠른 변경
- snackbar/toast effect 소비

공유·저장·capture·Complete·업데이트와 앱 버전 Event는 6-D에서 추가한다.

## 6. mutation 동작

### 생성

1. 현재 목록이 5개면 repository를 호출하지 않고 제한 toast effect를 노출한다.
2. 생성 성공 시 최근 ID와 완료 snapshot을 기록한다.
3. 생성된 상세를 선택하고 열린 bottom sheet를 닫는다.
4. 생성 snackbar effect를 노출한다.

빈 목록 자동 생성은 PR #196의 읽기 책임을 그대로 사용하고, 사용자 `Add` Event만 이번 단계에서 추가한다.

### cell 편집

- main goal이 비어 있을 때 sub/task 편집 진입을 막고 toast effect를 노출한다.
- title은 현재 KMP `Locale` 기준으로 한국어/일본어 15자, 그 외 24자를 유지한다.
- description은 1000자를 초과하면 직전 유효값을 유지한다.
- due date의 빈 문자열은 repository update 직전에 `null`로 정규화한다.
- main update에는 emoji와 main/sub color를 포함한다.
- task update에만 completion 값을 전달한다.
- update 성공 후 bottom sheet를 닫는다.

### 삭제

- 반다라트 삭제 후 완료 snapshot ID도 제거한다.
- cell 삭제는 선택한 cell ID만 repository에 전달한다.
- 삭제 완료 후 dialog와 bottom sheet를 함께 닫는다.
- 반다라트 삭제 시 dropdown도 닫는다.
- 성공 snackbar는 반다라트 삭제에만 현재 `main` 동작대로 노출한다.

## 7. one-shot effect 경계

생성 제한, main goal 선행 검증, 생성/삭제 안내는 6-C 동작 검증에 필요하므로 공통 UI effect로 모델링한다.

- effect는 retained state로 보존하지 않는다.
- UI가 `ConsumeEffect` Event로 명시적으로 지운다.
- `ImageBitmap`, Android Play API와 플랫폼 handler를 Screen 계약에 넣지 않는다.
- runtime이 아직 legacy이므로 실제 Snackbar/Toast UI 연결은 6-D runtime 교체 시 검증한다.

## 8. 테스트 계획

기존 `FakeBandalartRepository`를 별도 파일에서 확장하고 mutation 호출을 기록한다.

### 생성

- 5개 미만에서 생성, 최근 ID와 완료 snapshot 갱신, 상세 선택, modal 닫힘
- 5개에서 생성 미호출과 제한 effect

### modal/draft

- 목록/emoji/cell bottom sheet 열기·닫기
- 반다라트/cell 삭제 dialog 열기·취소
- cell draft의 title 길이, description 길이, due date와 picker 상태
- main draft의 emoji/color와 task completion 갱신
- `rememberRetained` 대상이 Presenter 재생성 후 유지되는지 Circuit test API로 가능한 범위에서 검증

### repository mutation

- main/sub/task update entity가 정확한 값으로 호출되는지 검증
- 반다라트 삭제 시 completion snapshot ID도 제거되는지 검증
- cell 삭제 ID와 modal 종료 검증
- 빠른 emoji update 호출과 bottom sheet 종료 검증

### 회귀

- PR #196의 최근 항목, fallback, 빈 목록, 셀 트리, 완료 전환 테스트 유지
- Metro graph의 Home Presenter factory 생성 테스트 유지

## 9. 검증 기준

- `feature:home` Android host Presenter/ViewModel test 통과
- `composeApp` Metro graph test 통과
- 이번 변경 Kotlin 파일 Spotless 포맷 통과
- `feature:home`, `composeApp` Detekt 통과
- PR CI의 전체 unit test, Android Lint, Android assemble, iOS simulator framework link 통과
- runtime 미연결이므로 이 단계에서 별도 UI 수동 검증은 하지 않는다.

## 10. 예상 구현 순서

1. `HomeScreen`에 edit/modal State/Event/effect 계약을 추가한다.
2. `HomePresenter`에 retained modal/draft와 일반 dropdown/effect 상태를 추가한다.
3. 생성, draft validation과 main/sub/task update를 연결한다.
4. 삭제 dialog와 repository delete를 연결한다.
5. fake repository mutation 기록과 Presenter test를 추가한다.
6. Metro graph와 기존 legacy 테스트를 함께 검증한다.
7. 마이그레이션 맵과 실제 troubleshooting을 갱신한다.

## 11. 롤백 경계

- 새 edit/modal 계약과 Presenter 로직은 shadow graph에서만 실행된다.
- repository interface, DB schema, DataStore key와 legacy runtime은 변경하지 않는다.
- 문제가 생기면 PR #196의 read-only Home Presenter 상태로 되돌릴 수 있다.

## 12. 참고 자료

- [Circuit retained state](https://slackhq.github.io/circuit/state/)
- [Circuit Presenter testing](https://slackhq.github.io/circuit/docs/testing/)
- `main`의 `HomeViewModel`, `HomeUiState/Action/Event`
- `origin/develop`의 `HomeScreen`, `HomePresenter`

## 13. 구현 및 로컬 검증 결과

- `circuit-retained`를 Home feature에 직접 선언하고 bottom sheet/dialog에만 `rememberRetained`를 적용했다.
- repository에서 다시 읽는 목록·상세·셀 트리와 loading/completion은 일반 `remember`를 유지했다.
- Cell bottom sheet가 원본과 편집 draft, cell type, date/emoji picker 상태를 함께 소유하도록 계약을 확장했다.
- 생성 제한, 목록/emoji/cell modal, title/description/due date/completion/emoji/color draft와 main/sub/task update를 Presenter Event로 연결했다.
- 반다라트/cell 삭제와 완료 snapshot 정리를 연결하고 삭제 대상은 dialog State에 포함했다.
- snackbar/toast는 플랫폼 문자열이나 UI를 참조하지 않는 공통 Effect 종류로만 추가했다.
- 기존 read test 5개와 legacy HomeViewModel test를 유지하고 edit/modal Presenter test 7개를 별도 파일로 추가했다.
- `:feature:home:testAndroidHostTest`, `:composeApp:testAndroidHostTest`가 통과했다.
- 변경 Kotlin 파일 Spotless IDE hook과 `:feature:home:detekt`, `:composeApp:detekt`가 통과했다.
- PR #197에서 전체 unit test, Android Lint, Android assemble, iOS Simulator Arm64 framework link가 통과했다.
- PR #197은 일반 merge로 `main`에 반영됐다.
