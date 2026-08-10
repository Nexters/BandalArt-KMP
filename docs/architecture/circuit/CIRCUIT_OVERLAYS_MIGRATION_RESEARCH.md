# Circuit Overlays 마이그레이션 검토

- 검토일: 2026-08-10
- 대상 버전: Circuit `0.35.1`
- 결론: **전면 마이그레이션은 보류하고 백로그로 관리한다.**

## 결론

현재 Home의 다이얼로그와 바텀시트를 Circuit Overlays로 전면 이전해도 상태와 이벤트의 복잡도가 충분히 줄지 않는다. 반면 다음 비용과 동작 차이가 생긴다.

- `OverlayHost.show()`는 UI context 전용이라 Presenter가 직접 모달을 띄울 수 없다. 현재 Presenter 소유 상태를 완전히 대체하지 못하고 UI에 `OverlayEffect`와 결과 전달 계층이 추가된다.
- 한 `OverlayHost`는 `Mutex`로 한 번에 하나만 표시한다. 설정 바텀시트 위 알림 확인 다이얼로그, 셀 편집 바텀시트 위 삭제 확인 다이얼로그처럼 현재 가능한 겹침을 유지하려면 nested host 또는 순차 전환 정책이 필요하다.
- Overlay는 프로세스 종료 후 복원되지 않고 내비게이션 백스택에도 참여하지 않는다.
- 셀 편집 draft, 날짜 선택기, 이모지 선택기와 보상형 광고 생성 상태는 Overlay로 옮겨도 사라지지 않는다. 상태의 위치만 바뀔 가능성이 크다.
- 현재 nullable modal state와 Presenter event 테스트 방식은 Circuit 공식 Overlay 테스트 recipe가 권장하는 형태와 이미 일치한다.

따라서 지금은 기존 구조를 유지한다. Circuit Overlays는 여러 화면이 공통 모달을 요청하거나, 순수한 일회성 request/result 흐름이 반복되어 typed suspend result가 실제 중복을 제거할 때 다시 검토한다.

## 현재 구조

### 앱 root

`BandalartApp`은 `NavigableCircuitContent`를 직접 렌더링하며 `ContentWithOverlays`나 `circuit-overlay` 의존성을 사용하지 않는다.

### Home modal state

`HomeScreen.State`가 아래 상태를 노출한다.

- `bottomSheet: BottomSheetState?`
- `dialog: DialogState?`

`HomePresenter`는 두 값을 `rememberRetained`로 소유한다. `HomeBottomSheets`와 `HomeDialogs`는 sealed state를 `when`으로 분기해 Compose Material modal을 렌더링한다.

| 종류 | 현재 상태 | 특징 | Overlay 적합도 |
|---|---|---|---|
| 반다라트 삭제 확인 | `DialogState.BandalartDelete` | 단순 확인/취소 결과 | 높음 |
| 셀 삭제 확인 | `DialogState.CellDelete` | 셀 편집 바텀시트 위에서 표시 | 단독으로는 높지만 중첩 때문에 낮아짐 |
| 보상형 생성 확인 | `DialogState.RewardedCreate` | 광고·생성 coordinator와 결합 | 중간 |
| 셀 편집 | `BottomSheetState.Cell` | 편집 draft, 날짜·이모지 picker, 저장·삭제 | 낮음 |
| 반다라트 목록/생성 | `BottomSheetState.BandalartList` | 내부 화면 전환 상태 포함 | 낮음 |
| 대표 이모지 | `BottomSheetState.Emoji` | 선택 결과형에 가까움 | 중간 |
| 설정 | `BottomSheetState.Settings` | 여러 설정 mutation과 하위 확인 dialog | 낮음 |

`SettingsBottomSheet`의 마감일 알림 확인 dialog만 로컬 `remember` 상태로 관리한다. 이 한 지점의 상태 소유권을 정리할 필요는 있지만, 전면 Overlay 마이그레이션을 정당화할 정도의 중복은 아니다.

## Circuit 0.35.1의 실제 성질

### 잘 맞는 경우

Circuit은 Overlay를 bottom sheet, dialog, tooltip 같은 일회성 request/result 흐름으로 설명한다. `OverlayHost.show()`는 typed result가 올 때까지 suspend하므로 선택기나 확인 dialog를 호출 함수처럼 다룰 수 있다.

앱 root를 `ContentWithOverlays`로 감싸면 `LocalOverlayHost`와 `LocalOverlayState`가 제공된다.

### 현재 구조와 충돌하는 제한

1. **UI 전용**

   Circuit 소스는 `show()`를 UI context에서만 호출하고 Presenter에서는 호출하지 말라고 명시한다. Presenter 중심 UDF를 유지하려면 modal 요청 nullable state와 결과 Event가 여전히 필요하다.

2. **동시 표시가 아니라 직렬화**

   기본 host는 active overlay 하나만 보유하며 추가 `show()`를 `Mutex`에 대기시킨다. 현재처럼 bottom sheet를 유지한 채 dialog를 그 위에 표시하는 동작은 동일 host에서 직접 표현되지 않는다.

3. **복원과 백스택 부재**

   공식 비교표에서 Overlay는 process death 생존과 back stack 참여를 지원하지 않는다. 현재 `rememberRetained`보다 무조건 강한 상태 모델이 아니다.

4. **테스트 구조를 없애지 않음**

   공식 recipe도 실제 `OverlayHost`를 Presenter 단위 테스트에 세우지 말고 nullable state로 요청 여부와 결과 Event를 검증하라고 권장한다. 이는 현재 Home 테스트 모델과 같다.

