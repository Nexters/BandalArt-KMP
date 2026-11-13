# 루틴(매일 새로 시작하기) 발견성 개선 전략

- 관련 이슈: #465
- 선행 기능: #393, [반다라트별 일일 완료 체크 초기화 전략](DAILY_COMPLETION_RESET_STRATEGY.md)
- 기준 브랜치: `feat/routine-discoverability` (`main`에서 분기)

## 문제

`매일 새로 시작하기`는 셀이 아니라 반다라트 단위 설정(`BandalartDBEntity.dailyResetEnabled`)이다. 현재 사용자가 이 기능을 알거나 상태를 확인할 수 있는 경로는 다음뿐이다.

| 경로 | 위치 | 한계 |
| --- | --- | --- |
| `⋮` → `루틴 설정` | `HomeHeader.kt`의 `HomeTitle`, `BandalartDropDownMenu.kt` | 아이콘만 보이는 overflow 메뉴 안에 있다. |
| 신규 기능 Balloon | `HomeHeader.kt`의 `RoutineSettingsTooltip`, 문자열 `home_routine_settings_tooltip` | 앱 전체에서 한 번만 표시한다. |
| `RoutineSettingsBottomSheet` | `HomeBottomSheets.kt` | 위 두 경로로만 열린다. |

Balloon 노출 조건(`HomePresenter`의 `showRoutineSettingsTooltip`)은 DataStore `routine_settings_tooltip_dismissed_v1`이 `false`이고 반다라트 데이터가 로드됐으며 바텀시트·다이얼로그·드롭다운·이미지 저장·보상형 광고가 모두 없을 때다. 다음 중 하나가 일어나면 영구 해제된다.

- Balloon 자체 터치, 바깥 터치, 뒤로가기 (`setDismissWhenClicked/TouchOutside/BackPressed`)
- `⋮` 열기 (`OpenDropDownMenu`)
- 루틴 설정 진입 (`OpenRoutineSettings`)

앱 첫 진입에서 셀을 한 번 누르기만 해도 안내가 사라진다. 이후에는 다음 두 가지를 확인할 수단이 없다.

1. 하루마다 완료 체크를 초기화할 수 있다는 사실 (꺼진 사용자)
2. 지금 보고 있는 반다라트가 매일 초기화되는지 (켜진 사용자). 다음 날 체크가 사라진 이유를 알기 어렵다.

## 성공 기준

- 루틴이 켜진 반다라트는 홈 화면에서 별도 조작 없이 그 상태가 보이고, 한 번의 탭으로 루틴 설정에 들어갈 수 있다.
- 루틴이 꺼진 사용자도 반다라트를 만들거나 메인 목표를 편집하는 흐름에서 이 설정을 만난다.
- 기존 `⋮` 메뉴, 루틴 설정 바텀시트, Balloon과 날짜·초기화 정책은 바꾸지 않는다.
- DB schema, DataStore key, backup schema는 바꾸지 않는다.

## 개선안 비교

### A. 홈 헤더 루틴 상태 칩

진행률·마감일 줄 오른쪽에 `매일 초기화` 칩을 표시한다. 루틴이 켜졌을 때만 보이며 탭하면 `OpenRoutineSettings`를 보낸다.

- 장점: 상태 확인 문제(2)를 직접 해결한다. 기존 이벤트를 재사용하므로 Presenter 변경이 없다.
- 한계: 꺼진 사용자에게는 보이지 않아 (1)을 해결하지 못한다. 꺼진 상태에도 칩을 보이면 모든 반다라트 헤더에 상시 노이즈가 생긴다.

### B. 메인 목표 편집 시트의 루틴 스위치

메인 셀 편집 바텀시트(`BandalartBottomSheet`, `CellType.MAIN`)의 마감일·메모 아래에 `루틴 설정` 섹션과 `매일 새로 시작하기` 스위치를 둔다. 반다라트 생성 직후와 목표 편집에서 반드시 거치는 화면이다.

