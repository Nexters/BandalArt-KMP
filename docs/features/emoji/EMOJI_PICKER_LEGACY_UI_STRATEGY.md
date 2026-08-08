# 이모지 선택기 기존 UI 복원 전략

## 배경

Fluent Emoji 300개 catalog를 도입하면서 기존의 단순한 6열 이모지 grid가 제목, 닫기 버튼, 검색창, 카테고리 chip과 화면 대부분을 차지하는 picker로 변경됐다. 기능은 확장됐지만 반다라트의 기존 ModalBottomSheet와 시각적 밀도·구조가 크게 달라졌다.

이번 작업은 Fluent Emoji resource와 Unicode 저장 방식을 유지하면서 선택 UI만 기존 형태로 되돌린다.

## 목표

- 기존 picker의 6열 카드 grid, padding, 12dp corner, 24dp 이모지와 1dp 선택 테두리를 복원한다.
- 제목, 닫기 버튼, 검색 field, 카테고리 chip과 선택 check badge를 노출하지 않는다.
- 기존 4개 행에 해당하는 높이를 viewport로 사용하고 300개 catalog는 세로 스크롤로 탐색한다.
- 최근 사용 항목은 별도 탭을 추가하지 않고 같은 grid의 앞쪽에 배치한다.
- ModalBottomSheet가 화면 높이의 92%를 강제로 차지하지 않고 콘텐츠 높이에 맞게 표시되도록 복원한다.
- 선택 즉시 저장/편집 draft 반영, Unicode DB 저장과 legacy Unicode fallback은 변경하지 않는다.

## 구현 범위

1. `FluentEmojiPicker`
   - 최근 사용 항목과 나머지 300개 catalog를 `LazyVerticalGrid(GridCells.Fixed(6))`로 표시한다.
   - 화면 너비와 기존 padding/gap을 기준으로 cell 크기와 4개 행 viewport 높이를 계산한다.
   - 기존 Card/Box 기반 선택 스타일을 적용한다.
   - grid 자체만 세로 스크롤되도록 한다.
2. `BandalartEmojiBottomSheet`
   - `fillMaxHeight(0.92f)`를 제거하고 기존 `wrapContentSize()`를 적용한다.
3. 호출부
   - 기존 UI에 없던 picker 내부 닫기 action을 제거한다.
   - 외부 탭, back 또는 sheet dismiss 동작은 유지한다.

## 비범위

- Fluent catalog, resource pipeline 또는 Unicode 저장 형식 변경
- 기존 Unicode를 다른 이모지로 migration
- Presenter, Room, DataStore 또는 최근 사용 저장 로직 변경
- 이모지 catalog 개수 변경
- 인접 화면 리팩터링

## 검증

- catalog 300개 및 Unicode-resource mapping 단위 테스트 통과
- Home Presenter의 이모지 선택/저장 관련 테스트 통과
- ktlint 통과
- 수동 확인 항목
  - ModalBottomSheet가 기존과 유사한 높이·padding·6열 grid로 표시됨
  - 4개 행 이후 세로 스크롤 가능
  - 선택한 이모지에 기존 1dp outline 표시
  - catalog 밖 legacy Unicode가 기존 화면에서 시스템 이모지로 유지됨
  - sheet dismiss와 편집 draft 저장 의미 유지

## 완료 조건

- Fluent Emoji를 유지하면서 사용자가 기존 반다라트 picker와 같은 UI로 인식한다.
- 300개 항목 때문에 sheet가 화면 대부분을 점유하지 않고 grid 내부에서만 스크롤된다.
- 기존 저장 데이터와 선택 event 흐름에 회귀가 없다.
