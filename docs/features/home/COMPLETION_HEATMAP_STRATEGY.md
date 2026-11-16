# 완료 기록 히트맵(잔디) 전략

- 작성일: 2026-11-09
- 관련 이슈: [#466 완료 기록 히트맵(잔디) 표시](https://github.com/Nexters/BandalArt-KMP/issues/466)
- 선행 작업: #463 연속 달성(PR #464), #462 주간 회고 시트
- 참고: [연속 달성과 위젯 진행률](COMPLETION_STREAK_WIDGET_PROGRESS_STRATEGY.md), [주간 회고 알림과 진행률 요약](../notifications/WEEKLY_REVIEW_REMINDER_STRATEGY.md)

## 목표

- GitHub 잔디처럼 날짜별 완료 수를 색 농도로 보여 줘서, 연속 일수 숫자만으로는 보이지 않는 지난 몇 달의 흐름을 한눈에 보게 한다.
- #463의 `cell_completion_records`와 #462의 기간 조회 DAO `getCompletionRecordsBetween`를 그대로 쓴다. 새 테이블, 마이그레이션, 백업 변경은 없다.

## 선행 작업 상태와 쌓는 순서

- 작성 시점에 #464는 열려 있고 #462는 구현 중이다. 이 작업은 #462 브랜치(`feat/weekly-review-reminder`) 위에 쌓는다. #462가 #464 위에 있으므로 두 선행 작업을 모두 포함한다.
- PR base는 `feat/weekly-review-reminder`로 두고, 선행 PR이 머지되면 `main`으로 바꾼다.

## 표시 위치

| 후보 | 장점 | 단점 |
|---|---|---|
| 주간 회고 시트 | "돌아보기"라는 목적이 같다. #462가 만든 시트·열기 경로(설정 행, 알림 탭)를 그대로 쓴다. 기록이 끊긴 사용자도 항상 열 수 있다 | 시트가 길어진다. 설정 안쪽에 있어 찾기 어렵다 |
| 별도 통계 화면 | 공간이 넉넉하고 이후 통계를 더 붙이기 쉽다 | 새 Circuit Screen, 네비게이션, 진입 버튼이 필요하다. 보여 줄 통계가 히트맵 하나뿐인 지금은 과하다 |
| 홈 헤더 스트릭 탭 | "스트릭 → 잔디" 연상과 바로 맞는다. 홈에서 한 번에 열린다 | 연속 일수가 0이면 헤더에 표시가 없어(#463 정책) 진입점이 사라진다. 12sp 텍스트라 터치 영역이 작다 |

결정: 히트맵은 **주간 회고 시트의 한 섹션**으로 넣고, **홈 헤더의 `🔥 N일 연속`을 누르면 같은 시트를 연다**.

- 시트는 기록 유무와 관계없이 설정의 "이번 주 회고 보기"로 항상 열 수 있다. 헤더 탭은 연속 달성 중인 사용자를 위한 지름길이다.
- 별도 통계 화면은 통계가 늘어날 때 다시 검토한다.

## 기간과 격자

- 기간: 이번 주를 포함한 **최근 16주**. `start = weekStartOf(today) - 15주`, `end = today`.
  - 16주는 가장 좁은 화면(폭 320dp)에서도 셀을 10dp 이상으로 그릴 수 있는 상한이다. 12주보다 계절 단위 흐름(약 4개월)이 보인다.
- 격자: 열이 주(왼쪽이 과거), 행이 요일(위가 월요일)이다. 주 시작은 #462와 같은 ISO 월요일이다.
- 마지막 열은 이번 주다. 오늘 이후 날짜 칸은 그리지 않는다.
- 셀 크기는 시트 폭에 맞춰 계산하고 최대 16dp, 간격 3dp로 둔다. 넓은 화면(iPad, 가로)에서는 최대 크기로 그리고 가운데 정렬한다.
- 월·요일 축 라벨은 두지 않는다. 대신 제목 아래에 기간(`7월 27일 ~ 11월 9일` 형식, #462의 기간 문자열 재사용)을 표시한다.

## 집계 정의

- 하루의 완료 수 = 그날의 `cell_completion_records` 건수(셀당 하루 1건). #462의 "이번 주 완료 수"와 같은 기준이라 마지막 열의 합은 시트 위쪽 이번 주 합계와 같다.
- 반다라트 구분 없이 앱 전체 기준이다. 삭제된 반다라트의 기록도 센다(#462 합계와 같은 정책).
- 요약 수치: 기간 안의 완료한 날 수(`DISTINCT completedDate`)와 총 완료 수.
- 과거 이력은 #463 이후부터만 있다(backfill 없음). 업데이트 직후에는 대부분 빈 칸이다.

### 색 단계

GitHub처럼 0 + 4단계, 5가지 색을 쓴다. 단계 경계는 사용자별 최댓값 비율이 아닌 **고정 기준**으로 둔다. 같은 날의 색이 다른 날 기록에 따라 바뀌지 않고, 단위 테스트로 고정할 수 있기 때문이다.

| 단계 | 하루 완료 수 |
|---|---|
| 0 | 0 |
| 1 | 1 |
| 2 | 2–3 |
| 3 | 4–6 |
| 4 | 7 이상 |

- 기준은 반다라트 하나의 태스크가 하루에 보통 몇 개 체크되는지(매일 초기화 반다라트 기준 3~8개)를 보고 정했다.
- 색은 앱 기본 메인 색 `MainColor(#3FFFBA)` 계열의 민트 램프다. 0단계는 테마의 `surfaceVariant`.
  - Light: `#C5FFEA`, `#3FFFBA`, `#12C98A`, `#0B8A5E`
  - Dark: `#124A39`, `#16805D`, `#25C48D`, `#3FFFBA`
- 반다라트별 색(`mainColor`)은 쓰지 않는다. 앱 전체 기록이라 특정 반다라트 색을 고를 근거가 없다.

## 도메인과 데이터

`core/domain`

- `CompletionHeatmap(startDate, endDate, days: List<CompletionHeatmapDay>, activeDayCount, completedCount)`
- `CompletionHeatmapDay(date, count, level)`. `days`는 `startDate`부터 `endDate`까지 빈 날 없이 날짜순이다.
- 순수 함수
  - `completionHeatmapLevel(count): Int` — 위 색 단계.
  - `calculateCompletionHeatmap(records, today, weeks = 16): CompletionHeatmap` — 기간 밖 기록은 버리고 날짜별로 센다.
- `BandalartRepository.getCompletionHeatmap(): CompletionHeatmap`. `getCompletionRecordsBetween(start, today)` 한 번으로 읽는다. #462의 `getWeeklyReviewSummary()`와 같이 `applyDueDailyResets()` 뒤에 읽는다.

## 홈 UI

- `HomeScreen.BottomSheetState.WeeklyReview`에 `heatmap: CompletionHeatmap`을 추가한다. Presenter는 시트를 열 때 요약과 함께 한 번 읽는다(시트를 연 채 바뀌는 경우는 #462처럼 고려하지 않는다).
- 시트 순서: 헤더 → 이번 주 합계(또는 빈 상태) → 반다라트별 행 → 구분선 → 히트맵 섹션.
  - 히트맵 섹션: 제목 "최근 16주 달성 기록", 기간, "N일 달성 · 총 M개 완료", 격자, 오른쪽 아래 범례(`적게 □□□□□ 많이`).
  - 이번 주 기록이 없어도 히트맵 섹션은 보여 준다. 지난주까지의 기록이 있을 수 있다.
  - 내용이 길어지므로 시트 본문에 세로 스크롤을 단다.
- 홈 헤더: `completionStreak > 0`일 때 보이는 `🔥 N일 연속` 텍스트를 누르면 `HomeScreen.Event.OpenWeeklyReview`를 보낸다. 터치 영역은 세로 패딩으로 조금 넓히되 헤더 높이를 크게 바꾸지 않는 선에서 둔다.

## 접근성

- 색만으로 정보를 전달하지 않는다. 같은 정보의 핵심(완료한 날 수, 총 완료 수)을 텍스트로 함께 보여 준다.
- 격자 112칸을 각각 포커스하게 하지 않는다. 스크린리더로 칸을 하나씩 넘기는 것은 쓸모가 없다.
  - 히트맵 섹션은 하나의 노드로 합친다. 격자와 범례는 semantics에서 지우고, 제목·기간·요약 텍스트를 읽는다.
  - 결과 예: "최근 16주 달성 기록, 7월 27일 ~ 11월 9일, 34일 달성 · 총 80개 완료".
- 헤더 스트릭 텍스트는 버튼 역할과 클릭 라벨("이번 주 회고 보기", #462 문자열 재사용)을 붙인다.
- 단계별 색 대비: 0단계와 1단계는 대비가 낮다(GitHub와 같은 한계). 범례와 요약 텍스트로 보완한다.
- 고정 `fontScale = 1f` 테마 정책을 그대로 따른다.

## 문구

`core/designsystem` compose resource에 둔다.

| 키 | ko | en | ja |
|---|---|---|---|
| `weekly_review_heatmap_title` | 최근 %1$d주 달성 기록 | Last %1$d weeks | 直近%1$d週間の達成記録 |
| `weekly_review_heatmap_summary` | %1$d일 달성 · 총 %2$d개 완료 | Active %1$d days · %2$d completed | %1$d日達成 · 合計%2$d個完了 |
| `weekly_review_heatmap_less` | 적게 | Less | 少 |
| `weekly_review_heatmap_more` | 많이 | More | 多 |

## 테스트

`docs/architecture/kmp/KMP_TESTING_GUIDE.md` 기준

- `core/domain` `androidHostTest`
  - `completionHeatmapLevel`: 0, 1, 2, 3, 4, 6, 7, 큰 값 경계.
  - `calculateCompletionHeatmap`: 시작일이 15주 전 월요일, 끝이 오늘(월요일·일요일인 날), 날짜 빈칸 채움, 같은 날 여러 건 합산, 기간 밖 기록 제외, 빈 기록.
- `feature/home` `androidHostTest`: 주간 회고 시트 열기 시 `heatmap`이 함께 채워지는지(기존 #462 Presenter 테스트 확장), 헤더 탭 이벤트로 시트가 열리는지.
- 정적 검사와 빌드: `spotlessCheck`, `detekt`, Android `assembleDebug`, iOS 시뮬레이터 빌드.

## 범위 밖

- 날짜 칸을 눌러 그날의 완료 목록 보기(툴팁)
- 월·요일 축 라벨
- 반다라트별 히트맵, 기간 선택
- 위젯에 히트맵 표시
- 별도 통계 화면
