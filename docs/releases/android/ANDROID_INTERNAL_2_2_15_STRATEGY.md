# Android Internal 2.2.15 배포 전략

## 목적

PR #236에서 복원한 이모지 선택 Bottom Sheet UI와 PR #239의 미사용 폰트 제거를 Android Internal Testing에서 분리 검증한다.

## 배포 기준

- source: 최신 `main`의 `cd968ee`
- package: `com.nexters.bandalart`
- version: `2.2.15 (20215)`
- track: `Internal Testing`
- status: `completed`

## 포함 범위

- 기존 반다라트 UI와 일치하는 이모지 Bottom Sheet 디자인
- Fluent Color 이모지 300개
- 검색, 9개 카테고리, 최근 사용 목록
- 긴 이모지 목록 스크롤
- 기존 Unicode 이모지 fallback과 저장 데이터 유지
- 미사용 Pretendard 5개 굵기 제거 후 400/500/600/700 표시

## 비범위

- AdMob 슬롯·광고 SDK 작업
- iOS TestFlight/App Store 배포
- Firebase iOS 런타임 검증
- Production 승격

## 검증 항목

- 기존 앱 위에 2.2.15 업데이트 후 Room/DataStore 데이터 유지
- 이모지 선택 Bottom Sheet가 기존 레이아웃과 같은 형태로 표시
- 이모지 grid 스크롤, 검색, 카테고리, 최근 사용 정상
- 선택 즉시 저장과 셀 편집 저장/취소 정상
- 기존 저장 이모지와 fallback 렌더링 정상
- 라이트·다크 모드의 글꼴 굵기와 이모지 선택 상태 정상
- Play Console의 다운로드 크기 변화 확인

## 배포 절차

1. version과 3개 언어 Internal release notes 검토
2. CI 통과 후 `main` 일반 merge
3. Play 전체 track의 최대 versionCode가 20215보다 작은지 확인
4. clean release AAB 생성 및 서명·리소스 namespace 검증
5. Gradle Play Publisher로 Internal Testing 업로드
