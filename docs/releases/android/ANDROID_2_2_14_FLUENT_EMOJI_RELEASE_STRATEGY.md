# Android 2.2.14 Fluent Emoji Internal Release 전략

## 배경

2.2.13 이후 `main`에 Fluent Color 이모지 300개 공통 renderer, 검색·카테고리 picker와 최근 사용 최대 12개 저장이 반영됐다. #212의 코드 단계는 완료됐지만 실제 Android release artifact 크기와 기존 사용자 데이터 호환성은 Internal Testing에서 확인해야 한다.

저장소에 추적 중인 `androidApp/release/app-release.aab`는 2.2.13 clean release 산출물이 아니라 과거 커밋 `101138e`에서 생성된 15,918,047B 파일이다. 이 파일을 2.2.13 기준값으로 간주하지 않는다.

## 목표

- Android 앱 버전을 2.2.14 (`20214`)로 올린다.
- 한국어·영어·일본어 Internal release notes에 Fluent Emoji의 사용자 기능을 반영한다.
- 최신 `main`에서 clean release AAB를 생성하고 실제 AAB 크기를 기록한다.
- Play Console에서 2.2.13과 2.2.14의 다운로드 크기를 비교해 5MB 증가 예산을 확인한다.
- Internal Testing에서 기존 이모지 fallback, 신규 선택·저장·최근 사용을 확인한다.

## 범위

### 포함

- patch version `13`에서 `14`로 상향
- Fluent Color 300개, 검색·카테고리·최근 사용 출시 노트
- 기존 clean AAB namespace 검증과 Play Internal Testing 업로드
- #212 출시 검증 결과 문서화

### 제외

- 이모지 catalog 확대 또는 asset 품질 변경
- UI·도메인·DB schema 변경
- Production 트랙 배포
- iOS 실제 기기·App Store 검증

iOS 실제 렌더링·artifact 크기·App Store 검증은 개발자 계정 복구 후 진행할 #214로 이관했다. #212에서는 공통 코드의 iOS framework build까지 확인하고, 이번 Android 출시 검증이 끝나면 Android 배포 범위를 닫는다.

## 자동 검증

- Code quality
- 전체 unit tests
- Android lint and build
- iOS framework build
- clean release AAB의 versionName `2.2.14`, versionCode `20214`
- 제거된 `bandalart.composeapp.generated.resources` namespace 부재
- `bandalart.core.designsystem.generated.resources` namespace 존재
- clean release AAB byte 크기 기록

## Internal Testing 수동 검증

1. 2.2.13 설치 상태에서 2.2.14로 업데이트한다.
2. 기존 목표와 기존 24개 이모지가 유지되고, catalog 밖 이모지는 시스템 fallback으로 표시되는지 확인한다.
3. 독립 이모지 시트에서 선택하면 즉시 저장·닫기 되는지 확인한다.
4. 셀 편집 시트에서 선택한 이모지는 저장 전 draft로만 반영되고 취소 시 목표 값이 유지되는지 확인한다.
5. 검색·9개 카테고리로 항목을 찾을 수 있는지 확인한다.
6. 최근 사용이 최신순·중복 없이 표시되고 앱 재실행 후에도 유지되는지 확인한다.
7. 라이트·다크 모드에서 투명 asset 가장자리, 선택 border/check와 텍스트 대비를 확인한다.
8. Play Console에서 2.2.13 대비 2.2.14 다운로드 크기 증가가 5MB 이하인지 확인한다.

## 중단 조건

- Play 전체 트랙 최대 versionCode가 `20214` 이상이다.
- clean AAB의 버전 또는 Compose resource namespace가 예상과 다르다.
- AAB 업로드 전 자동 검증이 실패한다.
- Play Console 기준 다운로드 크기 증가가 5MB를 초과한다.
- 관리자 권한으로 branch rule 또는 review 조건을 우회해야 한다.

## 완료 조건

- PR의 코드 리뷰와 CI가 통과하고 관리자 우회 없이 `main`에 병합된다.
- 2.2.14 (`20214`)가 Play Internal Testing에 업로드된다.
- clean AAB 크기와 Play 다운로드 크기 증가량이 문서에 기록된다.
- Internal Testing 수동 검증 항목이 통과하면 #212의 Android 배포 범위를 닫고, 남은 iOS 출시 검증은 #214에서 이어간다.
