# 설정 이메일 문의 연동 전략

## 목표

설정 바텀시트의 앱 정보 영역에서 사용자가 `mraz3068@gmail.com`으로 문의 메일을 작성할 수 있게 한다. 공통 UI와 Circuit 상태는 플랫폼 API를 모르고, Android와 iOS는 동일한 mail draft를 각 플랫폼의 외부 메일 앱으로 전달한다.

## 성공 기준

- 설정의 앱 정보 영역에 48dp 이상의 클릭 가능한 `문의하기` 행이 표시된다.
- Android와 iOS에서 수신자, 현지화된 제목·본문, 앱 버전과 플랫폼 정보가 포함된 메일 작성 화면을 연다.
- 메일 앱을 열 수 없거나 실행에 실패하면 이메일 주소를 clipboard에 복사하고 snackbar로 알린다.
- 빠른 중복 탭이나 recomposition으로 같은 외부 화면을 중복 실행하지 않는다.
- 반다라트 제목, 목표, cell 내용과 기기 식별자는 draft에 포함하지 않는다.

## 설계

### 공통 모델과 플랫폼 경계

`core:common`에 다음 경계를 둔다.

- `SupportMailDraft`: recipient, subject, body
- `SupportMailOpenResult`: Opened, Unavailable, Failed
- `SupportMailLauncher`: platformName, open, copyToClipboard
- RFC 3986 기반 `mailto:` 문자열 생성
- 실패 시 recipient를 복사하는 공통 `openWithClipboardFallback`

URI 생성과 fallback 결정은 순수 공통 코드로 테스트한다. Android와 iOS 구현은 생성된 동일 URI를 사용하므로 제목·본문 encoding 규칙이 플랫폼마다 달라지지 않는다.

### Android

- `Intent.ACTION_SENDTO`와 `mailto:` URI만 사용한다.
- manifest `queries`에 mailto handler 조회를 선언한다.
- handler가 없으면 `Unavailable`, 시작 중 예외는 `Failed`로 반환한다.
- application context에서 실행하므로 `FLAG_ACTIVITY_NEW_TASK`를 사용한다.
- fallback은 `ClipboardManager`로 이메일 주소만 복사한다.

### iOS

- `UIApplication.canOpenURL` 확인 후 `mailto:` URL을 연다.
- 열 수 없으면 `Unavailable`, URL 생성 또는 실행 실패는 `Failed`로 반환한다.
- fallback은 `UIPasteboard`에 이메일 주소만 기록한다.

두 구현은 `composeApp`의 기존 `PlatformBindings`를 통해 Metro `AppGraph`에 제공한다.

### Circuit one-shot 흐름

1. `SettingsBottomSheet`가 `ContactSupport` event를 보낸다.
2. `HomePresenter`가 `OpenSupportMail` effect를 한 번 노출한다.
3. `Home`의 `LaunchedEffect(state.effect)`가 현지화 resource와 앱 버전으로 draft를 만든다.
4. launcher가 외부 앱을 열거나 clipboard fallback을 수행한다.
5. UI가 effect를 consume한다.

같은 effect object가 consume되기 전에 연속으로 설정되어도 Compose state가 다시 발행되지 않으므로 빠른 중복 탭이 외부 화면을 여러 개 열지 않는다. 설정 bottom sheet는 닫지 않아 외부 앱에서 복귀했을 때 상태를 유지한다.

## 다국어

한국어, 영어, 일본어에 다음 resource를 추가한다.

- 문의하기 행
- 메일 제목
- 문의 안내와 앱 버전·운영체제 정보가 포함된 본문
- 메일 앱을 열 수 없어 주소를 복사했다는 fallback 안내

메일 아이콘과 chevron은 장식 요소로 두고 행 전체를 하나의 접근성 click target으로 제공한다. 기존 Pretendard와 `MaterialTheme.colorScheme`을 그대로 사용한다.

## 테스트

자동 검증:

- recipient, 공백, 줄바꿈, 비 ASCII 제목·본문의 mailto encoding
- Opened에서는 복사하지 않고 Unavailable/Failed에서는 recipient 복사
- `ContactSupport` event의 one-shot effect 발생과 consume
- Metro graph의 platform launcher 제공
- 관련 host test, Spotless와 Detekt

수동 검증:

- Android Gmail 등 mail handler에서 작성 화면과 draft 확인
- iOS Mail에서 작성 화면과 draft 확인
- mail handler가 없는 Android 환경의 clipboard/snackbar fallback
- 외부 앱 복귀 후 설정 bottom sheet와 테마 유지
- 한국어·영어·일본어 및 라이트·다크 모드 확인

## 비범위

- 앱 내부 메일 전송
- 첨부파일, 사용자 목표 데이터 또는 기기 식별자 자동 첨부
- 문의 내역 저장, FAQ, 카카오톡 채널
- Compose UI test/restoration harness 도입(#217)
