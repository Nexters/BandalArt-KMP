# iOS TestFlight 업데이트 검증 전략

- 작성일: 2026-08-08
- 관련 이슈: [#214](https://github.com/Nexters/BandalArt-KMP/issues/214)
- 기준 브랜치: 최신 `main` 기반 `test/ios-testflight-1.1.0-1`

## 현재 기준점

- App Store에 공개된 최신 버전은 `1.0.1 (2)`이며 공개일은 2025-03-24다.
- App Store Connect 조회에서 과거 build는 `1.0.1 (2)`와 `1.0.0 (1~3)`뿐이고 모두 만료됐다. 활성 TestFlight build는 없다.
- 새 기능 release의 marketing version은 `1.1.0`으로 확정했다. `iosApp/iosApp/Info.plist`는 `CFBundleShortVersionString = 1.1.0`, `CFBundleVersion = 1`을 직접 가진다.
- Xcode project는 Debug·Release 모두 `GENERATE_INFOPLIST_FILE = NO`이고 `INFOPLIST_FILE = $(SRCROOT)/iosApp/Info.plist`를 사용한다. 따라서 현재 bundle version의 source-of-truth는 Xcode build setting이 아니라 이 `Info.plist`다.
- Xcode build setting에는 별도로 `MARKETING_VERSION = 1.0`, `CURRENT_PROJECT_VERSION = 1`이 남아 있지만 현재 `Info.plist`가 이 변수를 참조하지 않으므로 archive의 version/build를 결정하지 않는다.
- committed build `1`은 신규 `1.1.0` train의 seed다. CD는 업로드 직전에 같은 train의 최신 build를 조회해 `+1`을 runner checkout에만 반영하므로 source를 반복 수정하지 않는다.
- Circuit + Metro 기반 KMP 마이그레이션은 `main`에 병합됐고 iOS simulator framework build까지 CI에서 검증했다. 실제 기기, 기존 설치 업데이트와 TestFlight 배포 검증은 #214에 남아 있다.
- Fluent Emoji의 공통 renderer, picker·최근 사용과 Android 출시 검증은 완료됐고 PR #245에서 metadata category 탐색을 추가했다. #214가 요구하는 검색 UI는 현재 `main`에 없어 완료 전 구현하거나 이슈 범위를 명시적으로 조정해야 한다. iOS 렌더링·저장·artifact 크기 검증은 #214로 이관됐다.
- PR #243의 보상형 생성 안내와 복구 flow는 공통 UI에 반영됐다. iOS는 광고 SDK가 없는 동안 안내 뒤 fail-open으로 한 개를 생성하는 경로를 검증한다.
- Firebase Analytics·Crashlytics의 프로젝트 설정은 복구됐다. DebugView, test crash, 개인정보 공개 정합성과 Firebase symbolication은 이번 TestFlight 기회에 확인할 수 있지만 #214의 필수 완료 gate로 삼지 않는다.
- iOS Release의 미사용 폰트는 정리됐지만 실제 다운로드·설치 크기는 아직 App Thinning report와 App Store Connect 수치로 확인하지 못했다.

근거 문서:

- [IOS_RELEASE_SIZE_OPTIMIZATION_RESEARCH.md](./IOS_RELEASE_SIZE_OPTIMIZATION_RESEARCH.md)
- [IOS_RELEASE_SIZE_OPTIMIZATION_STRATEGY.md](./IOS_RELEASE_SIZE_OPTIMIZATION_STRATEGY.md)
- [IOS_RELEASE_SIZE_BASELINE.md](./IOS_RELEASE_SIZE_BASELINE.md)
- [FIREBASE_IOS_INTEGRATION_RESEARCH.md](./FIREBASE_IOS_INTEGRATION_RESEARCH.md)

## 목표

1. 신규 marketing version `1.1.0`과 App Store Connect의 같은 train 최신 build를 대조해 업로드 값을 확정한다.
2. 최신 `main`의 `1.1.0` Release archive를 TestFlight에 업로드하고 실제 iPhone에서 신규 설치와 기존 App Store 버전 위 업데이트를 검증한다.
3. 업데이트 뒤 Room/DataStore 데이터, Circuit navigation, Metro platform binding과 주요 사용자 흐름이 유지되는지 확인한다.
4. Fluent Emoji의 iOS 렌더링·검색·category·최근 사용·저장·공유 결과를 검증한다. 검색 UI가 없는 현재 build는 알려진 #214 미완료 항목으로 기록한다.
5. App Thinning과 App Store Connect의 기기별 수치로 Release artifact 크기 기준선을 기록한다.

## 2026-08-08 signing·platform 사전 점검 결과

- 로컬 도구는 Xcode 26.6이다.
- Xcode에 배포 권한이 있는 Apple account와 유료 Team이 구성되어 있다.
- `security find-identity -v -p codesigning`에서 유효한 Apple Development identity 1개를 확인했다. 인증서 fingerprint는 문서에 기록하지 않는다.
- 새 Apple Development certificate의 유효 기간은 2026-08-08부터 2027-08-08까지다.
- bundle identifier `com.nexters.bandalart.iosApp`의 automatic development provisioning profile이 생성됐고 2027-08-08까지 유효하다.
- iOS 26.5 platform runtime 8.52GB를 설치했다. `showdestinations`에서 `Any iOS Device`와 iOS 26.5 simulator destination을 확인했다.
- Release `showBuildSettings`는 `CODE_SIGN_IDENTITY = Apple Development`, `CODE_SIGN_STYLE = Automatic`과 올바른 Team·bundle identifier를 선택한다.

로컬 Apple Distribution certificate가 없다는 사실만으로 Xcode Organizer 배포를 막지 않는다. Apple 공식 문서에 따르면 Xcode 13 이상은 로컬 distribution certificate가 없을 때 cloud-managed certificate로 Organizer 배포를 signing할 수 있다. 실제 배포 가능 여부는 사용자가 Organizer validation과 upload로 확인한다.

- [Apple: Cloud-managed certificates](https://developer.apple.com/help/account/certificates/cloud-managed-certificates)

## 선행 조건

- Apple Developer Program 계정과 App Store Connect 앱에 접근할 수 있다.
- Xcode에 올바른 Apple account와 유료 Team이 연결되어 있다.
- bundle identifier `com.nexters.bandalart.iosApp`, Apple Development certificate와 automatic provisioning profile이 유효하고 Xcode가 이를 인식한다.
- iOS platform과 archive 대상 destination이 Xcode에 표시된다.
- App Store Connect에서 현재 App Store version, 업로드된 build 목록, 다음 업로드에 사용 가능한 version/build train을 확인할 수 있다.
- TestFlight 업로드 권한과 검증 대상 테스터·실제 iPhone이 준비되어 있다.
- 기존 App Store `1.0.1`을 설치해 업데이트 전 데이터를 만들 수 있는 기기가 준비되어 있다.

2026-08-08의 development signing과 platform 로컬 사전 점검은 통과했다. keychain·profile 상태는 바뀔 수 있으므로 실제 archive 직전에 identity와 destination을 다시 확인한다. CD 경로에는 별도의 Apple Distribution `.p12`, App Store provisioning profile과 GitHub `ios-testflight` Environment secrets가 필요하다. 수동 Organizer 경로는 Xcode cloud-managed distribution signing을 fallback으로 사용할 수 있다.

## 버전과 build number 결정 원칙

1. marketing version source는 현재처럼 `Info.plist`의 직접값 `1.1.0`으로 유지한다.
2. Xcode project의 잔여 `MARKETING_VERSION = 1.0`, `CURRENT_PROJECT_VERSION = 1`만 변경해 실제 bundle version이 바뀐 것으로 간주하지 않는다.
3. CD는 App Store Connect에서 같은 `1.1.0` train의 모든 build를 조회하고 최신값 `+1`을 runner의 `CFBundleVersion`에 반영한다.
4. 수동 Organizer fallback은 archive 전에 같은 train의 최신 build를 확인하고 미사용 build를 `Info.plist`에 반영한다.
5. 생성된 app bundle과 archive의 `CFBundleShortVersionString`, `CFBundleVersion`이 선택값과 일치하는지 확인한다.
6. 값이 충돌하면 임의 증가를 반복하지 않고 App Store Connect 이력과 archive metadata를 다시 대조한다.

## 실행 단계

### 1. App Store Connect와 로컬 설정 대조

- App Store app, bundle identifier, 공개 version과 build history를 확인한다.
- TestFlight의 만료·처리 중·사용 가능 build와 새 업로드가 들어갈 version train을 확인한다.
- `Info.plist`의 `CFBundleShortVersionString`, `CFBundleVersion`, Xcode project의 관련 build setting, signing team과 bundle identifier를 기록한다.
- marketing version source가 `Info.plist`의 `1.1.0`인지 확인한다.
- CD는 같은 train 최신 build `+1`, 수동 fallback은 확인한 미사용 build를 선택한다.
- 생성된 app bundle에서 실제 version/build 값을 확인해 source 선택이 적용됐는지 검증한다.

### 2. TestFlight 배포 경로 선택과 Release archive 준비

- 최신 `main`을 기준으로 Xcode package dependency와 KMP Release framework가 정상적으로 해석되는지 확인한다.
- Release effective build settings를 확인한다.
- CD workflow가 병합되고 `ios-testflight` Environment의 Distribution `.p12`, App Store profile, Individual ASC key가 준비되면 GitHub `release-cd`의 iOS job을 기본 경로로 사용한다.
- CD workflow·Environment·secret 같은 자동 실행 인프라만 준비되지 않은 경우에는 Xcode Organizer를 수동 fallback으로 사용할 수 있다. 이 경우에도 ASC 인증·권한, 같은 `1.1.0` train 조회와 미사용 build 확인, signing validation을 독립적으로 통과해야 한다.
- ASC 인증·권한, train 조회, version/build 충돌 또는 signing preflight 실패는 Organizer로 우회하지 않고 배포를 중단한다.
- 수동 fallback의 Archive 생성과 Organizer 업로드는 사용자가 Xcode에서 수행한다. CD 경로의 archive/upload는 workflow와 Fastlane lane이 담당한다.
- 수동 fallback에서 사용자가 생성한 `.xcarchive` 경로를 전달하면 앱 executable, `ComposeApp.framework`와 resource 구성을 분석한다.
- Archive export에서 App Thinning을 `All compatible device variants`로 설정하고 `App Thinning Size Report.txt`를 보존한다.

### 3. TestFlight 업로드와 설치

- 선택한 CD 또는 Organizer 경로의 validation을 통과한 archive를 App Store Connect에 업로드한다.
- processing, export compliance, 개인정보 또는 signing 경고를 확인한다.

내부 테스터에게 배포하기 전에 TestFlight의 `What to Test`에 다음 내용을 간결하게 기록한다.

- App Store `1.0.1`을 삭제하지 않고 업데이트한 뒤 기존 반다라트와 설정이 유지되는지 확인한다.
- Splash/Home/편집/완료/저장·공유 등 Circuit + Metro 주요 흐름을 확인한다.
- Fluent Emoji의 표시, category·최근 사용과 저장 결과를 확인한다.
- 검색 UI는 현재 build의 알려진 미구현 항목으로 남기고 #214 완료 전에 구현·검증하거나 이슈 범위를 명시적으로 조정한다.
- 보상형 생성 안내를 확인하고, iOS에서는 광고 SDK가 없는 현재 정책에 따라 fail-open으로 정확히 한 개가 생성되는지 확인한다.
- 문제는 `TestFlight 앱 > BandalArt > Send Beta Feedback`으로 스크린샷, 기기·iOS 버전과 기대/실제 결과를 함께 보낸다. 담당자는 결과를 #214 comment에 정리하고 차단 회귀는 연결된 별도 이슈로 등록한다.

- TestFlight에서 해당 build를 내부 테스터에게 배포한다.
- 실제 iPhone에 신규 설치하고 초기 진입부터 주요 흐름을 검증한다.
- 별도 기기 또는 복구 가능한 검증 기기에서 App Store `1.0.1`로 데이터를 만든 뒤 TestFlight build를 덮어 설치한다.

### 4. #214 기능·데이터 회귀 검증

- 기존 Room 데이터, 선택한 반다라트와 최근 선택 상태가 업데이트 뒤 유지되는지 확인한다.
- DataStore의 온보딩 완료 여부, 테마와 Fluent Emoji 최근 사용 기록이 유지되는지 확인한다.
- Splash에서 Onboarding/Home으로 올바르게 분기하는지 확인한다.
- Home 목록, 표 생성·편집·삭제·완료, BottomSheet/Dialog 상태와 날짜 입력을 확인한다.
- Complete 이동·뒤로가기, 이미지 저장·공유와 iOS platform launcher를 확인한다.
- 라이트/다크/System 테마, foreground/background 복귀와 앱 재실행을 확인한다.
- 주요 흐름에서 crash, 중복 navigation 또는 Metro platform binding 누락이 없는지 확인한다.

### 5. Fluent Emoji·artifact 크기 검증

- 기존 Unicode와 catalog 밖 이모지가 시스템 fallback으로 표시되는지 확인한다.
- Fluent Color 이모지가 Home·목록·편집·Complete·공유 결과에서 같은 의미와 스타일로 표시되는지 확인한다.
- 검색 결과 필터, 9개 카테고리, 최근 사용 순서·중복 제거와 즉시 저장/편집 draft 흐름을 확인한다. 검색 UI가 아직 없는 build에서는 해당 항목을 실패/미완료로 기록한다.
- 동일 iPhone variant의 compressed download size와 installed size를 기록하고 App Store `1.0.1` 공개 수치와 산정 방식이 다름을 함께 명시한다.

## 선택 artifact·Firebase 관측 검증

아래 항목은 이번 TestFlight 기회에 수행할 수 있지만 #214의 필수 완료 조건은 아니다. 산출물 확인이 어렵거나 계정·콘솔 권한과 수집 대기 때문에 실패해도 TestFlight 업데이트, 데이터 보존과 핵심 기능 검증이 통과했다면 #214 전체를 막지 않고 별도 후속 이슈로 분리한다.

- archive의 앱 dSYM과 `ComposeApp.framework.dSYM` 존재 여부와 UUID 확인
- Firebase Analytics DebugView에서 iOS event 확인
- debugger를 분리한 Crashlytics test crash 수집과 Firebase console symbolication 확인
- App Store Connect 개인정보 공개와 실제 Analytics·Crashlytics 수집 내용 대조
- Firebase Missing dSYM 경고와 자동 업로드 상태 확인

선택 검증을 수행했다면 Firebase debug launch argument와 test-only crash trigger가 배포 코드에 남지 않았는지 반드시 확인한다.

## 수동 검증 체크리스트

### 설치와 데이터

- [ ] 실제 iPhone 신규 설치 성공
- [ ] App Store `1.0.1` 위 TestFlight 업데이트 성공
- [ ] 기존 반다라트, cell 내용, 완료 상태와 선택 항목 유지
- [ ] 온보딩 완료 여부, 테마와 최근 이모지 유지
- [ ] 종료·재실행 뒤에도 같은 데이터와 설정 유지

### 주요 기능

- [ ] Splash → Onboarding/Home 분기
- [ ] Home 목록과 표 생성·편집·삭제·완료
- [ ] BottomSheet/Dialog와 날짜 입력·복원
- [ ] Complete 이동과 뒤로가기
- [ ] 이미지 저장·공유와 iOS launcher
- [ ] 라이트/다크/System 테마
- [ ] foreground/background 전환과 재실행

### Fluent Emoji

- [ ] 기존 Unicode와 catalog 밖 Unicode fallback
- [ ] Home·목록·편집·Complete·공유 렌더링
- [ ] 검색 UI와 결과 필터 — 현재 `main` 미구현, #214 완료 전 해결 또는 범위 조정
- [ ] 9개 카테고리 탐색
- [ ] 최근 사용 중복 제거·최신순·재실행 유지
- [ ] 독립 picker 즉시 저장과 편집 draft 저장/취소

### 배포와 artifact

- [ ] App Store Connect processing 및 TestFlight 배포 성공
- [ ] App Thinning report와 대표 기기 크기 기록
- [ ] 주요 흐름 crash·중복 navigation·DI 누락 없음

### 선택 artifact·Firebase 관측

- [ ] 앱·Compose dSYM 존재와 UUID 확인
- [ ] Analytics DebugView event 확인
- [ ] Crashlytics test crash 수집과 Firebase console symbolication 확인
- [ ] App Store Connect 개인정보 공개와 Firebase 수집 내용 대조
- [ ] 선택 검증을 수행했다면 test-only crash trigger와 Firebase debug flag 제거

## 실패 처리

- signing, version/build 충돌 또는 App Store Connect 업로드 권한 문제는 코드로 우회하지 않고 blocker로 기록한다.
- 기존 설치 업데이트에서 데이터가 사라지면 신규 설치 검증을 계속해 성공으로 간주하지 않고 저장 경로와 migration을 우선 조사한다.
- 선택 관측 항목의 콘솔 권한, 개인정보 공개 정합성 또는 Firebase 수집·symbolication 실패는 별도 후속 이슈로 기록하며 #214 핵심 검증의 성공을 뒤집지 않는다.
- 발견한 iOS 전용 제품 회귀는 #214에 결과를 남기고 별도 fix 이슈와 PR로 분리한다.
- TestFlight 또는 App Store의 표시 크기를 `.xcarchive` 전체 크기와 직접 비교하지 않는다.

## 비범위

- App Store production 출시와 심사 제출
- 같은 `1.1.0` train 조회 없이 build number를 임의 지정하는 절차
- 새로운 제품 기능 또는 UI 추가
- AdMob iOS SDK와 광고 노출
- Firebase SDK major upgrade 또는 공통 Analytics abstraction 도입
- dSYM 제거, 필수 architecture 제거 또는 근거 없는 linker 최적화
- Android 배포와 Android 회귀 검증

## 완료 조건

- `1.1.0` marketing version과 같은 train에서 선택한 build number가 archive에 일치한다.
- 최신 `main` archive가 TestFlight에 업로드되고 실제 iPhone 신규 설치와 App Store `1.0.1` 위 업데이트가 성공한다.
- 기존 Room/DataStore 데이터와 설정이 업데이트 뒤 유지된다.
- Circuit 화면, Metro DI, 플랫폼 launcher와 주요 사용자 흐름이 실제 기기에서 정상 동작한다.
- Fluent Emoji의 iOS 렌더링·검색·category·최근 사용·저장·공유 검증을 통과하거나, 검색 요구를 #214에서 제외하는 범위 변경이 명시적으로 승인된다.
- App Thinning과 App Store Connect 기반의 기기별 크기 결과가 기록된다.
- 발견한 iOS 전용 회귀가 별도 작업으로 분리된다.
