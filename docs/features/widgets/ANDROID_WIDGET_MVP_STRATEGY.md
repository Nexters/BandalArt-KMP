# Android Glance 위젯 MVP 구현 전략

## 목적

이 문서는 #293에서 Android Jetpack Glance 기반 반다라트 위젯을 단계적으로 구현하기 위한 경계와 검증 계획을 정의한다. 상위 이슈 #156의 Android MVP만 다루며 iOS WidgetKit과 App Group 데이터 공유는 후속 작업으로 남긴다.

MVP는 앱을 열지 않고 선택한 반다라트의 진행 상황을 확인하고, medium/large 위젯에서 선택한 서브 목표의 태스크를 완료 처리할 수 있어야 한다.

## 사용자 경험

### Small

- 프로필 이모지, 메인 목표 제목, 전체 달성률을 표시한다.
- 탭하면 선택한 반다라트가 앱에서 열린다.

### Medium/Large

- 선택한 서브 목표 제목과 제목이 있는 하위 태스크를 최대 5개 표시한다.
- 태스크 완료 상태를 위젯에서 토글한다.
- 토글이 끝나면 Room의 완료율을 다시 읽어 위젯을 갱신한다.

### 설정 및 빈 상태

- 위젯 추가/재설정 화면에서 반다라트와 서브 목표를 선택한다.
- 선택한 데이터가 삭제되면 빈 상태와 재설정 action을 표시한다.
- 각 위젯 인스턴스는 서로 다른 선택 상태를 가진다.

## 아키텍처 경계

### 공통 domain

플랫폼 widget 타입을 노출하지 않는 최소 snapshot 계약을 둔다.

```kotlin
data class BandalartWidgetSnapshot(
    val bandalartId: Long,
    val subGoalId: Long?,
    val title: String,
    val profileEmoji: String?,
    val completionRatio: Int,
    val subGoalTitle: String?,
    val tasks: List<BandalartWidgetTask>,
)

data class BandalartWidgetTask(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
)

interface BandalartWidgetRepository {
    suspend fun getSnapshot(bandalartId: Long, subGoalId: Long?): BandalartWidgetSnapshot?
    suspend fun setTaskCompleted(
        bandalartId: Long,
        subGoalId: Long,
        taskId: Long,
        completed: Boolean,
    ): BandalartWidgetSnapshot?
}
```

- snapshot에는 렌더링에 필요한 최소 데이터만 포함한다.
- 선택한 반다라트·서브 목표·태스크의 부모 관계를 검증한 뒤에만 완료 상태를 바꾼다.
- 빈 제목 태스크는 위젯 목록에서 제외한다.
- 조회 중 데이터가 삭제된 경우 예외 대신 `null`을 반환한다.

### 공통 data

- Room 조회와 기존 완료율 재계산 트랜잭션을 사용해 snapshot을 만든다.
- 태스크 완료 처리는 현재 title/description/due date를 보존하고 완료 값만 바꾼다.
- 위젯 전용 DB를 만들거나 앱 상태를 별도 복제하지 않는다.

### 변경 알림

- Android Glance API를 domain/data에 직접 노출하지 않는다.
- 기존 Room mutation은 완료율을 다시 계산하면서 `bandalarts` 행을 갱신하므로 Android Application에서 반다라트 목록 Flow를 관찰해 `updateAll()`을 요청한다.
- 공통 레이어에 Android만을 위한 notifier 추상화를 추가하지 않는다.
- 위젯 callback은 저장 완료 후 새 snapshot을 읽고 클릭한 인스턴스를 즉시 갱신한다.
- 향후 mutation이 `bandalarts` 행을 갱신하지 않게 바뀌면 이 관찰 조건도 함께 수정해야 한다.

### Android

