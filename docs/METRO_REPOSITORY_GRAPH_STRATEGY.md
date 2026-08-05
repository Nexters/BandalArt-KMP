# Metro Repository graph 전략

## 목적

이 문서는 이슈 #182의 4-B 작업 범위를 고정한다. 4-A에서 Metro가 소유하게 된 DAO와 두 DataStore wrapper를 입력으로 사용해 세 repository의 생성 및 app singleton 수명 책임을 Koin에서 Metro로 이전한다.

Repository 구현과 domain 계약의 동작은 변경하지 않는다. 기존 ViewModel과 feature Koin module은 다음 Circuit vertical slice 전까지 유지하며, Koin은 Metro graph accessor의 동일 repository 인스턴스만 받는다.

## 기준점

- 기준 브랜치: `main`
- 기준 리비전: `9f6e9dfd57b80aca72343da7e311d3637d5c4568`
- 작업 브랜치: `refactor/metro-repository-graph`
- 선행 작업: PR #191 Platform/Room/DataStore graph
- Metro: 1.1.1
- 과도기 DI: Metro가 core/data 객체를 생성하고 Koin이 기존 ViewModel에 accessor를 노출

## 현재 생성 책임

| 계약 | 구현 | 입력 | 현재 생성자 |
| --- | --- | --- | --- |
| `BandalartRepository` | `DefaultBandalartRepository` | `BandalartDataStore`, `BandalartDao` | Koin `singleOf` |
| `InAppUpdateRepository` | `DefaultInAppUpdateRepository` | `InAppUpdateDataStore` | Koin `singleOf` |
| `OnboardingRepository` | `DefaultOnboardingRepository` | `BandalartDataStore` | Koin `singleOf` |

세 repository는 `core:data`의 `dataModule`에서 생성된다. 4-A 이후 constructor 입력은 이미 Metro `AppGraph`가 유일하게 생성하고 Koin bridge로 노출한다.

## 목표 구조

```text
PlatformBindings
      │
      ▼
PlatformDataBindings ── DB / DAO / DataStore wrappers
      │
      ▼
RepositoryBindings ─── three repository interfaces
      │
      ├── AppGraph accessors
      │
      └── Koin accessor bridge ── existing ViewModels
```

- Metro `AppScope`가 세 repository를 한 번 생성한다.
- `AppGraph`는 concrete class가 아니라 domain repository interface를 노출한다.
- Koin bridge는 graph accessor 결과만 `single`로 등록한다.
- repository 구현은 Metro 또는 Koin API를 직접 알지 않는다.

## Binding 방식 결정

JetBrains kotlinconf-app은 같은 shared 모듈 안의 구현에 constructor injection과 `@ContributesBinding(AppScope::class)`을 적용한다. BandalArt의 repository 구현은 별도 `core:data` 모듈에 있고 최종 graph는 `composeApp`에 있으므로 4-B에서는 Native multi-module contribution 자동 집계에 의존하지 않는다.

`composeApp` common source set에 명시적인 `RepositoryBindings` binding container를 두고 다음 provider를 선언한다.

- `BandalartRepository` ← `DefaultBandalartRepository(BandalartDataStore, BandalartDao)`
- `InAppUpdateRepository` ← `DefaultInAppUpdateRepository(InAppUpdateDataStore)`
- `OnboardingRepository` ← `DefaultOnboardingRepository(BandalartDataStore)`

이 방식은 구현 모듈을 Metro에 결합하지 않고 Android/iOS graph가 동일 provider 목록을 사용하게 한다. Circuit codegen 도입 뒤 multi-module contribution이 양 플랫폼에서 검증되더라도 이번 binding을 기계적으로 바꿀 필요는 없다.

## 구현 순서

1. `RepositoryBindings`를 `composeApp` common source set에 추가한다.
2. 세 repository를 `@SingleIn(AppScope::class)` provider로 만든다.
3. `AppGraph`에 세 domain interface accessor를 추가한다.
4. `metroKoinBridgeModule`이 세 accessor를 기존 Koin consumer에 노출한다.
5. `core:data`의 `dataModule`과 Koin dependency를 제거한다.
6. `composeApp`의 `coreModule`/`dataModule` include를 제거하고 feature Koin module만 유지한다.
7. graph singleton 및 Koin bridge identity test에 세 repository를 추가한다.