- 장점: 마감일처럼 반다라트 성격을 정하는 화면에서 꺼진 사용자도 설정을 만난다(1).
- 결정 사항: 시트의 다른 필드처럼 draft로 다루고 `완료` 저장 시 반영한다. 즉시 반영하면 `닫기`로 나가도 저장되는 필드가 하나만 생겨 시트 전체 의미가 어긋난다. 변경 감지는 기존 `initialBandalartData != bandalartData` 비교를 그대로 쓴다.
- 저장: 메인 셀 갱신 뒤 값이 바뀐 경우에만 `BandalartRepository.setDailyResetEnabled`를 호출한다. anchor 날짜 정책(켜는 날은 초기화하지 않음)은 기존 경로를 그대로 따른다.
- 한계: 상태 확인(2)은 편집 시트를 열어야만 가능하다.

### C. 첫 사용 온보딩 또는 Balloon 재노출

온보딩 화면에 루틴 소개를 추가하거나, Balloon을 첫 100% 달성처럼 맥락이 맞는 시점에 다시 보여준다.

- 장점: 신규 사용자에게 기능 존재를 명시적으로 알린다.
- 한계: 온보딩은 이미 온보딩을 마친 기존 사용자에게 효과가 없다. Balloon 재노출은 새 DataStore 플래그와 기존 태스크 Balloon과의 순서 정책을 다시 설계해야 하며, 여전히 일회성 안내라 (2)를 해결하지 못한다.

## 결정

A와 B를 함께 적용하고 C는 보류한다.

- A는 켜진 사용자의 상태 확인, B는 꺼진 사용자의 발견을 맡아 서로 겹치지 않는다.
- 둘 다 기존 이벤트·repository API만 사용하므로 데이터 계층 변경이 없다.
- C는 A·B 반영 뒤에도 사용률이 낮을 때 다시 검토한다.

## 구현

### A. 헤더 칩 (`HomeHeader.kt`)

- `bandalartData.dailyResetEnabled`일 때 진행률 줄의 `Spacer(weight)` 뒤, `달성 완료!` 배지 앞에 둔다.
- `Icons.Default.Refresh`(드롭다운 `루틴 설정` 항목과 같은 아이콘)와 `home_daily_reset_badge` 문자열을 쓴다.
- outline 테두리의 pill 모양으로 `달성 완료!` 배지와 높이·글자 크기를 맞추고, 완료 배지(메인 색 채움)와 구분한다.
- 탭 시 `HomeScreen.Event.OpenRoutineSettings`. `Role.Button`과 텍스트 라벨로 접근성 이름을 제공한다.

### B. 편집 시트 스위치

- `HomeScreen.Event.UpdateDailyResetDraft(enabled)`를 추가하고 Presenter가 `BottomSheetState.Cell.bandalartData.dailyResetEnabled`만 바꾼다.
- `BandalartBottomSheet`의 `CellType.MAIN`에서 메모 아래에 `routine_settings_title` 제목, `routine_settings_daily_reset_title` 텍스트, 기존 `달성 여부`와 같은 스타일의 `Switch`를 둔다. 설명 문구는 `routine_settings_daily_reset_description`을 재사용한다.
- `saveCell()`의 `CellType.MAIN` 경로에서 메인 셀 저장 뒤 `bandalartData.dailyResetEnabled != initialBandalartData.dailyResetEnabled`이면 `setDailyResetEnabled`를 호출한다.

### 문자열

| key | ko | en | ja |
| --- | --- | --- | --- |
| `home_daily_reset_badge` | 매일 초기화 | Resets daily | 毎日リセット |

## 테스트

- Presenter: 메인 편집 시트에서 스위치 draft 변경 후 저장하면 `setDailyResetEnabled(id, true)`가 한 번 호출된다.
- Presenter: 스위치를 바꾸지 않고 저장하면 호출되지 않는다.
- 기존 루틴 설정·Balloon 테스트가 그대로 통과한다.
- UI 수동 확인: Android에서 칩 표시·탭 진입과 편집 시트 스위치 저장을 확인한다.

## 검증 명령

- `:feature:home:testAndroidHostTest`
- `spotlessCheck`, `detekt`
- `:composeApp:assembleDebug`, iOS simulator 빌드
