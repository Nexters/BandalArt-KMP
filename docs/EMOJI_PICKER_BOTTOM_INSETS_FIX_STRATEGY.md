# 이모지 선택 바텀시트 하단 inset 수정 전략

## 문제

Android 3-button navigation 환경에서 이모지 선택 바텀시트의 마지막 아이콘 행이 잘리고, 하단 navigation bar 영역이 앱의 시트 색상과 다른 연분홍색으로 보인다.

## 원인

- Material 3 `ModalBottomSheet`는 기본 `windowInsets`로 `safeDrawing`의 상·하단 inset을 이미 적용한다.
- `FluentEmojiPicker`가 `NavigationBarHeightDp` 높이의 별도 Spacer를 추가해 하단 inset이 중복 적용된다.
- `ModalBottomSheet`의 기본 container 색상과 picker의 `MaterialTheme.colorScheme.surface`가 달라 자동 inset 영역에 Material 3 기본 container 색상이 노출된다.

## 수정 범위

1. `FluentEmojiPicker`의 수동 navigation bar Spacer를 제거한다.
2. 이모지 `ModalBottomSheet`의 container 색상을 앱의 `MaterialTheme.colorScheme.surface`로 지정한다.
3. Android 앱 버전을 `2.2.16 (20216)`으로 올리고 Internal 출시 노트를 갱신한다.

검색·카테고리·최근 사용·이모지 선택 상태와 grid 크기 계산은 변경하지 않는다.

## 검증 기준

- 3-button navigation 환경에서 네 번째 표시 행이 잘리지 않는다.
- gesture navigation과 3-button navigation 모두 하단 시스템 영역과 시트 배경색이 자연스럽게 이어진다.
- light/dark theme에서 navigation bar 영역이 각각 시트 surface 색상을 따른다.
- 이모지 grid 스크롤과 선택 동작이 기존과 동일하다.
- CI의 code quality, Android lint/build, unit test, iOS framework 검증이 모두 통과한다.
- 머지 후 Play Internal 트랙에 `20216`이 반영된다.
