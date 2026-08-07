# iOS Release 앱 크기 최적화 전략

## 배경

App Store에 공개된 iOS `1.0.1`은 75.1MB로 표시되지만 현재 `main`은 당시보다 폰트 중복이 제거됐고 Fluent Emoji와 Firebase 설정 등 기능 구성도 달라졌다. App Store 표시 크기, `.xcarchive`, `.ipa`, App Thinning의 기기별 다운로드·설치 크기는 서로 다른 지표이므로 동일 코드의 Release 기준선을 먼저 확보한다.

관련 이슈는 [#234](https://github.com/Nexters/BandalArt-KMP/issues/234)이며, Firebase Analytics·Crashlytics 복구는 PR #237에서 선행됐다.

## 목표

- 최신 `main`의 동일 Release 코드로 비교 가능한 크기 기준선을 만든다.
- 실행 파일, Kotlin/Native·Compose framework, SPM dependency, 정적 리소스별 기여도를 식별한다.
- 측정으로 감소 효과가 확인된 안전한 설정·리소스 변경만 반영한다.
- dSYM, Crashlytics symbolication, App Thinning과 기존 데이터 호환을 유지한다.

## 원칙

1. Archive 측정 전에는 compiler/linker 최적화 설정을 추측으로 추가하지 않는다. 명백히 미사용인 정적 리소스는 원본 기준선을 기록한 뒤 제거할 수 있다.
2. 한 실험에는 한 종류의 변경만 적용한다.
3. `.xcarchive` 전체 크기를 사용자 다운로드 크기로 보고하지 않는다.
4. App Thinning report의 동일 iPhone variant를 변경 전후 기준으로 사용한다.
5. dSYM과 필수 architecture를 크기 감소 목적으로 제거하지 않는다.
6. 감소 효과가 미미하거나 기능·성능·심볼화 위험이 큰 변경은 적용하지 않고 결과만 기록한다.

## 1단계: 정적 기준선

- Debug·Release effective build settings를 파일로 기록한다.
- 다음 Release 값을 확인한다.
  - `SWIFT_OPTIMIZATION_LEVEL`
  - `SWIFT_COMPILATION_MODE`
  - `DEAD_CODE_STRIPPING`
  - `STRIP_INSTALLED_PRODUCT`
  - `STRIP_STYLE`
  - `STRIP_SWIFT_SYMBOLS`
  - `ENABLE_TESTABILITY`
  - `DEBUG_INFORMATION_FORMAT`
- Compose resources, 폰트, Fluent Emoji, iOS asset catalog의 파일 개수와 합계를 기록한다.
- SPM 제품과 Kotlin/Native framework linkage를 기록한다.

정적 기준선 결과는 [IOS_RELEASE_SIZE_BASELINE.md](./IOS_RELEASE_SIZE_BASELINE.md)에 기록한다. 현재 첫 실험 후보는 UI에서 사용하지 않는 Pretendard `Thin`, `ExtraLight`, `Light`, `ExtraBold`, `Black` 다섯 굵기다. 원본 리소스 기준 합계는 7,779,960B이며, 실제 감소량은 동일 App Thinning variant로 검증하기 전까지 확정값으로 취급하지 않는다.

## 2단계: Release Archive 기준선

저장소 지침에 따라 Archive 생성은 사용자가 Xcode에서 수행한다. 이 변경이 병합된 최신 `main`의 Archive가 준비되면 다음을 자동 분석한다.

- `.xcarchive/Products/Applications/*.app` 전체와 하위 상위 용량 항목
- 앱 executable과 embedded framework의 파일 크기·architecture·UUID
- 앱 및 `ComposeApp.framework` dSYM의 존재와 UUID
- bundled Compose resources, 폰트, Fluent Emoji와 Firebase/SPM 구성요소
- preview/debug/test 전용 파일 포함 여부

Archive export는 App Thinning을 `All compatible device variants`로 설정하고 `App Thinning Size Report.txt`를 보존한다.

## 3단계: 최적화 후보 결정

기준선에서 실제 기여도가 확인된 경우에만 아래 순서로 실험한다.

1. 누락된 Release compiler/linker 또는 strip 설정 보정
2. 중복·미사용 Compose/iOS 리소스 제거
3. 이미지 format·해상도 조정
4. 미사용 SPM product 또는 native dependency 제거
5. 현재 Kotlin 버전에서 공식 지원되는 binary size 옵션 실험

Firebase Analytics·Crashlytics는 제품 요구사항으로 유지한다. 제거 실험이 필요해도 크기 기여도 측정 목적으로만 별도 비교하고 실제 제품 브랜치에서는 제거하지 않는다.

## 검증

- plist와 Xcode 프로젝트 형식 정상
- Release archive 및 App Thinning export 성공
- 동일 iPhone variant의 compressed download·installed size 비교
- 앱·Compose dSYM 생성과 Crashlytics 업로드 유지
- TestFlight 신규 설치 및 기존 앱 위 업데이트 성공
- Circuit 화면, Room/DataStore 데이터, Fluent Emoji 정상
- 변경 전후 용량과 성능·기능 영향 문서화

## 비범위

- dSYM 비활성화 또는 삭제
- 필수 architecture 수동 제거
- 근거 없는 static/dynamic framework 전환
- iOS 난독화 도구 도입
- Firebase 연동 제거
- Android 앱 크기 최적화

## 완료 조건

- 최신 동일 코드의 기기별 다운로드·설치 크기 기준선이 기록된다.
- 용량 상위 구성요소가 실행 파일·framework·resource 단위로 설명된다.
- 효과가 확인된 안전한 최적화만 코드에 반영된다.
- 최종 감소량과 App Store Connect 결과가 #234와 #214에 연결된다.