## 보존해야 할 계약

- 세 domain repository interface signature
- repository 구현의 query, mapping, update와 비교 로직
- DAO와 DataStore 호출 순서
- Room schema와 Preferences key/file
- 기존 ViewModel constructor와 feature Koin module
- Android 인앱 업데이트 repository의 rejected version 비교 동작

4-B에서는 repository source와 기존 repository unit test의 동작을 수정하지 않는다.

## 검증 계획

### 자동 검증

- 기존 `BandalartRepositoryTest` 통과
- 기존 `InAppUpdateRepositoryTest` 통과
- 기존 `OnboardingRepositoryTest` 통과
- 전체 기존 ViewModel test 통과
- `AppGraph` accessor 반복 조회 시 같은 repository 인스턴스 반환
- Koin bridge 조회 결과와 Metro accessor의 identity 일치
- Android unit test와 Lint
- Android debug APK와 release AAB
- iOS simulator framework link

프로젝트 지침에 따라 로컬 전체 빌드와 Gradle 검증은 사용자가 실행한다. PR CI가 생성되면 동일 gate를 다시 확인한다.

### 정적 확인

- `core:data`에 Koin import/dependency가 남아 있지 않음
- Koin module에 repository constructor 호출이 남아 있지 않음
- Metro graph가 Koin을 조회하지 않음
- concrete repository 구현이 DI framework에 의존하지 않음
- DB schema, DAO, DataStore source diff 없음

## 제외 범위

- repository 동작 리팩토링
- UseCase 신규 도입
- ViewModel과 feature Koin module 제거
- Circuit Screen/Presenter/UI 이식
- Koin runtime/plugin 제거
- DB schema, DAO query, DataStore key 변경
- `develop` Hilt graph 수정

## 완료 조건

- Metro가 세 repository interface의 유일한 app singleton 생성자다.
- 기존 Koin consumer는 Metro와 동일한 repository 인스턴스를 사용한다.
- `core:data`에서 Koin 의존성과 module이 제거된다.
- 기존 repository 및 feature 테스트 계약이 유지된다.
- 다음 5단계에서 Splash/Onboarding Presenter가 repository를 Metro에서 직접 주입받을 수 있다.

## 검증 결과 (2026-08-05)

다음 검증을 실행해 통과했다.

```shell
./gradlew allTests \
  :androidApp:lintDebug \
  :androidApp:assembleDebug \
  :composeApp:linkDebugFrameworkIosSimulatorArm64
```

- 전체 KMP 테스트와 기존 세 repository 테스트 통과
- Metro AppGraph/Koin bridge repository identity test 통과
- Android Lint 통과
- debug APK 생성 통과
- iOS Simulator arm64 framework link 통과
- 747 tasks, `BUILD SUCCESSFUL` (1분 25초)

iOS bundle ID 자동 추론, 기존 Koin Native binary와 Gradle 10 deprecated API 경고는 남아 있지만 이번 repository graph 변경으로 추가된 오류는 없다.

## 롤백

문제가 생기면 `RepositoryBindings`와 graph accessor를 제거하고 `core:data`의 기존 `dataModule`을 복원한다. 4-B는 repository 내부 동작과 저장 형식을 변경하지 않으므로 데이터 롤백은 필요하지 않아야 한다.

## 참고 자료

- [Circuit + Metro KMP 이식 맵](CIRCUIT_METRO_KMP_MIGRATION_MAP.md)
- [Platform/Room/DataStore graph 전략](METRO_PLATFORM_DATA_GRAPH_STRATEGY.md)
- [KMP/Metro troubleshooting](KMP_METRO_MIGRATION_TROUBLESHOOTING.md)
- [JetBrains kotlinconf-app](https://github.com/JetBrains/kotlinconf-app/tree/0b1616cba68e)
