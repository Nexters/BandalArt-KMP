# 연속 달성과 위젯 진행률 전략

관련 이슈: #463 (후속: #462 주간 회고 알림)

## 목표

- 태스크 셀을 완료한 날이 며칠째 이어지는지(연속 달성, streak)를 홈과 Android·iOS 위젯에 보여 준다.
- 위젯에 달성률과 함께 남은 할 일 수를 보여 준다.
- 완료 이력을 날짜 단위로 저장해 #462의 주간 집계(이번 주 완료한 셀 수, 반다라트별 달성률 변화)가 같은 기록을 그대로 쓰게 한다.

## 가정

- `bandalart_cells.isCompleted`는 현재 상태일 뿐 이력이 아니다. 매일 초기화(`dailyResetEnabled`)가 켜진 반다라트는 날마다 이 값을 지운다. 그래서 완료 이력은 셀 상태와 분리된 별도 테이블에 쌓는다.
- 날짜 판단은 기존 매일 초기화와 같은 기준, 즉 기기 로컬 시간대의 `DailyResetDateProvider.today()`를 쓴다.

## 완료 기록 저장 (Room 2 → 3)

새 테이블 `cell_completion_records`를 추가하고 DB version을 3으로 올린다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | INTEGER PK autoincrement | |
| `bandalartId` | INTEGER NOT NULL | 완료 당시 반다라트 ID |
| `cellId` | INTEGER NOT NULL | 완료한 태스크 셀 ID |
| `completedDate` | TEXT NOT NULL | 완료 시점의 기기 로컬 날짜 `yyyy-MM-dd` |
| `completedAt` | INTEGER NOT NULL | 완료 시각 epoch millis |

- index: `UNIQUE(cellId, completedDate)`(셀당 하루 1건), `completedDate`(기간 조회).
- 외래 키는 두지 않는다. 반다라트 삭제나 백업 복원(`replaceAllForBackup`은 전체 삭제 후 재삽입)으로 셀이 사라져도 지나간 날의 연속 달성 이력은 남아야 하기 때문이다.
- 마이그레이션 `BANDALART_MIGRATION_2_3`은 `CREATE TABLE`과 index 생성만 한다. 기존 데이터는 바꾸지 않는다.
- 스키마 JSON `3.json`을 생성해 커밋하고, 기존 `BandalartDatabaseMigrationTest` 패턴으로 2 → 3 경로를 검증한다.

## 기록 시점과 완료 해제

- 기록 대상은 사용자가 직접 완료한 태스크 셀이다. 하위 목표·메인 목표의 자동 완료는 기록하지 않는다(같은 행동을 중복으로 세지 않기 위해).
- 기록은 DAO의 두 진입점에서 셀 갱신과 같은 transaction으로 처리한다.
  - `updateTaskCellWithDto`: 홈 편집 바텀시트와 롱클릭 빠른 완료.
  - `setTaskCompletedIfOwned`: Android·iOS 위젯 체크.
- 미완료 → 완료: 오늘 날짜로 `INSERT OR IGNORE` 한다. 같은 날 다시 완료해도 1건이다.
- 완료 → 미완료: 오늘 날짜 기록만 지운다. 실수로 체크했다가 바로 푼 경우를 취소로 보기 위해서다. 어제 이전 기록은 이미 지나간 날의 사실이므로 남긴다.
- 매일 초기화, 셀 초기화, 하위 목표 초기화, 백업 복원은 기록을 건드리지 않는다.
- 완료 기록은 로컬 전용이다. Supabase 백업 대상에 넣지 않는다. 다른 기기로 복원하면 연속 달성은 0부터 다시 시작한다.

## 날짜 경계와 시간대

- `completedDate`는 완료 순간의 기기 로컬 날짜다. 시간대를 바꿔도 이미 저장한 날짜는 다시 계산하지 않는다.
- streak 계산의 기준일도 조회 시점의 `today()`다.
- 앱을 켜 둔 채 자정을 넘기면 홈 표시는 다음 데이터 변경이나 재진입 때까지 이전 값일 수 있다. 이 정도 지연은 허용한다.