- stable `androidx.glance:glance-appwidget:1.1.1`을 사용한다. preview/alpha API는 MVP에 포함하지 않는다.
- `androidApp`에 Glance receiver, responsive UI, configuration Activity와 callback을 둔다.
- callback과 configuration Activity는 `BandalartApplication.appGraph`를 통해 공통 repository를 사용한다.
- 위젯별 선택 상태는 Glance가 제공하는 per-widget state에 저장한다.
- Glance UI에서 기존 Compose `BandalartChart`를 재사용하지 않는다.
- 특정 반다라트를 여는 동작은 trampoline 경계에서 앱의 launch target을 기록한 뒤 `MainActivity`를 연다.

## 단계별 PR

### PR 1 — 공통 snapshot과 안전한 완료 처리 기반

- widget snapshot/task 모델과 repository 계약
- Room 기반 repository 구현
- 부모 관계·빈 제목·삭제 경쟁·완료율 갱신 테스트
- 잘못된 widget callback이 다른 표의 task를 바꾸지 못하는 원자적 검증 경계

이 단계는 Glance UI 없이 데이터 계약을 고정한다.

### PR 2 — Android read-only 위젯과 설정

- 공식 stable Glance 의존성
- receiver/provider metadata와 responsive breakpoint
- small/medium/large read-only UI
- configuration Activity와 인스턴스별 선택 상태
- 삭제 fallback과 재설정 action
- 선택한 반다라트 deep link

### PR 3 — 태스크 상호작용과 양방향 갱신

- task completion callback
- 중복 callback에 대한 idempotent 상태 설정
- 앱 mutation 후 관련 위젯 update
- 복수 위젯 인스턴스 갱신
- 프로세스 종료·재부팅 복원

### PR 4 — 실제 launcher QA와 안정화

- pin/reconfigure 흐름
- 라이트/다크 모드, font scale, 긴 제목과 breakpoint
- 지원 launcher의 small/medium/large 크기
- device test 또는 검증 가치가 있는 최소 시스템 테스트

## 테스트 전략

- 공통 모델과 repository는 `androidHostTest`에서 fake/Room 경계로 검증한다.
- snapshot 생성, 부모 관계 검증과 idempotent 완료 설정은 host 테스트로 고정한다.
- Glance 렌더링 가능한 순수 상태 변환은 Android 단위 테스트로 분리한다.
- launcher 제공 UI, 위젯 추가/재설정, callback과 재부팅은 실제 기기에서 검증한다.
- 각 PR은 관련 compile/test를 실행하고 마지막 PR에서 Android CI 범위를 모두 확인한다.

## 실패 및 복구 정책

- 선택 정보 없음: 설정 필요 상태를 표시한다.
- 선택 대상 삭제: stale state를 유지하지 않고 재설정 상태를 표시한다.
- callback 대상 불일치: DB를 변경하지 않고 최신 snapshot 또는 빈 상태를 반환한다.
- 저장 실패: 성공한 것처럼 optimistic 표시하지 않고 다음 update에서 원본 상태를 유지한다.
- Glance 갱신 실패: Room 저장은 되돌리지 않으며 다음 앱/위젯 갱신에서 복구한다.

## 비목표

- 5x5 전체 표를 작은 위젯에 축소 표시
- iOS WidgetKit/App Group 구현
- 서버·FCM 기반 widget 동기화
- 기존 Compose 차트 UI 재사용
- 위젯에서 목표·태스크 제목 편집

## 완료 판단

- Android 실제 launcher에서 인스턴스별 선택과 responsive UI가 동작한다.
- 앱과 위젯 어느 쪽에서 변경해도 같은 Room 상태와 완료율을 표시한다.
- 잘못된 widget callback이 다른 반다라트나 서브 목표의 태스크를 바꾸지 않는다.
- 삭제·프로세스 종료·재부팅 후 안전한 빈 상태 또는 복원이 확인된다.

## 공식 참고 자료

- [Glance 설정](https://developer.android.com/develop/ui/compose/glance/setup)
- [Glance responsive UI](https://developer.android.com/develop/ui/compose/glance/build-ui)
- [Glance configuration Activity](https://developer.android.com/develop/ui/compose/glance/configuration)
- [Glance 상호작용](https://developer.android.com/develop/ui/compose/glance/user-interaction)
- [Glance 테스트](https://developer.android.com/develop/ui/compose/glance/testing)
