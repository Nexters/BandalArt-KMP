# iOS WidgetKit 반다라트 위젯 MVP 전략

## 1. 배경과 목표

- Parent: #156
- Issue: #295
- Depends on: Android/common widget foundation PR #294
- Base branch: `feat/android-widget-mvp`

Android PR에서 정의한 `BandalartWidgetRepository`의 snapshot과 소유 관계 검증 계약을 iOS WidgetKit에서도 사용한다. 본 앱을 열지 않고 선택한 반다라트의 진행 상태를 보고, iOS 17 이상에서 하위 태스크 완료 여부를 변경할 수 있게 한다.

## 2. 현재 제약

- 본 앱 iOS target은 16.6을 지원하고 Widget Extension target이 없다.
- iOS Room DB는 본 앱의 `NSDocumentDirectory/bandalart.db`에 있어 별도 프로세스인 extension에서 접근할 수 없다.
- `ComposeApp.framework`는 Compose, Firebase, 광고, UIKit 앱 코드를 포함하므로 widget extension에 직접 링크하지 않는다.
- 현재 서명·TestFlight 자동화는 `iosApp` target과 provisioning profile 하나만 처리한다.

## 3. 구현 경계

### 3.1 App Group과 DB

- 앱과 extension에 `group.com.nexters.bandalart` App Group entitlement를 적용한다.
- iOS Room DB 경로를 App Group container로 이동한다.
- 기존 Documents DB와 `-wal`, `-shm` sidecar를 DB open 전에 새 경로로 1회 이전한다.
- extension은 shared base가 없으면 DB를 만들지 않는다. legacy와 shared base가 동시에 발견되는 예외 상태에서는 어느 한쪽을 암묵적으로 선택하지 않고 본 앱 초기화를 중단해 두 파일을 보존한다.

### 3.2 extension용 KMP 경계

- 신규 `iosWidgetShared` KMP module을 iOS static framework로 생성한다.
- extension에는 Compose/Firebase/Ads를 포함한 `ComposeApp.framework`를 링크하지 않는다.
- `IosWidgetDataBridge`는 Room DAO를 통해 다음만 노출한다.
  - 반다라트·서브 목표 선택 목록
  - 선택된 widget snapshot
  - 소유 관계를 검증하는 명시적 task completion 변경
- 앱과 extension의 동시 접근은 SQLite transaction을 사용하고, 앱 활성화 시 Room invalidation refresh와 알림 재조정을 수행한다.

### 3.3 WidgetKit

- 본 앱 deployment target 16.6은 유지한다.
- Widget Extension은 interactive widget을 위해 iOS 17.0+로 제한한다.
- `AppIntentConfiguration`과 dynamic `AppEntity` query로 반다라트·서브 목표를 선택한다.
- `systemSmall`은 이모지, 메인 목표, 전체 달성률을 표시한다.
- `systemMedium`, `systemLarge`는 선택한 서브 목표와 하위 태스크를 표시한다.
- 태스크는 `Toggle(intent:)` 또는 `Button(intent:)`로 변경하고 `perform()` 완료 후 timeline을 다시 읽는다.
- 앱에서 DB가 변경되면 `WidgetCenter.reloadTimelines(ofKind:)`을 호출한다.

### 3.4 deep link

- widget root는 `bandalart://widget?bandalartId={id}`를 사용한다.
- SwiftUI 앱의 `onOpenURL`에서 ID를 검증하고 buffered KMP bridge에 기록한다.
- KMP bridge는 기존 `BandalartWidgetLaunchTarget`에 연결하여 cold/warm launch에서 같은 Home 선택 계약을 사용한다.

## 4. Xcode·서명 변경

- `BandalartWidget` Widget Extension target과 Embed App Extensions phase를 추가한다.
- 앱과 extension에 App Group entitlement를 적용한다.
- extension bundle ID는 `com.nexters.bandalart.iosApp.widget`을 사용한다.
- simulator build는 provisioning profile 없이 검증한다.
- TestFlight archive 코드에는 앱·extension provisioning profile 검증과 export mapping을 포함한다. Apple Developer의 extension App ID·App Group 활성화, 두 profile 발급과 CI secret 등록은 외부 상태이므로 로컬 코드가 자동 생성하지 않는다.
- release workflow와 Fastlane은 앱·extension profile을 각각 검증·설치하고 동일 version/build를 주입하도록 확장한다. 실제 archive에는 `IOS_WIDGET_PROVISIONING_PROFILE_BASE64` secret과 App Group이 포함된 두 profile이 필요하다.

## 5. 테스트 계약

