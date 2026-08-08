# Firebase iOS Analytics·Crashlytics 연동 점검

## 결론

현재 프로젝트는 Firebase SDK를 추가하고 `FirebaseApp.configure()`를 호출하는 단계까지만 구성되어 있다. Analytics와 Crashlytics가 실제로 동작하기 위한 필수 설정이 빠져 있고, Firebase 설정 파일의 bundle ID도 앱과 다르다.

우선순위가 높은 문제는 다음 네 가지다.

1. 앱 target의 bundle ID는 `com.nexters.bandalart.iosApp`인데 `GoogleService-Info.plist`의 `BUNDLE_ID`는 `com.nexters.bandalart`다.
2. `FirebaseAnalytics`를 Swift Package Manager로 사용하지만 target의 `OTHER_LDFLAGS`에 `-ObjC`가 없다.
3. Crashlytics dSYM 업로드 Run Script와 Xcode 15 이상에서 요구되는 Input Files가 없다.
4. Release는 dSYM을 생성하지만 Debug는 `DWARF`만 사용한다. Firebase 공식 절차는 모든 build type에 `DWARF with dSYM File`을 설정하도록 안내한다.

따라서 현재 Firebase 콘솔에서 iOS Analytics와 Crashlytics가 보이지 않는 것은 충분히 설명된다. 단, 어느 한 항목만이 단독 원인이라고 단정하지 않고 아래 순서로 설정을 바로잡은 뒤 각 제품을 독립적으로 검증해야 한다.

## 구현 결과 (2026-08-07)

- Firebase CLI 계정을 프로젝트 권한이 있는 `mraz3068@gmail.com`으로 전환했다.
- Firebase 프로젝트 `bandalart-e0288`에 실제 App Store bundle ID `com.nexters.bandalart.iosApp`과 App Store ID `6743101965`를 사용하는 Apple app을 등록했다.
- 새 Apple app에서 받은 `GoogleService-Info.plist`로 교체했다.
- Debug·Release target의 `OTHER_LDFLAGS`에 `$(inherited)`와 `-ObjC`를 추가했다.
- Debug도 `dwarf-with-dsym`을 사용하도록 변경했다.
- 공식 SPM Crashlytics script와 Xcode 15+ Input Files 다섯 개를 마지막 build phase로 추가했다.
- Firebase 초기화를 SwiftUI `App.init`에서 `UIApplicationDelegateAdaptor` 기반 application delegate로 옮겼다.

코드·프로젝트 설정 복구는 완료했지만 Analytics DebugView와 Crashlytics test crash 수신은 실제 실행이 필요하므로 완료 조건으로 남긴다. 기존 `com.nexters.bandalart` Firebase Apple app은 데이터가 없음을 확인한 뒤 30일 복구 가능한 삭제 상태로 전환했다.

## 현재 프로젝트 점검

| 항목 | 현재 상태 | 판단 |
| --- | --- | --- |
| SPM 제품 | `FirebaseAnalytics`, `FirebaseCrashlytics`가 target에 연결됨 | 설치 자체는 되어 있음 |
| SDK 버전 | `11.8.1`, `Up to Next Major Version` | 우선 연동을 복구하고 major 업데이트는 별도 작업으로 분리 |
| 초기화 | SwiftUI `App.init`에서 `FirebaseApp.configure()` 호출 | 호출은 존재하지만 최신 공식 SwiftUI 구성과 다름 |
| Firebase bundle ID | 설정 파일 `com.nexters.bandalart` | 실제 target `com.nexters.bandalart.iosApp`과 불일치 |
| Analytics linker flag | `OTHER_LDFLAGS` 설정 없음 | `-ObjC` 누락 |
| Crashlytics Run Script | KMP framework embed script만 존재 | dSYM 자동 업로드 없음 |
| dSYM | Debug `dwarf`, Release `dwarf-with-dsym` | Debug도 dSYM으로 맞출 필요 있음 |
| User Script Sandboxing | Debug·Release 모두 `NO` | 현재는 sandbox 접근 제한이 없지만 공식 Input Files는 그대로 추가하는 편이 안전함 |
| Analytics 수집 비활성화 키 | 앱 `Info.plist`에 `FIREBASE_ANALYTICS_COLLECTION_ENABLED=NO` 없음 | 앱이 명시적으로 수집을 끈 상태는 아님 |

근거가 되는 로컬 파일:

- `iosApp/iosApp.xcodeproj/project.pbxproj`
- `iosApp/iosApp/GoogleService-Info.plist`
- `iosApp/iosApp/iosApp.swift`

