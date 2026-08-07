# iOS Release 크기 기준선

## 기준

- 측정일: 2026-08-07
- 기준 브랜치: `main`
- 기준 커밋: `ee11eae9646d8a31c466442bba608c992f2a1204`
- 대상: `iosApp` Release

App Store에 표시되는 75.1MB, `.xcarchive` 전체 크기, `.ipa`, App Thinning의 기기별 다운로드·설치 크기는 서로 다른 지표다. 이 문서는 Archive 생성 전에 저장소에서 확인할 수 있는 Release 설정과 정적 리소스 기준선을 기록한다.

## Release 설정

`xcodebuild -showBuildSettings`에서 확인한 주요 effective setting은 다음과 같다.

| 설정 | 값 |
| --- | --- |
| `COPY_PHASE_STRIP` | `NO` |
| `DEAD_CODE_STRIPPING` | `YES` |
| `DEBUG_INFORMATION_FORMAT` | `dwarf-with-dsym` |
| `ENABLE_TESTABILITY` | `NO` |
| `MACH_O_TYPE` | `mh_execute` |
| `ONLY_ACTIVE_ARCH` | `NO` |
| `OTHER_LDFLAGS` | `-ObjC` |
| `STRIP_INSTALLED_PRODUCT` | `YES` |
| `STRIP_STYLE` | `all` |
| `STRIP_SWIFT_SYMBOLS` | `YES` |
| `SWIFT_COMPILATION_MODE` | `wholemodule` |

현재 값만 보면 Release용 dead-code stripping과 설치 제품 strip이 이미 활성화되어 있다. `COPY_PHASE_STRIP = NO`만 보고 최종 앱 바이너리가 strip되지 않는다고 판단하면 안 된다. dSYM은 Crashlytics symbolication에 필요하므로 비활성화하지 않는다.

## 정적 리소스

### Compose resources

| 구분 | 파일 수 | 원본 합계 |
| --- | ---: | ---: |
| 전체 | 342 | 17,291,942B |
| 폰트 | 11 | 14,468,072B |
| drawable | 321 | 1,640,264B |
| files | 7 | 1,152,081B |
| 다국어 values | 3 | 31,525B |

폰트는 Compose resources 원본 합계의 약 83.7%를 차지한다. Fluent Emoji 300개 WebP의 원본 합계는 935,622B이므로 현재 리소스 크기의 주원인은 이모지보다 폰트다.

미사용 폰트 제거 후 Compose resources는 9,511,982B, 폰트는 6,688,112B이다. 변경 전 기준선 대비 원본 리소스가 약 45.0% 감소했다.

### Pretendard 사용 굵기

UI 코드에서 실제로 지정하는 Pretendard 굵기는 `W400`, `W500`, `W600`, `W700`이다.

| 분류 | 파일 | 원본 합계 |
| --- | --- | ---: |
| 사용 | Regular, Medium, SemiBold, Bold | 6,333,976B |
| 미사용 후보 | Thin, ExtraLight, Light, ExtraBold, Black | 7,779,960B |

미사용 후보 다섯 굵기는 `FontFamily` 등록 외에 UI 코드에서 참조되지 않았다. 이번 변경에서 해당 등록과 원본 파일을 제거해 소스 리소스 기준 7,779,960B를 줄였다. 실제 App Store 다운로드 감소량은 압축과 App Thinning을 거친 동일 기기 variant로 확인해야 한다.

두 브랜드 폰트 `Neurimbo Gothic Regular`, `Krona One Regular`는 앱 타이틀에서 사용되므로 제거 대상이 아니다. 폰트 파일 간 동일 checksum 중복도 없다.

## 현재 판단

- 새로운 linker/strip 옵션을 추측으로 추가할 근거는 없다.
- 첫 최적화로 사용하지 않는 Pretendard 다섯 굵기를 제거한다.
- Fluent Emoji 전체는 약 0.94MB라 우선순위가 낮고, 이모지 기능을 축소하면서 제거할 수준은 아니다.
- Firebase Analytics와 Crashlytics는 제품 요구사항이며 제거 대상이 아니다.
- 현재 Mac에 비교 가능한 BandalArt `.xcarchive`가 없어 사용자 다운로드 크기 기준선은 아직 없다.

## Archive 후 기록할 값

이 변경이 병합된 최신 `main`에서 Release Archive를 만든 다음 아래를 기록한다.

1. `.xcarchive/Products/Applications/iosApp.app` 크기
2. 앱 executable과 embedded framework별 크기·architecture
3. 앱 및 Compose framework dSYM 존재 여부와 UUID
4. App Thinning Size Report의 동일 iPhone variant 다운로드·설치 크기
5. App Store 기존 공개 버전과 새 빌드의 기기별 지표 차이

Archive 경로가 준비되면 저장소 변경 없이 산출물부터 분석한다. 제거 전후를 같은 빌드 조건으로 엄밀하게 비교해야 한다면 기준 커밋 `ee11eae`와 병합 후 커밋을 각각 Archive한다. 일반 배포 검증에서는 병합 후 App Thinning 지표와 App Store 기존 공개 버전을 대조하고 기능 회귀를 확인한다.
