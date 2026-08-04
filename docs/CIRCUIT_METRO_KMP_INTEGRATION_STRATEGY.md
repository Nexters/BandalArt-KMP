# Circuit + Metro KMP 통합 전략

## 배경

- 실행 이슈: #182
- 상위 로드맵: #180
- 통합 기준: `main`의 KMP/CMP Android·iOS 구현
- 동작 참조: `develop`의 Circuit Screen·Presenter·테스트
- 통합 브랜치: `refactor/circuit-metro-kmp`

`main`은 Room, DataStore, Compose 리소스와 플랫폼 기능이 이미 Android/iOS source set으로 이전된 상태다. `develop`은 Android에서 검증된 Circuit 화면 계약과 Presenter 동작을 가진다. 두 브랜치를 전체 병합하지 않고, `main`의 KMP 기반에 필요한 Circuit 코드와 테스트를 기능 단위로 이식하고 최종 런타임 DI를 Metro로 통일한다.

## 이번 단계: #182의 2번

이번 단계는 코드를 이식하거나 Metro를 적용하는 단계가 아니다. 다음 단계들이 전체 브랜치 병합 없이 시작될 수 있도록 실행 지도를 만드는 것이 목적이다.

### 포함

- `main`과 `develop`의 화면, 상태 소유자, 저장소, DI binding 대응표
- 기능별 이식·재사용·폐기 대상 분류
- Koin과 Metro의 과도기 공존 경계
- 객체 생성 책임과 singleton 소유권 이전 순서
- 공통·Android·iOS Metro component/scope 초안
- 후속 단계별 시작 조건과 검증 기준

### 제외

- Metro 플러그인·런타임·컴파일러 추가
- Circuit 의존성 또는 Screen/Presenter 코드 이식
- Koin module 제거
- Room·DataStore·플랫폼 구현 재작성
- 기능·디자인·내비게이션 동작 변경

## 원칙

1. `main`의 KMP 계층과 플랫폼 구현을 통합 기반으로 유지한다.
2. `develop`은 Circuit 동작과 Presenter 테스트의 참조로만 사용한다.
3. Hilt annotation과 component를 `main`으로 복사하지 않는다.
4. 동일 객체를 Koin과 Metro가 동시에 생성하지 않는다.
5. 객체 생성 책임은 의존성 그래프의 아래쪽부터 Metro로 이전한다.
6. 각 후속 단계는 Android와 iOS가 함께 빌드되는 독립 PR이어야 한다.
7. 기능 이식과 디자인 변경을 섞지 않는다.
8. 기존 로컬 DB 파일, schema, DataStore key와 사용자 데이터를 보존한다.

## 조사 및 설계 순서

1. `main`의 KMP module/source set, Koin module, ViewModel, Compose Navigation을 조사한다.
2. `develop`의 Circuit Screen/Presenter, Hilt binding, Presenter 테스트를 조사한다.
3. 기능별로 `재사용`, `이식`, `대체 후 제거`, `비범위`를 분류한다.
4. 플랫폼 provider → DB/DataStore → repository → Presenter factory → composition root 순서로 생성 책임을 정리한다.
5. Metro component와 scope가 플랫폼 생명주기를 어떻게 따라야 하는지 초안을 만든다.
6. 결과를 별도 이식 맵 문서에 기록하고 후속 단계의 작업 단위를 확정한다.

## 완료 조건

- `main`/`develop`의 Screen, ViewModel/Presenter, repository, DI binding 대응표가 있다.
- 기능별 이식 대상과 폐기 대상이 파일 단위로 구분돼 있다.
- Koin/Metro 공존 기간의 객체 소유권 규칙이 명확하다.
- 공통·Android·iOS Metro component/scope와 플랫폼 binding 경계가 정의돼 있다.
- #182의 3~8번을 전체 브랜치 병합 없이 시작할 수 있는 순서와 검증 기준이 있다.

## 산출물

- 이 문서: 통합 원칙과 단계 경계
- `CIRCUIT_METRO_KMP_MIGRATION_MAP.md`: 실제 파일 대응표, DI 소유권, component/scope 설계 및 후속 PR 지도

## 기준 리비전

- `main`: `9a70451cd5792a3c4facf2dba166fc337988519d`
- `develop`: `437df428b545e7b9353bd66a3033d3ea8944b6a2` (#187)

문서가 작성된 뒤 두 브랜치가 변경되면 후속 작업 시작 전에 위 리비전과 변경분을 다시 비교한다. 특히 데이터 schema, DataStore key, Screen 계약과 앱 버전이 달라졌다면 이식 맵을 먼저 갱신한다.

## 사전 결론

1. `main`의 KMP 코드 위에 `develop`의 Circuit 계약과 Presenter 동작을 선택적으로 재작성한다.
2. `develop`의 Android UI, Hilt annotation, network/guest-login 계층은 이식하지 않는다.
3. Metro 1.3.2의 Circuit codegen을 사용하려면 Kotlin 2.3.20 이상이 필요하므로 현재 `main`의 Kotlin 2.1.20을 먼저 올린다.
4. AGP 9 이상에서는 KMP와 `com.android.application`을 한 모듈에 둘 수 없으므로 `composeApp`의 Android entry point를 별도 `androidApp` 모듈로 분리한다.
5. Koin과 Metro의 공존 방향은 `Koin → Metro accessor`만 허용한다. Metro graph가 Koin service locator를 조회하지 않는다.
6. 최초에는 `AppScope` 하나만 사용한다. Presenter는 assisted factory가 화면마다 생성하고 DI graph에 Activity/Screen scope를 추가하지 않는다.

## 공식 문서 기준

- [KMP 프로젝트의 AGP 9 마이그레이션](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html)
- [KMP 권장 모듈 구조](https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html)
- [Metro 1.3.2 설치](https://zacsweers.github.io/metro/1.3.2/installation/)
- [Metro Kotlin 호환성](https://zacsweers.github.io/metro/1.3.2/compatibility/)
- [Metro dependency graph](https://zacsweers.github.io/metro/1.3.2/dependency-graphs/)
- [Metro Circuit integration](https://zacsweers.github.io/metro/1.3.2/circuit/)
- [Metro adoption strategy](https://zacsweers.github.io/metro/1.3.2/adoption/)
- [Metro KMP 제약](https://zacsweers.github.io/metro/1.3.2/features/)