- App Group path 결정과 legacy DB/sidecar 이전
- 선택 목록에서 빈 반다라트·서브 목표 제외
- 삭제된 선택의 null fallback
- 다른 반다라트·서브 목표의 task 변경 거부
- task completion 변경 후 snapshot과 완료율 갱신
- deep-link URL parsing과 buffered target 전달
- Xcode target, embed phase, entitlement, bundle ID, deployment target 정적 검증
- `iosWidgetShared` framework link과 Widget Extension simulator build

## 6. 실행 순서

1. App Group DB 경로·legacy migration seam을 작성하고 테스트한다.
2. `iosWidgetShared` framework과 bridge를 작성한다.
3. Widget Extension target, AppIntent configuration, family별 SwiftUI를 추가한다.
4. 본 앱 timeline refresh과 deep-link bridge를 연결한다.
5. Xcode project 구성과 simulator build를 검증한다.
6. 별도 리뷰로 extension-safe API, DB migration, 서명 파급을 확인한다.

## 7. 실패 기록 계약

Kotlin/Native framework가 Widget Extension에서 링크되지 않거나 Room/SQLite의 extension 프로세스 제약으로 완주하지 못하더라도 다음을 이 문서에 추가하고 PR을 생성한다.

- 실행한 명령과 확정적인 실패 메시지
- 성공한 중간 단계와 실패 경계
- 시도한 구조와 기각한 대안
- provisioning/App Group 등 외부 상태가 필요한 부분
- 후속 작업이 재개될 수 있는 구체적인 조건

## 8. 공식 근거