## 연속 달성 계산

- 정의: 완료 기록이 1건 이상인 날짜가 끊김 없이 이어지는 일수. 끝은 오늘이거나 어제여야 한다.
  - 오늘 기록이 없어도 어제까지 이어졌다면 그 값을 유지한다(오늘 아직 완료하지 않았을 뿐 끊긴 것은 아니다).
  - 오늘과 어제 모두 기록이 없으면 0이다.
- 반다라트 구분 없이 앱 전체 기준으로 센다.
- `core/domain`에 순수 함수 `calculateCompletionStreak(completedDates, today)`를 두고 단위 테스트한다.
- DAO는 `SELECT DISTINCT completedDate ... ORDER BY completedDate DESC`를 Flow와 suspend로 제공한다.

## 기존 사용자 초기값

- 과거 이력을 추정해 채우지 않는다(backfill 없음). 현재 `isCompleted = 1`인 셀을 오늘 기록으로 넣으면 실제로 하지 않은 오늘의 달성이 생기기 때문이다.
- 업데이트 후 첫 완료부터 1일로 시작한다.

## 홈 표시

- `HomeHeader`의 달성률·기한 행에 streak가 1 이상일 때만 `🔥 N일 연속`을 표시한다. 0이면 아무것도 표시하지 않는다.
- `BandalartRepository.observeCompletionStreak(): Flow<Int>`를 Presenter에서 `collectAsState`로 모아 `HomeScreen.State.completionStreak`로 내려 준다.
- 문자열은 ko·en·ja 리소스로 추가한다.

## 위젯 표시

공통으로 `BandalartWidgetSnapshot`에 `streakDays`, `remainingTaskCount`를 추가한다.

- `remainingTaskCount`: 표시 중인 반다라트 전체에서 제목이 있고 미완료인 태스크 셀 수.
- `streakDays`: 위 연속 달성 값(앱 전체 기준).

### Android (Glance)

- Small: 이모지·제목·달성률 아래에 streak가 1 이상이면 `🔥 N일 연속` 한 줄을 추가한다.
- Medium/Large: 헤더 캡션을 `달성률 N% · 남은 할 일 M개`로 바꾸고, streak가 1 이상이면 헤더 오른쪽 끝에 `🔥 N일 연속`을 표시한다.
- 위젯 체크도 bandalarts 행을 갱신하므로 기존 상태 Flow로 다시 그려진다. 자정 경계는 기존 `updatePeriodMillis="0"` 정책을 유지하고 다음 데이터 변경이나 위젯 갱신 때 반영된다.

### iOS (WidgetKit)

- `IosWidgetSnapshot`에 같은 두 값을 추가하고 Swift `BandalartWidgetSnapshotModel`에 매핑한다.
- Small: 달성률 아래에 streak 한 줄을 추가한다. Medium/Large: 헤더 아래에 `남은 할 일 M개`와 streak를 표시한다.
- timeline policy를 `.never`에서 `.after(다음 자정)`으로 바꿔 날짜가 바뀌면 streak를 다시 계산한다.
- 문자열은 ko·en·ja `Localizable.strings`에 추가한다.

## #462 재사용

- 주간 회고는 같은 테이블에서 `completedDate BETWEEN 주 시작 AND 주 끝`으로 완료 셀 수를 세고, `bandalartId`로 반다라트별로 묶는다.
- `completedDate` index가 이 기간 조회를 받친다. 이번 작업에서는 해당 쿼리를 만들지 않는다.
- 반다라트별 달성률 변화는 같은 주의 완료 건수를 반다라트 태스크 수와 비교하는 방식으로 #462에서 정한다.

## 테스트

- `core/database` androidHostTest: 2 → 3 마이그레이션, 완료/해제/재완료 기록, 셀 초기화·매일 초기화 후 기록 유지, 남은 할 일 수.
- `core/domain`: `calculateCompletionStreak` 경계(오늘만, 어제까지, 끊김, 빈 목록).
- `iosWidgetShared` jvmTest: 매핑에 새 필드 포함.
- 기존 fake repository는 새 메서드에 맞춰 수정한다.
