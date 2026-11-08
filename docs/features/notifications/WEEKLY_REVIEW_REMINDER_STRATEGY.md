# 주간 회고 알림과 진행률 요약 전략

- 작성일: 2026-11-09
- 관련 이슈: [#462 주간 회고 알림과 진행률 요약](https://github.com/Nexters/BandalArt-KMP/issues/462)
- 기준 commit: `8e545aa2` (#463 연속 달성과 위젯 진행률)
- 참고: [마감일 알림 정책 가이드](DEADLINE_NOTIFICATION_POLICY_GUIDE.md), [연속 달성과 위젯 진행률](../home/COMPLETION_STREAK_WIDGET_PROGRESS_STRATEGY.md)

## 목표

- 매주 한 번 로컬 알림으로 "이번 주 회고"를 안내해 재방문을 늘린다.
- 알림을 열면 이번 주 진행률 요약(완료한 셀 수, 반다라트별 진행)을 보여 준다.
- 설정 시트에서 켜고 끌 수 있다.

## 결정

| 항목 | 결정 |
|---|---|
| 알림 시각 | 매주 일요일 20:00, 기기 현지 시각 |
| 시각 선택 | 제공하지 않는다. 고정 시각으로 시작하고 반응을 본 뒤 후속 이슈로 판단한다 |
| 기본값 | 꺼짐. 켤 때 권한을 확인·요청한다 |
| 설정 저장 | DataStore `weekly_review_reminder_enabled` (Boolean, 기본 `false`) |
| 백업 | 클라우드 백업에 넣지 않는다. 기기 알림 권한과 묶인 기기별 설정이다 |
| 권한 | 마감일 알림과 같은 `DeadlineNotificationAuthorization`과 확인·요청·설정 이동 흐름을 재사용한다 |
| 집계 데이터 | #463의 `cell_completion_records`를 그대로 쓴다. 새 테이블과 마이그레이션은 없다 |
| 주 범위 | 월요일~일요일(ISO 주). 요약은 이번 주 월요일부터 오늘까지 |
| 요약 UI | 홈 바텀시트. 알림 탭과 설정 시트의 "이번 주 회고 보기" 행에서 연다 |

### 기본값을 꺼짐으로 두는 이유

- 마감일 알림도 기본 꺼짐이다. Android 13+와 iOS는 앱 단위 권한이라, 마감일 알림 때문에 받은 권한으로 사용자가 고르지 않은 주간 알림을 보내면 신뢰를 잃는다.
- 켜는 순간 권한 흐름을 거치므로 사용자가 무엇을 허용하는지 분명하다.
- 노출 경로(온보딩 안내, 홈 배너 등)는 이 이슈 범위 밖이다.

## 집계 정의

`cell_completion_records`는 사용자가 직접 완료한 태스크 셀을 셀당 하루 1건 기록한다(#463 전략 참고).

- 기간: `weekStart = today - (today.dayOfWeek.isoDayNumber - 1)일`, `weekEnd = today`. `today`는 `DailyResetDateProvider.today()`다.
- 이번 주 완료 수: 기간 안의 기록 건수. 같은 셀을 다른 날 완료하면(매일 초기화 반다라트) 날마다 1건씩 센다. "이번 주에 체크한 횟수"가 회고 목적에 맞기 때문이다.
- 완료한 날 수: 기간 안의 `DISTINCT completedDate` 수.
- 반다라트별 완료 수: `bandalartId`로 묶은 건수. 삭제된 반다라트의 기록은 반다라트별 목록에서 빼고 총합에는 남긴다.
- DAO: `SELECT * FROM cell_completion_records WHERE completedDate BETWEEN :start AND :end`. `completedDate` index를 탄다.

### "반다라트별 달성률 변화"를 다루는 방식

- 정확한 변화량(주 시작 시점 달성률 → 현재 달성률)을 내려면 주마다 달성률 스냅샷을 저장해야 한다. 매일 초기화 반다라트는 날마다 달성률이 0으로 돌아가므로, 셀 상태나 완료 기록만으로는 주 시작 시점 값을 복원할 수 없다.
- 이번 단계에서는 스냅샷 테이블을 만들지 않는다. 대신 반다라트마다 "이번 주 완료 N개"와 "현재 달성률 M%"를 함께 보여 준다. 이번 주에 얼마나 움직였는지는 완료 수가, 지금 어디까지 왔는지는 달성률이 보여 준다.
- 실제 변화량 표시는 스냅샷 저장(스키마 v4)이 필요하므로 별도 후속 이슈로 남긴다.

## 도메인과 데이터

`core/domain`

- `WeeklyReviewSummary(weekStart, weekEnd, completedCount, activeDayCount, bandalarts: List<WeeklyReviewBandalartSummary>)`
- `WeeklyReviewBandalartSummary(bandalartId, title, profileEmoji, mainColor, completedCount, completionRatio)`. 이번 주 완료 수 내림차순, 같으면 id 오름차순.
- 순수 함수 `calculateWeeklyReviewSummary(records, bandalarts, today)`. 주 경계와 묶기를 단위 테스트한다.
- `BandalartRepository.getWeeklyReviewSummary(): WeeklyReviewSummary`. 시트를 여는 시점에 한 번 읽는다(시트를 연 채 데이터가 바뀌는 경우는 고려하지 않는다).
- 알림 계약
  - `WeeklyReviewReminderScheduler { schedule(): Result; cancel(): Result }`. 결과 타입은 기존 `DeadlineReminderSchedulingResult`를 재사용한다.
  - `WeeklyReviewReminderReconciler { reconcile() }`. 설정이 꺼져 있거나 권한이 GRANTED/QUIET가 아니면 `cancel`, 맞으면 `schedule`.
  - `WeeklyReviewLaunchTarget { pending: StateFlow<Boolean>; record(); acknowledge() }`. 마감일 launch target과 같은 buffered 방식.
- `SettingsRepository`: `weeklyReviewReminderEnabled: Flow<Boolean>`, `setWeeklyReviewReminderEnabled(Boolean)`.

## 재조정 시점

마감일 알림 재조정과 같은 지점에서 함께 호출한다.

- 앱 시작(Android `MainActivity` 사용자 실행, iOS `DeadlineReminderLifecycleBridge.record()`)
- 설정 토글 변경, 권한 결과, 설정 앱에서 돌아온 직후(`DeadlineReminderForegrounded`)
- 시간·시간대 변경(Android `DeadlineReminderTimeChangeReceiver`, iOS 시간대 변경 observer와 `applicationSignificantTimeChange`)

## Android

- 예약: WorkManager `OneTimeWorkRequest`, unique name·tag `weekly_review.v1`, `ExistingWorkPolicy.REPLACE`. 지연 시간은 지금 이후 가장 가까운 일요일 20:00까지. 재조정이 같은 시각을 다시 계산하므로 REPLACE로 덮어써도 결과는 같다.
- 마감일 알림의 `clearFeatureState`는 `deadline.v1` 태그 전체를 취소하므로 주간 알림은 반드시 다른 태그를 쓴다.
- 발송: `WeeklyReviewReminderWorker`
  1. 설정이 꺼졌거나 권한이 없으면 보내지 않는다.
  2. 실행 시점의 현지 날짜가 일요일이 아니면 보내지 않는다(기기가 꺼져 있다 월요일에 늦게 실행된 경우. 새 주가 시작돼 요약이 비어 보이기 때문이다).
  3. 알림을 보낸 뒤 `reconcile()`로 다음 주 일요일을 예약한다. 발송을 건너뛴 경우에도 재예약한다.
- 채널: `weekly_review_v1`(이름 "주간 회고"), importance DEFAULT. 마감일 채널과 분리해 사용자가 하나만 끌 수 있게 한다.
- 권한 판단의 한계: 공용 `AndroidDeadlineNotificationAuthorization`은 마감일 채널이 꺼졌을 때 BLOCKED를 돌려준다. 이 경우 주간 알림도 꺼진 것으로 표시된다. 반대로 주간 채널만 끈 경우는 설정상 켜짐으로 보이지만 OS가 표시하지 않는다. 채널별 상태 구분은 이번 범위에서 하지 않는다.
- 탭: 기존 `DeadlineNotificationTrampolineActivity`에 action `com.nexters.bandalart.action.OPEN_WEEKLY_REVIEW`를 추가하고, 이 action이면 `WeeklyReviewLaunchTarget.record()`를 호출한 뒤 `MainActivity`를 연다. 새 activity는 만들지 않는다.
- Worker 의존성은 기존 `AndroidDeadlineReminderDependencies` registry에 주간 항목을 추가해 전달한다.

## iOS

- 예약: `UNCalendarNotificationTrigger(dateMatching: weekday=1(일), hour=20, minute=0, repeats=true)`, identifier `weekly_review.v1`. 같은 identifier로 추가하면 기존 요청을 대체한다. 시간대를 지정하지 않은 date components라 기기 현지 시각을 따른다.
- 취소: `removePendingNotificationRequestsWithIdentifiers(["weekly_review.v1"])`.
- `AppDelegate`
  - `willPresent`: `weekly_review.v1`도 배너·목록·소리로 표시한다.
  - `didReceive`: 기본 action이고 identifier가 `weekly_review.v1`이면 `DeadlineNotificationLaunchBridge.recordWeeklyReview()`를 호출한다.
- `DeadlineReminderLifecycleBridge`는 주간 재조정도 함께 실행한다. Swift 쪽 호출부는 바꾸지 않는다.

## 홈 UI

- 설정 시트 "알림" 섹션
  - 마감일 알림 행 아래에 "주간 회고 알림" 스위치 행과 설명("매주 일요일 저녁 8시에 이번 주 진행을 알려 드려요").
  - 켤 때의 확인 다이얼로그·권한 요청·설정 이동 흐름은 마감일 알림과 같다. Presenter는 지금 권한을 요청 중인 대상(마감일/주간)을 기억해 결과를 해당 설정에 반영한다.
  - 스위치 아래 "이번 주 회고 보기" 행. 알림 설정과 관계없이 항상 보인다.
- 요약 바텀시트(`BottomSheetState.WeeklyReview`)
  - 제목 "이번 주 회고", 기간(예: 11월 3일 ~ 11월 9일)
  - "완료한 셀 N개 · M일 동안 달성"
  - 반다라트별 행: 이모지, 제목(없으면 기존 "제목 없음" 표기), "이번 주 N개 완료", "달성률 M%"
  - 이번 주 기록이 없으면 빈 상태 문구("이번 주에는 아직 완료한 셀이 없어요. 오늘 하나 시작해 볼까요?")
  - 반다라트 행을 누르면 해당 반다라트로 이동하고 시트를 닫는다.
- 알림 탭으로 들어오면 `WeeklyReviewLaunchTarget.pending`을 관찰해 요약 시트를 열고 `acknowledge()`한다.

## 문구

알림 문구는 플랫폼 리소스, 화면 문구는 `core/designsystem` compose resource에 둔다.

| 키 | ko | en | ja |
|---|---|---|---|
| 알림 제목 | 이번 주 회고 | Weekly review | 今週のふりかえり |
| 알림 본문 | 이번 주 반다라트 진행을 돌아보고 다음 주를 준비해 보세요. | Look back on this week's progress and get ready for next week. | 今週の進み具合をふりかえって、来週の準備をしましょう。 |
| Android 채널 이름 | 주간 회고 | Weekly review | 週間ふりかえり |
| 설정 행 | 주간 회고 알림 | Weekly review reminder | 週間ふりかえり通知 |
| 설정 설명 | 매주 일요일 저녁 8시에 이번 주 진행을 알려 드려요 | Every Sunday at 8 PM, we'll recap your week | 毎週日曜日の夜8時に今週の進み具合をお知らせします |
| 요약 열기 행 | 이번 주 회고 보기 | View this week's review | 今週のふりかえりを見る |
| 요약 합계 | 완료한 셀 %1$d개 · %2$d일 동안 달성 | %1$d cells completed · active %2$d days | 完了したセル %1$d個 · %2$d日間達成 |
| 반다라트 행 | 이번 주 %1$d개 완료 · 달성률 %2$d% | %1$d completed this week · %2$d% done | 今週 %1$d個完了 · 達成率 %2$d% |
| 빈 상태 | 이번 주에는 아직 완료한 셀이 없어요. 오늘 하나 시작해 볼까요? | No cells completed this week yet. Why not start one today? | 今週はまだ完了したセルがありません。今日ひとつ始めてみませんか？ |

- 권한 확인 다이얼로그는 마감일 알림 문구를 그대로 쓰되 제목만 주간 회고용으로 둔다.

## 테스트

`docs/architecture/kmp/KMP_TESTING_GUIDE.md` 기준

- `core/domain` `androidHostTest`: `calculateWeeklyReviewSummary`(월요일 시작, 일요일 포함, 지난주 제외, 삭제된 반다라트, 정렬), 다음 일요일 20:00 계산(일요일 20:00 이전·이후, 다른 요일).
- `core/database` `androidHostTest`: 기간 조회 DAO.
- `core/data` `androidHostTest`: `DefaultWeeklyReviewReminderReconciler`(꺼짐 → cancel, 권한 없음 → cancel, 켜짐+GRANTED/QUIET → schedule).
- `composeApp` `androidHostTest`: Android scheduler의 unique work·지연 시간, Worker의 일요일 아님 건너뛰기와 재예약.
- `feature/home` `androidHostTest`: 주간 토글 켜기(권한 흐름 포함)·끄기, 알림 launch로 요약 시트 열기.

## 범위 밖

- 알림 요일·시각 선택
- 실제 달성률 변화량(주간 스냅샷 저장)
- 지난주와 비교, 차트, 공유
- 홈 배너 등 주간 알림 노출 유도