`GoogleService-Info.plist`의 `IS_ANALYTICS_ENABLED=false`는 현재 파일이 Firebase 프로젝트의 최신 Analytics 설정과 맞지 않을 가능성을 보여 주지만, 이 값 하나만으로 런타임 수집이 비활성화되었다고 단정하지 않는다. Firebase가 공식적으로 안내하는 수집 비활성화 키는 앱 `Info.plist`의 `FIREBASE_ANALYTICS_COLLECTION_ENABLED`다. 가장 안전한 해결은 올바른 Firebase Apple app에서 설정 파일을 다시 내려받는 것이다.

## `-ObjC`가 필요한 이유와 적용 범위

Firebase의 Swift Package Manager 문서는 `FirebaseAnalytics`를 설치한 client가 `-ObjC` linker option을 추가해야 한다고 명시한다. Crashlytics 시작 가이드도 Analytics와 함께 사용할 때 동일한 설정을 요구한다.

`-ObjC`는 Objective-C class 또는 category를 구현한 static library member를 linker가 포함하도록 한다. Objective-C category method는 일반적인 undefined symbol을 만들지 않아서, 이 옵션이 없으면 필요한 구현이 최종 실행 파일에서 빠질 수 있다. Firebase의 SPM 배포는 기본적으로 static linking을 사용하므로 이 요구와 직접 관련된다.

적용값:

```text
OTHER_LDFLAGS = $(inherited) -ObjC
```

- 앱 target의 Debug와 Release 모두에 적용한다.
- `$(inherited)`를 보존한다.
- `-all_load`로 넓히지 않는다. Apple은 `-ObjC`도 실행 파일 크기를 늘릴 수 있다고 설명하며, Firebase가 요구하는 범위는 `-ObjC`다.

출처:

