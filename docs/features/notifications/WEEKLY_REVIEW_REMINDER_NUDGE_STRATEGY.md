# 주간 회고 알림 켜기 유도 전략

- 작성일: 2026-11-10
- 관련 이슈: [#470 주간 회고 알림 켜기 유도 장치 추가](https://github.com/Nexters/BandalArt-KMP/issues/470)
- 기준 branch: `feat/weekly-review-reminder` (`1b14994b`, #467)
- 참고: [주간 회고 알림과 진행률 요약](WEEKLY_REVIEW_REMINDER_STRATEGY.md), [연속 달성과 위젯 진행률](../home/COMPLETION_STREAK_WIDGET_PROGRESS_STRATEGY.md)

## 배경

주간 회고 알림은 기본값이 꺼짐이고, 켜는 경로가 설정 시트의 토글뿐이다. 설정 시트를 열지 않는 사용자는 기능이 있는지도 모른다. 기본값은 그대로 두고(권한 신뢰 문제, #467 전략 참고), 사용자가 관심을 보이는 두 시점에 켜기를 권한다.

## 목표

1. 주간 회고 시트를 본 사용자에게, 알림이 꺼져 있으면 시트 안에서 바로 켤 수 있는 배너를 보여 준다.
2. 처음 연속 달성(streak 1일 이상)을 이룬 사용자에게 홈에서 한 번만 알림 켜기를 권한다.

## 결정

| 항목 | 결정 |
|---|---|
| 권한 흐름 | 두 경로 모두 기존 `HomeScreen.Event.ConfirmWeeklyReviewReminderPermission`을 보낸다. Presenter의 `confirmReminderPermission`이 상태 확인, 권한 요청, 시스템 설정 이동, 설정에서 돌아온 뒤 켜기를 그대로 처리한다 |
| 사전 확인 다이얼로그 | 넣지 않는다. 설정 토글은 스위치 하나라 의미를 설명하는 다이얼로그가 필요했지만, 배너와 권유 다이얼로그는 문구 자체가 그 설명이다. 한 번 더 확인하게 하면 이탈만 늘어난다 |
| 배너 노출 조건 | `weeklyReviewReminderEnabled == false`. 켜지면 같은 시트 안에서 배너가 사라진다 |
| 권유 노출 조건 | `completionStreak >= 1`, 권유 플래그 `false`, 저장된 알림 설정 `false`, 홈 위에 바텀시트·다이얼로그·드롭다운·이미지 요청·보상형 광고 요청이 없을 때 |
| 권유 형태 | 홈 다이얼로그(`HomeScreen.DialogState.WeeklyReviewReminderNudge`). 기존 `BandalartActionAlertDialog`를 쓴다 |
| 권유 플래그 | DataStore `weekly_review_reminder_nudge_shown_v1` (Boolean, 기본 `false`). 클라우드 백업에 넣지 않는다 |
| 플래그 기록 시점 | 다이얼로그를 띄우는 순간 기록한다. 응답 전에 앱이 종료돼도 다시 권하지 않는다("한 번만") |
| 이미 켜져 있을 때 | 첫 streak 시점에 알림이 이미 켜져 있으면 다이얼로그 없이 플래그만 기록한다. 이후 사용자가 직접 끈 경우에는 다시 권하지 않는다 |

### 노출 조건을 Presenter에서 다시 읽는 이유

`weeklyReviewReminderEnabled`와 권유 플래그는 각각 `collectAsState(initial = …)`로 모으므로, 첫 프레임에는 DataStore 값이 아직 반영되지 않았을 수 있다. 권유 플래그의 초기값을 `true`로 두어 판단을 미루고, 조건이 맞으면 `LaunchedEffect` 안에서 `settingsRepository.weeklyReviewReminderEnabled.first()`를 다시 읽은 뒤 결정한다. 마지막 판단은 저장된 값을 기준으로 한다.

### 다른 홈 UI와의 순서

- 다이얼로그가 떠 있으면 기존 툴팁 노출 조건(`dialog == null`)에 따라 툴팁은 숨겨진다. 다이얼로그를 닫으면 다시 보인다.
- 바텀시트(셀 편집 등)가 열려 있거나 완료 화면으로 가는 이미지 요청이 있으면 권유를 미룬다. 조건이 풀리는 시점에 `LaunchedEffect` key가 바뀌어 다시 판단한다.

## UI

### 회고 시트 배너

- 위치: 헤더 바로 아래, 요약 위.
- 구성: 알림 아이콘 + 제목 "매주 일요일 저녁에 알림 받기" + 보조 문구 "한 주를 돌아볼 시간을 알려 드려요" + 텍스트 버튼 "켜기". 행 전체를 눌러도 같은 동작이다.
- 배경: `surfaceVariant`, 모서리 12dp. 기존 시트 여백(가로 20~24dp)에 맞춘다.

### 홈 권유 다이얼로그

- 아이콘: `ic_notifications_outlined`
- 제목: "연속 달성을 시작했어요"
- 본문: "매주 일요일 저녁 8시에 한 주를 돌아보는 알림을 보내 드릴까요? 설정에서 언제든 끌 수 있어요."
- 확인 "알림 받기" → 다이얼로그를 닫고 `ConfirmWeeklyReviewReminderPermission`
- 취소 "다음에" → `DismissDialog`

## 변경 범위

- `core/datastore` `BandalartDataStore`: 키, `weeklyReviewReminderNudgeShown` Flow, `markWeeklyReviewReminderNudgeShown()`
- `core/domain` `SettingsRepository`, `core/data` `DefaultSettingsRepository`: 같은 계약 노출
- `feature/home`
  - `HomeScreen.DialogState.WeeklyReviewReminderNudge`
  - `HomePresenter`: 권유 `LaunchedEffect`, 확인 이벤트에서 권유 다이얼로그 닫기
  - `HomeDialogs`: 권유 다이얼로그
  - `HomeBottomSheets`, `WeeklyReviewBottomSheet`: `weeklyReviewReminderEnabled` 전달과 배너
- 문자열: `core/designsystem` composeResources ko(기본)/en/ja

## 테스트

`HomePresenterWeeklyReviewTest`에 추가한다.

- streak이 0에서 1이 되면 권유 다이얼로그가 뜨고 플래그가 기록된다.
- 플래그가 이미 `true`면 streak이 생겨도 다이얼로그가 뜨지 않는다.
- 알림이 이미 켜져 있으면 다이얼로그 없이 플래그만 기록된다.
- 권유 다이얼로그에서 확인하면 다이얼로그가 닫히고 권한 흐름을 거쳐 알림이 켜진다.
- 바텀시트가 열려 있는 동안에는 권유하지 않고, 닫힌 뒤 권한다.

`SettingsRepositoryTest`와 `BandalartDataStoreTest`에 플래그 노출·저장 테스트를 추가한다.

## 범위 밖

- 권한이 회수된 상태(`enabled == true`지만 권한 없음)에서의 배너 노출. 설정 시트처럼 권한 상태까지 반영하려면 시트를 열 때 상태를 새로 읽어야 하는데, #467에서 알림 채널 상태 판단을 고치는 작업이 진행 중이라 그 결과가 정리된 뒤 판단한다.
- 권유 노출·전환 분석 이벤트. 이 앱에는 아직 analytics 계층이 없다.
- 과거 완료 기록 backfill. 업데이트 직후에는 기록이 없으므로 업데이트 후 첫 완료가 "첫 연속 달성"이 된다.
