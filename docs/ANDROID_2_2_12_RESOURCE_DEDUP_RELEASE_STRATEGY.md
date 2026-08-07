# Android 2.2.12 리소스 단일화 및 배포 전략

## 배경

Android 2.2.11 AAB의 Play 예상 신규 설치 크기는 23MB로, Production 2.2.6보다 10.9MB 증가했다.

AAB 구성 분석 결과 KMP 전환 과정에서 동일한 Compose Multiplatform 리소스가 다음 두 경로에 함께 남아 각각 패키징되고 있다.

- `composeApp/src/commonMain/composeResources`: 약 16MB
- `core/designsystem/src/commonMain/composeResources`: 약 16MB

현재 Kotlin 소스는 `bandalart.core.designsystem.generated.resources`만 사용하며 `bandalart.composeapp.generated.resources`를 참조하지 않는다. AAB에서도 두 리소스 묶음이 각각 압축 후 약 9.49MB를 차지한다.

KMP Room의 `BundledSQLiteDriver`가 추가한 SQLite 네이티브 라이브러리는 플랫폼 공통 데이터베이스 동작에 필요한 리소스이므로 이번 작업에서 변경하지 않는다.

## 목표

- Compose 리소스의 단일 소스를 `core:designsystem`으로 확정한다.
- 사용되지 않는 `composeApp` 리소스 사본을 제거한다.
- Android 앱 버전을 `2.2.12 (20212)`로 올린다.
- 설정 화면, 다크 모드, 이메일 문의 기능을 포함한 한국어·영어·일본어 출시 노트를 제공한다.
- 리소스 제거 전후 AAB 구성을 비교해 중복 제거와 용량 감소를 확인한다.

## 범위

### 포함

- `composeApp/src/commonMain/composeResources` 제거
- `core/designsystem/src/commonMain/composeResources` 유지
- 앱 버전 `2.2.12` 상향
- Play Internal Testing 출시 노트 갱신
- Android release AAB 구성 및 용량 검증

### 제외

- `BundledSQLiteDriver` 또는 KMP Room 드라이버 변경
- Pretendard 폰트 가중치 축소나 서브셋 적용
- UI, 데이터베이스, 인앱 업데이트 동작 변경
- 네이티브 디버그 기호 업로드 설정

## 버전 계산

저장소의 Android application convention은 다음 공식을 사용한다.

`versionCode = major * 10000 + minor * 100 + patch`

따라서 `2.2.12`의 versionCode는 `20212`다.

## 작업 순서

1. `composeApp` generated resource import가 0개인지 다시 확인한다.
2. `composeApp/src/commonMain/composeResources`를 제거한다.
3. `gradle/libs.versions.toml`의 patch version을 `12`로 변경한다.
4. 한국어·영어·일본어 Internal release notes를 갱신한다.
5. 정적 검사와 대표 테스트를 실행한다.
6. 서명된 release AAB를 생성한다.
7. AAB에서 `bandalart.composeapp.generated.resources`가 사라지고 `bandalart.core.designsystem.generated.resources`만 남는지 확인한다.
8. 2.2.11 AAB와 압축 용량 및 주요 항목을 비교한다.
9. commit-push 후 `main` 대상 PR을 생성하고 CI 통과 뒤 일반 merge한다.
10. 최신 `main`에서 Play 배포 선검증 후 2.2.12를 Internal Testing에 업로드한다.

## 검증

- [ ] `bandalart.composeapp.generated.resources` import가 없다.
- [ ] `core:designsystem`의 모든 Compose 리소스가 유지된다.
- [ ] versionName이 `2.2.12`다.
- [ ] versionCode가 `20212`다.
- [ ] 한국어·영어·일본어 출시 노트가 비어 있지 않다.
- [ ] 정적 검사와 대표 테스트가 통과한다.
- [ ] release AAB가 정상 생성된다.
- [ ] release AAB에 `bandalart.composeapp.generated.resources` 항목이 없다.
- [ ] release AAB에 `bandalart.core.designsystem.generated.resources` 항목이 있다.
- [ ] release AAB 압축 크기가 2.2.11보다 유의미하게 감소한다.

## 중단 조건

- 제거된 `composeApp` 리소스를 참조하는 코드가 발견된다.
- Android 또는 iOS 컴파일에서 리소스 심볼 누락이 발생한다.
- release AAB에 중복 리소스가 계속 포함된다.
- Play 전체 트랙 최대 versionCode가 `20212` 이상이다.
- CI 실패, credential 누락 또는 관리자 권한 우회가 필요하다.