- [Firebase Swift Package Manager guide](https://github.com/firebase/firebase-ios-sdk/blob/main/SwiftPackageManager.md)
- [Firebase Analytics for iOS 시작 가이드](https://firebase.google.com/docs/analytics/ios/get-started)
- [Apple QA1490: Building Objective-C static libraries with categories](https://developer.apple.com/library/archive/qa/qa1490/_index.html)
- [Firebase를 framework/library에서 사용할 때의 linking 설명](https://github.com/firebase/firebase-ios-sdk/blob/main/docs/firebase_in_libraries.md)

## 구현 체크리스트

### 1. Firebase Console과 설정 파일 정합성 복구

- [x] Firebase 프로젝트 설정에서 bundle ID `com.nexters.bandalart.iosApp`인 Apple app이 등록되어 있는지 확인한다.
- [ ] 다른 bundle ID로 등록되어 있다면 기존 항목을 억지로 재사용하지 말고 실제 bundle ID로 Apple app을 등록한다. Firebase의 Apple bundle ID는 대소문자를 구분하며 등록 후 변경할 수 없다.
- [ ] Google Analytics가 Firebase 프로젝트의 `Settings > Integrations`에서 활성화되어 있는지 확인한다.
- [x] 정확한 Apple app에서 새 `GoogleService-Info.plist`를 내려받아 기존 파일을 교체한다.
- [x] 새 파일의 `BUNDLE_ID`와 Xcode target의 `PRODUCT_BUNDLE_IDENTIFIER`가 모두 `com.nexters.bandalart.iosApp`인지 검증한다.
- [ ] 파일명이 정확히 `GoogleService-Info.plist`인지, target membership에 포함되어 빌드된 `.app` 안에도 존재하는지 확인한다.
- [x] Firebase Console Apple app에 App Store ID `6743101965`를 등록한다.

Firebase는 여러 bundle ID가 있으면 각각을 별도 Firebase app으로 등록하고 각각의 설정 파일을 사용하라고 안내한다.

출처: [Add Firebase to your Apple project](https://firebase.google.com/docs/ios/setup)

### 2. 초기화와 Analytics linker 설정

- [x] 앱 target의 `Other Linker Flags`에 Debug·Release 공통으로 `$(inherited) -ObjC`를 추가한다.
- [x] `FirebaseApp.configure()`는 한 번만 호출한다.
- [x] 최신 Firebase SwiftUI 가이드에 맞게 `UIApplicationDelegate`의 `application(_:didFinishLaunchingWithOptions:)`에서 초기화하고 `@UIApplicationDelegateAdaptor`로 SwiftUI `App`에 연결한다.
- [ ] 초기화 직후 `FirebaseApp.app()`이 존재하는지 Debug에서 assertion 또는 로그로 확인하되, 구성 파일의 식별자 값은 로그로 출력하지 않는다.

현재 `App.init` 호출이 반드시 실패한다고 단정할 근거는 없다. 다만 Firebase의 현재 SwiftUI 공식 문서는 application delegate를 연결하는 형태를 요구하므로, 재연동 시 공식 lifecycle로 맞추는 편이 유지보수와 후속 Firebase 제품 연동에 안전하다.

출처:

- [Add Firebase to your Apple project](https://firebase.google.com/docs/ios/setup)
- [Firebase Analytics for iOS 시작 가이드](https://firebase.google.com/docs/analytics/ios/get-started)

### 3. Crashlytics dSYM 자동 업로드

- [x] Debug와 Release의 `DEBUG_INFORMATION_FORMAT`을 `dwarf-with-dsym`으로 설정한다.
- [x] 다음 SPM용 script를 새로운 Run Script build phase에 추가한다.

```sh
"${BUILD_DIR%/Build/*}/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
```

- [x] 이 phase를 target의 **마지막 build phase**로 둔다. KMP의 `embedAndSignAppleFrameworkForXcode` phase와 합치지 않는다.
- [x] Xcode 15 이상 기준으로 아래 Input Files를 모두 추가한다.

```text
${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}
${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${PRODUCT_NAME}
${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Info.plist
$(TARGET_BUILD_DIR)/$(UNLOCALIZED_RESOURCES_FOLDER_PATH)/GoogleService-Info.plist
$(TARGET_BUILD_DIR)/$(EXECUTABLE_PATH)
```

- [ ] 향후 `ENABLE_USER_SCRIPT_SANDBOXING=YES`와 `ENABLE_DEBUG_DYLIB=YES`를 동시에 사용하면 다음 항목도 추가한다.

```text
${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${PRODUCT_NAME}.debug.dylib
```

- [ ] archive 직후 `.xcarchive/dSYMs`에 앱 dSYM과 `ComposeApp.framework.dSYM`이 존재하는지 확인하고 각각 `dwarfdump --uuid`로 UUID를 기록한다.
- [ ] Firebase Console에 Missing dSYM 경고가 나타나면 해당 archive의 dSYM을 `upload-symbols`로 수동 업로드해 자동 script 문제와 symbol 생성 문제를 분리해 진단한다.

dSYM은 앱 크기 절감을 위해 제거할 대상이 아니다. App Store 사용자 다운로드에 포함되는 디버그 데이터와 Crashlytics symbolication용 archive dSYM은 구분해야 한다.

출처:

- [Get started with Crashlytics for Apple platforms](https://firebase.google.com/docs/crashlytics/ios/get-started)
- [Get readable crash reports](https://firebase.google.com/docs/crashlytics/ios/get-deobfuscated-reports)
- [Firebase Swift Package Manager guide](https://github.com/firebase/firebase-ios-sdk/blob/main/SwiftPackageManager.md)

### 4. Analytics 검증

- [ ] Xcode scheme의 Debug `Arguments Passed On Launch`에 `-FIRDebugEnabled`를 일시 추가한다.
- [ ] 앱을 실행해 `session_start` 같은 자동 event와 테스트용 custom event 하나를 발생시킨다. `first_open`은 새 설치에서만 기대하고, Compose 화면은 단일 host view controller 때문에 화면별 `screen_view`가 자동으로 구분되지 않을 수 있다.
- [ ] Firebase Console의 Analytics > DebugView에서 해당 iOS device와 event가 수 분 내 보이는지 확인한다.
- [ ] Xcode console에서 거부된 event/parameter 또는 잘못된 구성 경고가 없는지 확인한다.
- [ ] 확인 후 `-FIRDebugDisabled`로 한 번 실행해 해당 개발 device의 지속되는 debug mode를 명시적으로 해제하고, 그 다음 launch argument를 제거한다.
- [ ] DebugView의 개발 traffic을 운영 지표로 오해하지 않도록 필요하면 developer traffic filter를 설정한다.

출처: [Firebase Analytics DebugView](https://firebase.google.com/docs/analytics/debugview)

### 5. Crashlytics 검증

- [ ] 일반 사용자 화면에 노출되지 않는 Debug 전용 test crash trigger를 만든다.
- [ ] `fatalError("Crash was triggered")`를 사용해 Swift host crash를 먼저 검증한다.
- [ ] Xcode에서 앱을 설치·실행한 뒤 scheme을 Stop하여 debugger를 분리한다.
- [ ] 기기 또는 simulator 홈 화면에서 앱을 직접 실행하고 test crash를 발생시킨다.
- [ ] 앱을 다시 실행해 저장된 report가 업로드되도록 한다.
- [ ] Firebase Console Crashlytics에서 5분 안에 issue가 보이고 stack trace가 symbolicate되는지 확인한다.
- [ ] 보이지 않으면 `-FIRDebugEnabled`를 추가하고 재현한 뒤 console에서 `Completed report submission`을 확인한다.
- [ ] 검증 후 test crash trigger와 debug flag를 제거한다.
- [ ] 별도로 common Kotlin 코드에서 의도적인 unhandled exception을 발생시켜 Compose/Kotlin stack의 가독성을 확인한다.

출처:

- [Test your Crashlytics implementation](https://firebase.google.com/docs/crashlytics/ios/test-implementation)
- [Get started with Crashlytics for Apple platforms](https://firebase.google.com/docs/crashlytics/ios/get-started)

## KMP·Compose에서 추가로 확인할 점

- Firebase Apple SDK는 Swift host app에 연결되므로 `FirebaseApp.configure()`가 성공하면 같은 process에서 실행되는 Compose UI에도 기본 Analytics session과 native crash 수집이 적용된다.
- 그러나 현재 common Kotlin 코드에는 Analytics event 또는 non-fatal error를 Firebase로 전달하는 API가 없다. 화면·도메인 이벤트를 수집하려면 이후 `expect/actual` 또는 platform interface를 추가해야 한다. 이번 연동 복구에서는 자동 event와 native crash부터 검증한다.
- Kotlin/Native release binary는 Apple platform용 dSYM을 기본 생성한다. archive 안의 `ComposeApp.framework.dSYM`이 보존·업로드되어야 Kotlin 주소를 symbolicate할 수 있다.
- dSYM이 있어도 iOS 경계까지 전파된 uncaught Kotlin exception은 원래 Kotlin stack이 유실되어 Crashlytics에서 모호하게 보일 수 있다. 이 문제는 Firebase 초기 설정 실패와 별개다. 실제 test crash 결과가 불충분할 때만 JetBrains가 안내하는 `NSExceptionKt` 계열 integration을 별도 이슈로 검토한다.
- Firebase를 Swift host와 Kotlin framework 양쪽에 중복 link하지 않는다. 현재처럼 Firebase Apple SDK ownership은 iOS host target 한 곳에 두는 것이 안전하다.

출처:

- [Kotlin/Native debugging and dSYM](https://kotlinlang.org/docs/native-debugging.html)
- [Kotlin/Native FAQ: iOS crash reports](https://kotlinlang.org/docs/native-faq.html)
- [Firebase SDKs in frameworks and libraries](https://github.com/firebase/firebase-ios-sdk/blob/main/docs/firebase_in_libraries.md)

## 완료 조건

- [ ] 빌드된 앱 bundle ID와 `GoogleService-Info.plist`의 bundle ID가 일치한다.
- [ ] Debug·Release에 `$(inherited) -ObjC`가 적용된다.
- [ ] Crashlytics Run Script가 마지막 build phase에 있고 Xcode 15+ Input Files가 설정된다.
- [ ] 앱 및 Compose framework dSYM의 존재와 UUID를 확인한다.
- [ ] Analytics DebugView에서 iOS event를 확인한다.
- [ ] debugger가 분리된 test crash가 Crashlytics에 수집되고 symbolicate된다.
- [ ] test-only crash trigger와 Firebase debug launch argument가 배포 build에 남지 않는다.
- [ ] App Store Connect의 개인정보 공개와 `PrivacyInfo.xcprivacy`가 실제 Analytics·Crashlytics 수집 항목에 맞게 갱신된다.

## 구현 순서 권장안

1. Firebase Console에서 올바른 Apple app과 Analytics integration을 확인하고 설정 파일을 교체한다.
2. `-ObjC`, 공식 SwiftUI 초기화 lifecycle, dSYM 설정과 Crashlytics Run Script를 한 PR에 적용한다.
3. DebugView와 Swift test crash로 연동을 확인한다.
4. TestFlight archive에서 dSYM과 실제 crash symbolication을 확인한다.
5. 그 다음에만 Firebase SDK 11.x → 최신 major 업그레이드나 common Kotlin event abstraction을 별도 작업으로 진행한다.
