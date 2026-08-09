# 반다라트 템플릿 catalog v1 전략

## 배경

사용자는 25칸을 모두 직접 입력하지 않고도 자주 쓰는 목표 구조로 빠르게 시작할 수 있어야 한다. 템플릿은 한 번 출시하고 끝나는 정적 예제가 아니라, 문의로 받은 요청을 다음 앱 업데이트의 catalog에 반영하는 살아 있는 기능으로 운영한다.

이 문서는 GitHub issue #208의 첫 출하 범위를 고정한다. 이전 조사에서 고려한 별도 선택 화면, 전체 미리보기, 생성 전 편집은 v1에서 제외한다. 생성된 모든 셀은 기존 편집 흐름에서 바로 수정할 수 있으므로 선택과 생성 사이에 별도 단계를 두지 않는다.

## 사용자 흐름

1. 사용자가 기존 반다라트 목록 bottom sheet에서 `반다라트 추가`를 누른다.
2. 같은 sheet가 생성 방법 목록으로 전환된다. modal을 중첩하거나 새 navigation destination을 만들지 않는다.
3. 첫 항목은 빈 반다라트 직접 만들기이고, 그 아래에 앱에 포함된 템플릿 5개를 표시한다.
4. 항목을 누르면 기존 슬롯 확인과 보상형 광고 gate를 그대로 거친다.
5. 생성 가능한 경우 새 반다라트를 원자적으로 만들고 곧바로 선택한다.
6. 사용자는 기존 셀 편집 bottom sheet에서 자신의 상황에 맞게 내용을 수정한다.
7. `원하는 템플릿 요청하기`는 기존 문의 메일 흐름을 연다.

## v1 catalog

catalog는 앱 바이너리에 포함된 버전 관리형 Kotlin 데이터로 시작한다. 서버, 원격 config, 추천·인기순은 요청량을 확인한 뒤 별도 단계로 확장한다.

| ID | 표시 이름 | 기본 이모지 | 의도 |
| --- | --- | --- | --- |
| `job_preparation_v1` | 취업 준비 | 💼 | 지원 준비부터 면접까지 |
| `workout_habit_v1` | 운동 습관 | 💪 | 운동·회복·기록 습관 |
| `study_plan_v1` | 공부 계획 | 📚 | 목표·학습·복습·실전 |
| `money_habit_v1` | 재테크 습관 | 💰 | 예산·저축·투자·점검 |
| `travel_plan_v1` | 여행 준비 | 🧳 | 일정·예약·짐·현지 준비 |

- Room의 5×5 구조는 그대로 생성하되 모든 칸을 채울 필요는 없다. 정의되지 않은 칸은 빈 셀로 남긴다.
- 최초 catalog 내용은 issue의 기존 합의대로 한국어 기준으로 제공한다. 다른 언어 catalog는 실제 요청과 함께 후속 범위로 분리한다.
- ID에는 버전을 포함한다. 출시 뒤 내용을 크게 바꿀 때 기존 ID를 재해석하지 않고 새 버전을 추가한다.

## 생성과 데이터 정합성

### 원자 생성

- `BandalartRepository.createBandalart(templateId?)`가 빈 생성과 템플릿 생성을 한 경계에서 제공한다.
- DAO transaction 하나가 `bandalarts` 행, main 1개, sub 4개, task 20개를 모두 삽입한다.
- template은 title·profile emoji와 일부 셀 title만 제공한다. 나머지 색상, 완료 상태와 빈 셀 규칙은 현재 기본값을 따른다.
- transaction 실패 시 일부 셀이나 기존 반다라트가 수정된 상태를 남기지 않는다.
- 기존 반다라트를 update하거나 덮어쓰는 API는 사용하지 않는다.

### 슬롯·보상형 광고

- template 선택도 빈 생성과 동일한 무료 슬롯·추가 슬롯 정책을 거친다. UI에서 직접 repository를 호출해 gate를 우회하지 않는다.
- 무료 슬롯이 있으면 선택한 template ID로 즉시 생성한다.
- 광고가 필요하면 선택한 template ID를 pending rewarded creation과 함께 DataStore에 저장한다.
- reward grant 뒤 생성과 process recovery는 저장된 동일 ID를 사용한다. 구버전 pending 데이터처럼 ID가 없으면 빈 생성으로 호환한다.
- 광고 dismiss는 생성하지 않고 pending template도 함께 지운다. 광고 SDK 실패의 기존 fail-open 정책은 유지하되 선택한 template으로 생성한다.

## UI와 상태

- `BandalartList` bottom sheet state에 목록/생성 방법 두 화면만 둔다.
- 생성 방법 상태는 sheet 안에서만 필요한 작은 UI state이므로 Circuit state에 명시하고 dismissal과 함께 제거한다.
- template row는 이모지, 이름, 한 줄 설명을 보여준다. 선택 즉시 생성 요청을 보내며 별도의 preview·확인 dialog는 만들지 않는다.
- 문의 row는 기존 `ContactSupport` event와 mail launcher를 재사용한다.
- TalkBack/VoiceOver가 행 전체를 하나의 button으로 읽도록 하고 최소 48dp touch target을 유지한다.

## 테스트와 검증

### 자동 테스트

- catalog ID는 유일하고 5개이며 각 template은 sub 4개 이하, sub별 task 5개 이하이다.
- DAO가 빈 생성과 부분 template 모두 정확히 1+4+20 셀로 원자 생성한다.
- repository가 template ID를 올바른 DB draft로 변환한다.
- DataStore/slot repository가 pending template ID를 prepare, grant, clear, recovery에서 보존한다.
- Presenter가 무료 슬롯, 광고 grant, fail-open, process recovery에서 선택한 template을 생성한다.
- template 선택이 기존 selection coordinator를 통해 새 board를 선택하고 기존 board를 덮어쓰지 않는다.

### 수동 확인

- Android/iOS에서 기존 목록 → 추가 → 직접 만들기/5개 template/문의 row가 표시된다.
- 작은 화면에서 목록이 스크롤되고 마지막 문의 row까지 접근 가능하다.
- 무료 슬롯과 슬롯 소진 상태에서 같은 template이 생성된다.
- 광고 dismiss 시 생성되지 않고, grant 시 정확히 한 개만 생성된다.
- 생성된 일부 빈 셀을 기존 편집 흐름으로 자유롭게 수정할 수 있다.

## 완료 조건

- [x] 기존 bottom sheet 안에서 직접 만들기와 5개 template을 선택할 수 있다.
- [x] 선택 즉시 기존 slot/reward gate를 거쳐 새 반다라트를 만든다.
- [x] template 생성은 원자적이며 기존 데이터를 덮어쓰지 않는다.
- [x] 광고 대기·grant·process recovery 동안 template ID가 보존된다.
- [x] 문의 row가 기존 support mail 흐름을 연다.
- [x] 관련 host tests, Android compile, iOS simulator Kotlin compile과 정적 검사가 통과한다.
- [x] PR을 생성하고 CI와 사용자 검토를 거쳐 merge한다.
