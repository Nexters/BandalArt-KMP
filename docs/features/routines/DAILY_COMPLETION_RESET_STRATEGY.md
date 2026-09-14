# 반다라트별 일일 완료 체크 초기화 전략

- 관련 이슈: #393
- 기준 브랜치: `feat/daily-completion-reset`
- Android 출시 버전: `2.5.0 (20500)`
- iOS TestFlight 버전: `1.4.0`

## 목표

일일 루틴으로 사용하는 반다라트가 날짜가 바뀐 뒤 완료 체크만 다시 시작할 수 있게 한다. 목표 제목, 설명, 마감일, 이모지, 색상과 셀 구조는 보존한다.

## UX

- 현재 반다라트 제목 옆 `⋮` 메뉴에 `루틴 설정`을 추가한다.
- 별도 루틴 설정 바텀시트에서 `매일 새로 시작하기` 토글과 `지금 완료 체크 초기화` 액션을 제공한다.
- 토글 활성화는 현재 진행률을 바꾸지 않고 다음 로컬 날짜부터 적용한다.
- 수동 초기화는 완료된 셀이 있을 때만 활성화하며 확인 다이얼로그를 거친다.
- 초기화는 100%뿐 아니라 부분 완료 상태에도 적용한다.

### 발견성

- 기존 Balloon 패턴을 사용해 `⋮` 버튼에 신규 기능 안내를 앱 전체에서 한 번만 표시한다.
- 문구는 `매일 다시 시작할 수 있어요. ⋮에서 루틴 설정을 확인해 보세요.`를 기준으로 한국어·영어·일본어를 제공한다.
- Balloon을 닫거나 `⋮`를 열거나 루틴 설정에 진입하면 영구적으로 다시 표시하지 않는다.
- 기존 태스크 빠른 완료 Balloon과 동시에 표시하지 않는다. 루틴 안내를 먼저 보여주고 닫힌 뒤 태스크 안내를 허용한다.
- Balloon에는 바깥 터치·뒤로가기·자체 터치 닫기와 polite live region을 적용한다.

## 데이터 소유권과 인터페이스

반복 규칙은 전역 설정이 아니라 각 반다라트가 소유한다. 날짜 판단과 Room 갱신은 `BandalartRepository` 뒤에 숨긴다.

```kotlin
suspend fun setDailyResetEnabled(bandalartId: Long, enabled: Boolean)
suspend fun resetCompletionsNow(bandalartId: Long): Boolean
suspend fun applyDueDailyResets(): Set<Long>
```

- `BandalartDBEntity`: `dailyResetEnabled`, `lastDailyResetDate`, `completionResetSyncPending`
- `BandalartEntity`와 UI 모델: `dailyResetEnabled`
- 테스트 가능한 날짜 provider가 현재 `LocalDate`를 제공한다.
- 운영 adapter는 호출마다 `Clock.System`과 `TimeZone.currentSystemDefault()`로 오늘을 계산한다.

## 날짜 정책

- 날짜는 ISO `YYYY-MM-DD`로 저장한다.
- 활성화 시 `lastDailyResetDate = today`로 저장해 즉시 초기화하지 않는다.
- `today > lastDailyResetDate`일 때만 자동 초기화한다.
- 시간대 변경이나 시계 역행으로 오늘이 과거가 되어도 anchor를 되감지 않는다.
- 수동 초기화 시 자동 반복이 켜져 있으면 anchor를 오늘로 갱신한다.
- 앱 진입, foreground 복귀, 관련 데이터 조회와 위젯 snapshot 조회에서 기한 도래 초기화를 적용한다.
- 앱을 자정 넘겨 계속 사용하는 경우 foreground 동안 주기적으로 날짜 변경을 확인한다. OS 백그라운드 예약 작업에는 의존하지 않는다.

## 원자성

하나의 Room transaction에서 다음을 처리한다.

1. 대상 반다라트와 기한 도래 여부를 다시 확인한다.
2. 소속된 모든 셀의 `isCompleted`를 `false`로 바꾼다.
3. 반다라트의 `isCompleted = false`, `completionRatio = 0`을 저장한다.
4. 필요하면 마지막 초기화 날짜를 갱신한다.

동시 앱·위젯 호출은 transaction 안에서 anchor를 재확인해 같은 날짜에 한 번만 처리한다. 완료 축하 snapshot은 처리된 ID만 DataStore에서 `false`로 바꿔 다음 100% 달성 시 기존 완료 흐름이 다시 동작하게 한다. 초기화 뒤 마감일 알림을 다시 조정한다.

## Room과 백업

- Room version을 2로 올리고 v1→v2 수동 migration에서 세 열을 명시적으로 추가한다. 현재 convention plugin은 KSP에 `room.schemaLocation`을 전달하지 않아 auto migration schema 입력을 만들지 못하므로, 실제 v1 파일을 여는 migration test로 수동 migration을 검증한다.
- Boolean 열은 `defaultValue = "0"`, 날짜 열은 nullable로 추가해 기존 데이터는 반복 OFF로 유지한다.
- exported v2 schema를 커밋하고 Android·iOS 본앱 및 iOS Widget의 동일 DB open 경로를 검증한다.
- `BackupBandalart`에 반복 설정과 anchor를 기본값과 함께 추가하고 backup schema version을 2로 올린다.
- v1 backup decode/restore는 `false/null` 기본값으로 계속 지원한다.

## 위젯

- Android/iOS widget snapshot 조회 전 기한 도래 초기화를 적용한다.
- Room flow 변경을 사용하는 기존 widget refresh 경로를 재사용한다.
- 별도 반복 상태를 widget preferences에 복제하지 않는다.
- Room 초기화 transaction은 완료 축하 동기화가 필요하다는 pending flag도 함께 저장한다. 앱 repository가 DataStore를 `false`로 동기화한 뒤 flag를 지우므로, iOS 위젯이 먼저 초기화하거나 DataStore 쓰기가 일시 실패해도 다음 앱 진입에서 재시도된다.

## 테스트

- DAO: 부분/100% 초기화, 다른 반다라트 불변, 내용 보존, 같은 날 멱등성, 활성화 직후 무변화, 수동 anchor 갱신, 날짜 역행, transaction rollback
- migration: v1 DB를 v2로 열 때 기존 데이터 보존과 신규 열 기본값
- repository: 고정 날짜, 완료 snapshot 갱신, reminder reconcile, 무변경 시 side effect 없음
- backup: v1 기본값 복원과 v2 round trip
- Presenter/UI state: 메뉴→바텀시트, 반다라트별 토글, 수동 확인, disabled action, foreground 중복 호출, 완료 축하 재진입
- Balloon: 최초 1회, dismiss 영속화, 메뉴 진입 시 dismiss, 기존 Balloon과 상호 배제
- widget: 초기화 후 0%/미완료 snapshot과 timeline refresh

## 배포

- 기능, migration, 버전과 스토어 문구를 같은 PR로 `main`에 병합한다.
- Android는 `2.5.0 (20500)`으로 Play Internal Testing에 배포한다.
- iOS는 `1.4.0`으로 TestFlight에 배포하고 build number는 배포 직전 App Store Connect에서 미사용 번호를 조회해 주입한다.
- Release CD는 최신 `main`, Android update priority 0, iOS test ads 모드로 실행한다.
