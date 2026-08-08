# Fluent Emoji 카테고리 탐색 전략

## 배경

Microsoft Fluent Emoji는 picker 컴포넌트가 아니라 asset과 metadata를 제공한다. 각 emoji의 `metadata.json`에는 `group`, `cldr`, `keywords`, `glyph`, `unicode`가 있고, 현재 프로젝트의 생성 pipeline은 이 `group`을 9개 `FluentEmojiCategory`로 이미 정규화한다.

현재 `FluentEmojiPicker`는 최근 사용 항목 뒤에 전체 300개 catalog를 한 grid로 이어 붙인다. 카테고리 filter와 한국어·영어·일본어 label resource는 남아 있지만, 이전의 제목·검색·text chip·전체 높이 picker를 기존 compact UI로 복원하는 과정에서 category selector도 함께 숨겨졌다. 300개를 세로 스크롤만으로 찾기 어려우므로 compact UI는 유지하면서 Teams처럼 category icon으로 목록을 전환한다.

## 목표

- 기존 6열·4행 grid 높이와 Fluent Color cell UI를 유지한다.
- grid 아래에 가로 스크롤 가능한 48dp category icon tab 한 줄을 제공한다.
- `모두`, 유효한 최근 사용이 있을 때만 `최근 사용`, Fluent metadata의 9개 group 순서로 노출한다.
- `모두`는 현재 동작처럼 최근 사용을 앞에 두고 나머지 300개를 중복 없이 표시한다.
- category를 선택하면 해당 group 항목만 표시하고 grid를 첫 항목으로 이동한다.
- category 이동만으로 현재 emoji 선택값이나 저장 callback을 변경하지 않는다.
- 각 tab은 선택 semantics와 한국어·영어·일본어 접근성 이름을 제공한다.

## 구현 경계

1. `FluentEmojiPicker`
   - category 선택은 picker 내부의 일시적인 UI element state이므로 `rememberSaveable`로 소유한다.
   - `LazyRow`와 `selectableGroup`으로 icon tab을 제공하고, 선택된 tab을 색상과 border로 구분한다.
   - catalog에 포함된 대표 Fluent Emoji를 tab icon으로 사용한다.
   - 실제 category tab click에서만 grid를 맨 위로 이동하고, configuration 복원 시 저장된 scroll 위치는 유지한다.
   - 같은 composition에서 유효했던 최근 목록이 비게 될 때만 `모두`로 정규화한다. configuration 복원 직후 DataStore의 일시적인 빈 초기값은 저장된 최근 category를 지우지 않는다.
2. `FluentEmojiCatalogTest`
   - 전체 view의 최근 사용 우선·중복 제거, 최근 tab 노출 조건, category별 부분집합, tab icon catalog 포함을 검증한다.

## 비범위

- 검색창·picker 제목·내부 닫기 버튼 복원
- catalog 300개, category taxonomy, 생성 pipeline 또는 asset 재생성
- Presenter, Room, DataStore, 최근 사용 저장 규칙 변경
- Unicode 저장 형식이나 기존 데이터 migration
- bottom sheet inset 또는 화면 높이 정책 변경

## 완료 조건

- 사용자가 category icon을 눌러 9개 Fluent metadata group과 최근 사용 목록을 직접 열 수 있다.
- 전체 view와 emoji 선택 callback의 기존 동작이 유지된다.
- 최근 사용이 비어 있으면 빈 tab이 노출되지 않는다.
- category 전환 뒤 목록이 항상 첫 항목부터 보인다.
- core UI host test와 정적 검사가 통과한다.

## 필수 수동 검증

현재 `core/ui`에는 Compose interaction test harness가 없으므로 다음 항목은 Android와 iOS picker에서 직접 확인한다.

- 각 icon tab을 누르면 해당 metadata group만 표시되고 선택 tab의 색상·border가 바뀐다.
- category 전환은 grid를 첫 항목으로 이동하지만 화면 회전·configuration 복원은 기존 category와 scroll 위치를 유지한다.
- TalkBack·VoiceOver가 tab을 선택 가능한 tab과 현지화된 category 이름으로 읽는다.
- 48dp tab touch target과 가로 스크롤이 큰 글자·라이트·다크 모드에서 겹치지 않는다.
- category 이동만으로 emoji 선택 callback이나 저장값이 바뀌지 않는다.