- [Developing a WidgetKit strategy](https://developer.apple.com/documentation/widgetkit/developing-a-widgetkit-strategy)
- [Creating a widget extension](https://developer.apple.com/documentation/widgetkit/creating-a-widget-extension)
- [Making a configurable widget](https://developer.apple.com/documentation/widgetkit/making-a-configurable-widget)
- [Adding interactivity to widgets and Live Activities](https://developer.apple.com/documentation/widgetkit/adding-interactivity-to-widgets-and-live-activities)
- [Keeping a widget up to date](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date)
- [Configuring App Groups](https://developer.apple.com/documentation/xcode/configuring-app-groups)
- [FileManager containerURL(forSecurityApplicationGroupIdentifier:)](https://developer.apple.com/documentation/foundation/filemanager/containerurl(forsecurityapplicationgroupidentifier:))

## 9. 작업 이력

### 9.1 완료한 구현

- App Group DB 경로와 legacy DB/sidecar 이전 정책을 구현했다.
- extension이 본 앱보다 먼저 실행될 때 빈 shared DB를 생성하지 않도록 기존 shared DB가 있을 때만 bridge를 열고, legacy와 shared base가 동시에 있으면 어느 쪽도 암묵적으로 버리지 않고 중단한다.
- Compose/Firebase/Ads를 포함하지 않는 `iosWidgetShared` static framework와 Swift 전용 모델·bridge를 추가했다.
- Widget Extension, AppIntent 기반 설정, interactive task button, family별 SwiftUI와 deep link를 추가했다.
- 본 앱 활성화 시 Room invalidation refresh와 timeline reload를 수행하고, widget URL을 기존 Home 선택 경로로 전달한다.
- TestFlight workflow에 extension profile 복원·서명·export mapping과 nested `.appex` 검증을 추가했다.

### 9.2 확인한 제약과 해결

1. Kotlin/Native 테스트 러너가 simulator test 실행 후 종료되지 않았다.
   - DB migration과 순수 mapping 계약은 JVM host test로 분리해 검증했다.
   - 실제 iOS 경계는 `:iosWidgetShared:linkDebugFrameworkIosSimulatorArm64`와 Widget Extension Xcode build로 검증했다.
2. 최초 generic simulator build는 Xcode가 `x86_64`도 요청해 `iosSimulatorArm64`만 제공하는 KMP framework와 맞지 않았다.
   - `ARCHS=arm64 ONLY_ACTIVE_ARCH=YES`로 Apple Silicon simulator 산출물만 요청해 해결했다.
3. 설정 기본값 조회를 nil-coalescing autoclosure 안에서 `await`해 Swift 컴파일이 실패했다.
   - async 조회를 명시적인 분기로 분리했고 이후 extension과 host app을 포함한 build가 통과했다.

### 9.3 검증 명령과 결과

- `./gradlew :core:database:androidHostTest` — migration을 포함한 17개 테스트 통과
- `./gradlew :iosWidgetShared:jvmTest` — mapping 3개 테스트 통과
- `./gradlew :core:data:androidHostTest --tests '*BandalartWidgetRepositoryTest'` — repository 10개 테스트 통과
- `./gradlew :iosWidgetShared:allTests` — 통과
- `./gradlew :iosWidgetShared:linkDebugFrameworkIosSimulatorArm64` — static framework link 통과
- `python3 -m unittest scripts.tests.test_ios_widget_project` — Xcode target/entitlement/embed/TestFlight 구성 4개 테스트 통과
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme BandalartWidget -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath /private/tmp/bandalart-ios-widget-derived CODE_SIGNING_ALLOWED=NO ARCHS=arm64 ONLY_ACTIVE_ARCH=YES build` — Widget Extension과 host app build 통과

### 9.4 외부 상태가 필요한 잔여 검증

- Apple Developer에서 App Group과 extension App ID를 활성화하고 앱·extension provisioning profile을 발급해야 한다. 이후 `IOS_PROVISIONING_PROFILE_BASE64`를 교체하고 `IOS_WIDGET_PROVISIONING_PROFILE_BASE64`를 추가한다. 코드 경로는 준비했지만 이 외부 상태가 없으므로 signed archive/export는 실행하지 않았다.
- 실제 기기에서 기존 Documents DB의 App Group 이전, 위젯 추가·재설정·삭제, interactive task 변경과 cold/warm deep link를 확인해야 한다.
- TestFlight workflow의 extension profile 복원과 export mapping 코드는 완료됐다. signed archive는 두 profile과 GitHub secret을 등록한 뒤 검증한다.

## 10. TestFlight용 App Group과 서명 자산을 등록하는 방법

이 절차는 PR #296을 병합하고 TestFlight에 배포하기 전에 필요한 Apple Developer와 GitHub 설정을 설명한다. Apple Developer의 **Account Holder** 또는 **Admin** 권한이 필요하다.

### 10.1 등록할 식별자

세 식별자가 아래 값과 정확히 일치해야 한다:

- **App Group**: `group.com.nexters.bandalart`
- **본 앱 App ID**: `com.nexters.bandalart.iosApp`
- **위젯 App ID**: `com.nexters.bandalart.iosApp.widget`

위젯은 본 앱에 포함되는 extension이다. 위젯용 App Store Connect 앱 레코드는 만들지 않는다.

### 10.2 App Group 등록

App Group이 이미 있으면 새로 만들지 말고 기존 항목을 사용한다.

1. [Apple Developer의 Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/identifiers/list)로 이동한다.
2. **Identifiers**에서 추가 버튼을 누른다.
3. **App Groups**를 선택하고 계속한다.
4. Description에 `BandalArt`를 입력한다.
5. Identifier에 `group.com.nexters.bandalart`를 입력한다.
6. 값을 확인한 뒤 등록한다.

Apple은 App Group 등록에 Account Holder 또는 Admin 권한을 요구한다. 자세한 절차는 [Apple의 App Group 등록 문서](https://developer.apple.com/help/account/identifiers/register-an-app-group/)를 참고한다.

### 10.3 본 앱과 위젯 App ID에 App Group 연결

두 App ID 모두 같은 App Group을 사용해야 공유 데이터베이스에 접근할 수 있다.

1. **Identifiers**에서 `com.nexters.bandalart.iosApp`을 선택한다.
2. **Capabilities**에서 **App Groups**를 활성화한다.
3. **Configure**를 누르고 `group.com.nexters.bandalart`를 선택한다.
4. 변경 사항을 저장한다.
5. `com.nexters.bandalart.iosApp.widget` App ID가 없으면 Explicit App ID로 등록한다.
6. 위젯 App ID에서도 **App Groups**를 활성화한다.
7. **Configure**에서 같은 App Group을 선택하고 저장한다.

Capability를 변경하면 해당 App ID를 사용하는 기존 provisioning profile이 무효화될 수 있다. Apple도 capability 변경 후 profile 재생성을 요구하므로 본 앱 profile과 위젯 profile을 모두 새로 발급한다. 자세한 내용은 [Apple의 capability 활성화 문서](https://developer.apple.com/help/account/identifiers/enable-app-capabilities/)를 참고한다.

### 10.4 App Store Connect 배포 profile 두 개 발급

두 profile은 같은 Apple Distribution 인증서를 사용해야 한다. Profile 이름은 workflow에서 고정하지 않으므로 구분하기 쉬운 이름을 사용한다.

1. **Profiles**에서 추가 버튼을 누른다.
2. **Distribution > App Store Connect**를 선택한다.
3. 본 앱 App ID `com.nexters.bandalart.iosApp`을 선택한다.
4. 기존 `IOS_DISTRIBUTION_CERTIFICATE_BASE64`를 만들 때 사용한 Apple Distribution 인증서를 선택한다.
5. 이름을 `BandalArt AppStore AppGroup`으로 지정하고 profile을 생성한다.
6. `.mobileprovision` 파일을 내려받는다.
7. 같은 절차를 반복하고 위젯 App ID `com.nexters.bandalart.iosApp.widget`을 선택한다.
8. 이름을 `BandalArt Widget AppStore`로 지정하고 profile을 생성한다.
9. 위젯 `.mobileprovision` 파일을 내려받는다.

Apple의 전체 생성 절차는 [App Store Connect provisioning profile 생성 문서](https://developer.apple.com/help/account/provisioning-profiles/create-an-app-store-provisioning-profile/)에서 확인할 수 있다.

### 10.5 내려받은 profile 검증

GitHub secret에 등록하기 전에 두 파일을 로컬에서 확인한다. 다음 명령은 profile 내용을 출력하지만 private key나 인증서 암호를 출력하지 않는다.

예시는 내려받은 파일명을 `BandalArt_AppStore_AppGroup.mobileprovision`과 `BandalArt_Widget_AppStore.mobileprovision`로 바꾼 기준이다. 파일명을 유지하려면 명령의 경로를 실제 이름에 맞춘다.

```bash
main_profile_path="$HOME/Downloads/BandalArt_AppStore_AppGroup.mobileprovision"
widget_profile_path="$HOME/Downloads/BandalArt_Widget_AppStore.mobileprovision"

security cms -D -i "$main_profile_path" | plutil -p -
security cms -D -i "$widget_profile_path" | plutil -p -
```

출력에서 다음 값을 확인한다:

- **TeamIdentifier**: `8R5768RCRA`
- **본 앱 application-identifier**: `8R5768RCRA.com.nexters.bandalart.iosApp`
- **위젯 application-identifier**: `8R5768RCRA.com.nexters.bandalart.iosApp.widget`
- **두 profile의 App Group entitlement**: `group.com.nexters.bandalart`
- **ExpirationDate**: TestFlight 배포일보다 이후
- **get-task-allow**: 없거나 `false`
- **ProvisionedDevices**: 없음
- **ProvisionsAllDevices**: 없음

`get-task-allow`이 `true`면 Development profile이다. `ProvisionedDevices`가 있으면 Ad Hoc profile이므로 TestFlight에 사용할 수 없다.

### 10.6 profile을 base64로 변환

Workflow는 공백 없는 단일 행 base64 문자열을 기대한다. 다음 명령을 각각 실행하면 결과가 클립보드에 복사된다:

```bash
openssl base64 -A \
  -in "$HOME/Downloads/BandalArt_AppStore_AppGroup.mobileprovision" | \
  pbcopy
```

이 값을 `IOS_PROVISIONING_PROFILE_BASE64`에 저장한 뒤 위젯 profile을 변환한다:

```bash
openssl base64 -A \
  -in "$HOME/Downloads/BandalArt_Widget_AppStore.mobileprovision" | \
  pbcopy
```

base64 문자열이나 `.mobileprovision` 파일을 Git, 이슈, PR 댓글 또는 채팅에 올리지 않는다.

### 10.7 GitHub Environment secrets 등록

Workflow의 iOS job은 `ios-testflight` Environment를 사용한다. Repository secret이 아니라 해당 Environment에 등록한다.

1. GitHub 저장소의 **Settings > Environments > ios-testflight**로 이동한다.
2. **Environment secrets**에서 `IOS_PROVISIONING_PROFILE_BASE64`를 연다.
3. 10.6에서 복사한 본 앱 profile의 base64로 기존 값을 교체한다.
4. **Add environment secret**을 누른다.
5. 이름에 `IOS_WIDGET_PROVISIONING_PROFILE_BASE64`를 입력한다.
6. 10.6에서 복사한 위젯 profile의 base64를 붙여 넣고 저장한다.

GitHub는 Environment secret 값을 다시 표시하지 않는다. 등록 여부와 수정 시각은 다음 명령으로 확인한다:

```bash
gh secret list \
  --env ios-testflight \
  --repo Nexters/BandalArt-KMP
```

GitHub Environment secret의 동작은 [GitHub Actions secrets 문서](https://docs.github.com/en/actions/concepts/security/secrets)에서 확인할 수 있다.

### 10.8 등록 완료 체크리스트

아래 항목을 모두 확인한 뒤 PR #296을 병합하고 TestFlight 배포를 실행한다:

- [ ] `group.com.nexters.bandalart` App Group 등록
- [ ] 본 앱 App ID에 App Group 연결
- [ ] 위젯 App ID 등록 및 App Group 연결
- [ ] 본 앱 App Store Connect distribution profile 재발급
- [ ] 위젯 App Store Connect distribution profile 발급
- [ ] 두 profile의 Team ID, bundle ID, App Group, 만료일 검증
- [ ] `IOS_PROVISIONING_PROFILE_BASE64` 교체
- [ ] `IOS_WIDGET_PROVISIONING_PROFILE_BASE64` 추가
- [ ] `gh secret list --env ios-testflight`에서 두 secret 이름 확인

두 secret을 등록해도 실제 signed archive가 성공했다고 간주하지 않는다. 등록 후 release workflow의 profile 사전 검증, 앱과 `.appex` 서명 검증, TestFlight 업로드를 차례로 통과해야 한다.