## 구조적 효용 평가

### 기대 이점

- 확인/선택 결과를 sealed result로 통일할 수 있다.
- 앱 전체의 overlay 배치와 애니메이션 host를 한곳에 둘 수 있다.
- 여러 화면에서 같은 overlay를 사용할 때 호출부가 `showX(): Result` 형태로 작아질 수 있다.

### 현재 얻기 어려운 이유

- modal 호출 화면이 사실상 Home 하나라 공통 host의 재사용 이점이 작다.
- `HomeScreen.State`를 없애기보다 nullable 요청 state와 `OverlayEffect` adapter를 함께 유지할 가능성이 높다.
- 셀 편집과 설정은 단순 result UI가 아니라 장시간 유지되는 편집 surface다.
- 중첩 UX를 위해 host를 다시 나누면 작은 인터페이스 뒤에 복잡성을 숨기는 deep module이 아니라 host와 adapter가 늘어나는 shallow layer가 된다.
- 현재 코드에는 Overlay 기반 구현을 공유할 두 번째 화면이나 두 번째 adapter가 없다. 아직은 추상화할 안정된 seam이 아니다.

## 권장안

### 지금

- 기존 `HomeScreen.BottomSheetState`와 `DialogState`를 유지한다.
- 모달별 UI는 지금처럼 공통 디자인 컴포넌트로 통일한다.
- 알림 확인 dialog의 로컬 상태는 실제 복원 버그나 테스트 공백이 확인될 때 Presenter nullable state로 올리는 작은 작업으로 분리한다.
- 셀 편집 바텀시트는 Overlay pilot 대상으로 선택하지 않는다.

### 재검토 조건

아래 중 하나 이상이 실제로 발생하면 Circuit Overlays 도입 이슈를 실행 후보로 올린다.

- Home 외 두 번째 화면에서도 동일한 확인/선택 modal이 필요하다.
- modal 요청/결과 boilerplate가 세 곳 이상 반복된다.
- typed suspend result가 복수 callback과 Event를 실제로 제거한다.
- 앱 전역 overlay 애니메이션이나 z-order 정책을 한 host에서 관리할 필요가 생긴다.
- nullable modal state와 Event 증가가 Presenter 분해 또는 테스트를 지속적으로 방해한다.

### Pilot 순서

재검토 시에는 중첩되지 않고 편집 draft가 없는 하나의 dialog만 pilot으로 옮긴다.

1. 반다라트 삭제 확인 dialog
2. 대표 이모지 선택 bottom sheet
3. 결과와 테스트가 단순해졌는지 평가
4. 이점이 측정될 때만 다른 modal로 확대

셀 편집, 설정, 보상형 생성은 마지막까지 기존 Presenter 소유 상태를 유지한다.

## 백로그 이슈 초안

### 제목

`refactor: Circuit Overlays 도입 조건 검증과 Home modal pilot`

### 배경

Home이 여러 dialog와 bottom sheet를 nullable state로 관리한다. Circuit Overlays의 typed suspend result와 공통 host가 modal 요청/결과 boilerplate를 줄이는지 검증한다. 현재는 UI context 제한, 단일 active overlay, process death 비복원 때문에 전면 이전하지 않는다.

### 시작 조건

- [ ] Home 외 화면에서 공통 modal 요구가 생겼다.
- [ ] 동일 request/result event 흐름이 세 곳 이상 반복된다.
- [ ] 현재 상태/Event 구조로 인한 실제 유지보수 또는 테스트 문제가 기록됐다.

위 조건 중 하나도 충족하지 않으면 이슈를 실행하지 않는다.

### 범위

- `circuit-overlay` 의존성과 root `ContentWithOverlays` pilot
- 중첩되지 않는 dialog 하나의 typed result 모델
- 기존 Presenter 상태/Event 테스트와 Overlay 적용 후 테스트 비교
- Android/iOS dismiss, back, rotation/background 복귀 동작 확인

### 제외

- 셀 편집 bottom sheet
- 설정 bottom sheet와 그 위의 확인 dialog
- 보상형 광고 생성 coordinator
- 모든 modal의 일괄 이전

### 완료 조건

- [ ] pilot에서 상태/Event 또는 중복 코드가 실제로 감소한다.
- [ ] 기존 modal UX와 접근성을 유지한다.
- [ ] process death와 중첩 제약을 문서화한다.
- [ ] 이점이 없으면 pilot을 되돌리고 기존 구조 유지 결정을 기록한다.

## 근거

- [Circuit 0.35.1 `OverlayHost`와 단일 active overlay 구현](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-overlay/src/commonMain/kotlin/com/slack/circuit/overlay/Overlay.kt#L24-L104)
- [Circuit 0.35.1 `ContentWithOverlays`](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/circuit-overlay/src/commonMain/kotlin/com/slack/circuit/overlay/ContentWithOverlays.kt#L38-L94)
- [Circuit Overlay와 PopResult 비교](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/docs/docs/overlays.md#L73-L88)
- [Circuit Overlay 테스트 recipe](https://github.com/slackhq/circuit/blob/2b5819a716a0fa9861501ba35aa51cb64068c366/docs/recipes/test-an-overlay.md#L3-L9)
- `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/HomeCircuitScreen.kt`
- `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/presenter/HomePresenter.kt`
- `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/HomeBottomSheets.kt`
- `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/HomeDialogs.kt`
- `feature/home/src/commonMain/kotlin/com/nexters/bandalart/feature/home/ui/settings/SettingsBottomSheet.kt`
