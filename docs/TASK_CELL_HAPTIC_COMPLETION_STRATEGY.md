# 태스크 셀 롱클릭 빠른 완료 전략

## 목표

- 제목이 있는 미완료 태스크 셀을 길게 누르면 편집 바텀시트를 열지 않고 즉시 완료한다.
- 저장 성공 시 Android와 iOS에서 짧은 햅틱을 정확히 한 번 재생한다.
- 기존의 짧은 탭 편집 흐름과 완료 토글 편집 흐름은 유지한다.

## 동작 규칙

| 셀 상태 | 짧은 탭 | 긴 탭 |
| --- | --- | --- |
| 미완료 TASK, 제목 있음 | 편집 바텀시트 열기 | 완료 저장 후 햅틱 |
| 완료 TASK | 편집 바텀시트 열기 | 동작 없음 |
| 제목 없는 TASK | 편집 바텀시트 열기 | 동작 없음 |
| MAIN 또는 SUB | 기존 편집 흐름 | 동작 없음 |

긴 탭에는 접근성 동작 이름 `목표 완료`를 제공하고 영어 리소스는 `Complete goal`, 일본어 리소스는 `目標を完了`로 제공한다.

## 구현 설계

1. `BandalartCell`의 클릭 처리를 `combinedClickable`로 전환한다.
   - 기존 `onClick`은 변경하지 않는다.
   - 위 표의 유효 조건에서만 `onLongClick`과 `onLongClickLabel`을 제공한다.
2. UI는 `HomeScreen.Event.CompleteTask(cellData)`를 Presenter에 전달한다.
3. Presenter는 현재 선택된 반다라트와 전달받은 셀을 다시 검증한다.
   - 제목 없음, 이미 완료됨, 현재 셀 트리에 없는 셀은 무시한다.
   - 처리 중인 셀 ID를 기록해 같은 셀의 중복 요청을 무시한다.
4. 저장에는 기존 `updateBandalartTaskCell`을 사용하고 제목·설명·기한은 보존한 채 `isCompleted = true`만 변경한다.
5. 저장 성공 뒤 `PlayTaskCompletionHaptic(id)` Effect를 발행한다.
   - UI는 Effect가 존재할 때만 Jindong 패턴을 composition에 넣는다.
   - Effect ID를 key로 사용하고 처리 직후 소비해 재구성 또는 화면 복원 시 재생되지 않도록 한다.
   - 여러 셀이 연속으로 완료돼도 Effect 대기열에서 성공 순서대로 하나씩 소비해 햅틱이 유실되지 않도록 한다.
   - 저장 실패 시 Effect를 발행하지 않는다.

## Jindong 적용

- 버전: `1.1.0`
  - GitHub 최신 태그는 `1.1.1`이지만 Maven Central에는 `1.1.0`까지만 게시되어 있어 실제 해석 가능한 최신 버전을 사용한다.
- 공통 의존성: `jindong-core`, `jindong-compose`
- 앱 루트를 `JindongProvider`로 감싼다.
- 햅틱은 완료 피드백에 맞게 짧은 단일 `Haptic` 패턴으로 제한한다.
- Android manifest에 `android.permission.VIBRATE`를 명시한다.
- 지원 조건은 Jindong의 Android API 26 / iOS 13 이상이며, 앱의 Android minSdk 28 / iOS 16.6 범위 안이다.

## 검증

- Presenter 테스트
  - 유효한 태스크 완료 요청이 기존 필드를 보존하고 완료 상태만 저장하는지 확인한다.
  - 저장 성공 뒤 햅틱 Effect가 한 번 발행되고 소비되는지 확인한다.
  - 제목 없음 또는 이미 완료된 태스크는 저장과 Effect가 없는지 확인한다.
  - 같은 셀의 처리 중 중복 요청은 한 번만 저장되는지 확인한다.
  - 서로 다른 셀의 연속 완료 Effect가 저장 성공 순서대로 모두 전달되는지 확인한다.
- 정적 검증
  - Home 모듈 테스트와 코드 스타일 검사를 수행한다.
- 실기기 수동 검증
  - Android와 iOS에서 짧은 탭은 편집, 긴 탭은 즉시 완료되는지 확인한다.
  - 완료/빈 태스크 및 MAIN/SUB의 긴 탭이 무시되는지 확인한다.
  - 햅틱 강도가 과하거나 약하지 않은지 각 플랫폼에서 확인한다.

## 제외 범위

- 완료된 셀을 긴 탭으로 미완료 상태로 되돌리는 기능
- 성공/실패 Snackbar 또는 Toast 추가
- 기존 바텀시트 완료 토글 제거
- 앱 버전 상승과 배포
