# BandalArt 문서 안내

이 디렉터리의 문서는 목적에 따라 아래처럼 구분한다.

- `GUIDE`: 반복 적용하는 현재 프로젝트 규칙. 구현과 함께 갱신한다.
- `STRATEGY`: 특정 이슈·변경의 범위, 결정, 검증 기록.
- `RESEARCH`, `SPIKE`, `BASELINE`: 전략의 근거가 된 조사와 측정 결과.

새 문서는 관련 주제 폴더에 둔다. 루트에는 이 인덱스와 자주 직접 여는 [백로그 실행 전략](BACKLOG_EXECUTION_STRATEGY.md)만 둔다.
`STRATEGY`가 목록에 있다는 사실만으로 현재 진행 중임을 뜻하지 않는다. 현재 우선순위와 완료 상태는 백로그를 기준으로 보고, 반복 적용할 규칙은 아래의 현재 기준 가이드를 따른다.

## 현재 기준 가이드

- [Compose와 Circuit 상태 수명 가이드](architecture/state/COMPOSE_STATE_LIFETIME_GUIDE.md)
- [Coordinator 패턴과 현재 적용 위치](architecture/coordinator/COORDINATOR_PATTERN_GUIDE.md)
- [Compose Multiplatform 마이그레이션 문제 해결](architecture/kmp/COMPOSE_MULTIPLATFORM_MIGRATION_TROUBLESHOOTING.md)
- [KMP 테스트 소스셋과 Circuit Presenter 테스트 가이드](architecture/kmp/KMP_TESTING_GUIDE.md)

## Architecture

### Coordinator

- [Coordinator 패턴과 현재 적용 위치](architecture/coordinator/COORDINATOR_PATTERN_GUIDE.md)

### Circuit

- [Circuit 전체 마이그레이션 전략](architecture/circuit/CIRCUIT_COMPLETE_MIGRATION_STRATEGY.md)
- [Circuit Home 편집 마이그레이션](architecture/circuit/CIRCUIT_HOME_EDIT_MIGRATION_STRATEGY.md)
- [Circuit Home 읽기 마이그레이션](architecture/circuit/CIRCUIT_HOME_READ_MIGRATION_STRATEGY.md)
- [Circuit Home 런타임 마이그레이션](architecture/circuit/CIRCUIT_HOME_RUNTIME_MIGRATION_STRATEGY.md)
- [Circuit Splash·Onboarding 전략](architecture/circuit/CIRCUIT_SPLASH_ONBOARDING_STRATEGY.md)

### State

- [Compose와 Circuit 상태 수명 가이드](architecture/state/COMPOSE_STATE_LIFETIME_GUIDE.md)
- [Circuit 상태 보존과 DatePicker 전략](architecture/state/CIRCUIT_STATE_PRESERVATION_STRATEGY.md)

### KMP

- [AGP 9 KMP 마이그레이션](architecture/kmp/KMP_AGP_9_MIGRATION_STRATEGY.md)
- [Compose Multiplatform 마이그레이션 문제 해결](architecture/kmp/COMPOSE_MULTIPLATFORM_MIGRATION_TROUBLESHOOTING.md)
- [KMP 통합 검증 전략](architecture/kmp/KMP_INTEGRATION_VALIDATION_STRATEGY.md)
- [KMP 정적 분석 지원](architecture/kmp/KMP_STATIC_ANALYSIS_SUPPORT.md)
- [KMP 테스트 가이드](architecture/kmp/KMP_TESTING_GUIDE.md)

### Metro

- [Circuit·Metro KMP 통합 전략](architecture/metro/CIRCUIT_METRO_KMP_INTEGRATION_STRATEGY.md)
- [Circuit·Metro KMP 이식 맵](architecture/metro/CIRCUIT_METRO_KMP_MIGRATION_MAP.md)
- [KMP·Metro 트러블슈팅](architecture/metro/KMP_METRO_MIGRATION_TROUBLESHOOTING.md)
- [Metro 부트스트랩](architecture/metro/METRO_BOOTSTRAP_STRATEGY.md)
- [Metro composition root 정리](architecture/metro/METRO_COMPOSITION_ROOT_CLEANUP_STRATEGY.md)
- [Metro platform data graph](architecture/metro/METRO_PLATFORM_DATA_GRAPH_STRATEGY.md)
- [Metro repository graph](architecture/metro/METRO_REPOSITORY_GRAPH_STRATEGY.md)

## Features

### Ads

- [Android AdMob 미노출 트러블슈팅](features/ads/ADMOB_ANDROID_TROUBLESHOOTING.md)
- [AdMob 홈 하단 배너](features/ads/ADMOB_HOME_BANNER_STRATEGY.md)
- [AdMob 보상형 생성 gate](features/ads/ADMOB_REWARDED_CREATE_GATE_STRATEGY.md)
- [AdMob 슬롯 기반](features/ads/ADMOB_SLOT_FOUNDATION_STRATEGY.md)

### Emoji

- [이모지 picker 하단 inset 수정](features/emoji/EMOJI_PICKER_BOTTOM_INSETS_FIX_STRATEGY.md)
- [이모지 picker legacy UI 전략](features/emoji/EMOJI_PICKER_LEGACY_UI_STRATEGY.md)
- [Fluent Emoji 카테고리 탐색](features/emoji/FLUENT_EMOJI_CATEGORY_NAVIGATION_STRATEGY.md)
- [Fluent Emoji picker 마이그레이션](features/emoji/FLUENT_EMOJI_PICKER_MIGRATION_STRATEGY.md)
- [Fluent Emoji resource spike](features/emoji/FLUENT_EMOJI_RESOURCE_SPIKE.md)

### Home, Settings, Updates

- [태스크 셀 햅틱 완료](features/home/TASK_CELL_HAPTIC_COMPLETION_STRATEGY.md)
- [반다라트 템플릿 catalog v1](features/templates/BANDALART_TEMPLATE_CATALOG_V1_STRATEGY.md)
- [마감일 기반 로컬 알림 조사](features/notifications/LOCAL_DEADLINE_NOTIFICATION_RESEARCH.md)
- [마감일 기반 로컬 알림 구현 전략](features/notifications/LOCAL_DEADLINE_NOTIFICATION_STRATEGY.md)
- [Android 마감 알림 시작 복구](features/notifications/ANDROID_DEADLINE_REMINDER_STARTUP_RECOVERY.md)
- [이메일 문의](features/settings/EMAIL_INQUIRY_STRATEGY.md)
- [설정 bottom sheet typography](features/settings/SETTINGS_BOTTOM_SHEET_TYPOGRAPHY_STRATEGY.md)
- [설정 theme](features/settings/SETTINGS_THEME_STRATEGY.md)
- [foreground 인앱 업데이트](features/updates/IN_APP_UPDATE_FOREGROUND_STRATEGY.md)

### Widgets

- [Android 위젯 기능 가이드](features/widgets/ANDROID_WIDGET_FEATURE_GUIDE.md)
- [Android 위젯 표시 대상 선택 가이드](features/widgets/ANDROID_WIDGET_SELECTION_GUIDE.md)
- [Android 위젯 동기화 트러블슈팅](features/widgets/ANDROID_WIDGET_SYNC_TROUBLESHOOTING.md)
- [Android와 iOS 위젯 동기화 모델 비교](features/widgets/ANDROID_IOS_WIDGET_SYNC_RESEARCH.md)
- [Android Glance 위젯 MVP](features/widgets/ANDROID_WIDGET_MVP_STRATEGY.md)
- [iOS WidgetKit 위젯 MVP](features/widgets/IOS_WIDGETKIT_MVP_STRATEGY.md)

## Releases

### Android

- [2.2.12 resource dedup](releases/android/ANDROID_2_2_12_RESOURCE_DEDUP_RELEASE_STRATEGY.md)
- [2.2.13 clean release 복구](releases/android/ANDROID_2_2_13_CLEAN_RELEASE_RECOVERY_STRATEGY.md)
- [2.2.14 Fluent Emoji 출시](releases/android/ANDROID_2_2_14_FLUENT_EMOJI_RELEASE_STRATEGY.md)
- [2.2.15 Internal Testing](releases/android/ANDROID_INTERNAL_2_2_15_STRATEGY.md)
- [2.2.8 storage/theme hotfix](releases/android/HOTFIX_2_2_8_STORAGE_THEME_STRATEGY.md)
- [2.2.9 dark theme hotfix](releases/android/HOTFIX_2_2_9_DARK_THEME_CONTRAST_STRATEGY.md)
- [main Android Internal release](releases/android/MAIN_ANDROID_INTERNAL_RELEASE_STRATEGY.md)
- [2.2.10 Internal release](releases/android/VERSION_2_2_10_INTERNAL_RELEASE_STRATEGY.md)

### iOS

- [Firebase iOS 통합 조사](releases/ios/FIREBASE_IOS_INTEGRATION_RESEARCH.md)
- [iOS 1.2.0 App Store 출시 자료](releases/ios/IOS_1_2_0_APP_STORE_RELEASE.md)
- [iOS release size baseline](releases/ios/IOS_RELEASE_SIZE_BASELINE.md)
- [iOS release size 최적화 조사](releases/ios/IOS_RELEASE_SIZE_OPTIMIZATION_RESEARCH.md)
- [iOS release size 최적화 전략](releases/ios/IOS_RELEASE_SIZE_OPTIMIZATION_STRATEGY.md)
- [iOS TestFlight 업데이트 검증](releases/ios/IOS_TESTFLIGHT_UPDATE_STRATEGY.md)

### Automation

- [Fastlane CD 파일 가이드](releases/automation/FASTLANE_CD_FILE_GUIDE.md)
- [Fastlane Android/iOS CD 복구](releases/automation/FASTLANE_CD_RECOVERY_STRATEGY.md)

## Project

- [문서 정보 구조 정리 전략](project/DOCS_INFORMATION_ARCHITECTURE_STRATEGY.md)

## Legal

- [Android·iOS 광고 반영 개인정보처리방침 초안](legal/PRIVACY_POLICY_ANDROID_AD_UPDATE.md)
- [한·영·일 개인정보처리방침 개정 초안](legal/PRIVACY_POLICY_DRAFT.md)
