# 2.2.9 다크 테마 대비 회귀 핫픽스 전략

## 1. 배경

2.2.8 Internal Testing에서 Home 전체 다크 테마와 기존 Android 데이터 복구는 정상임을 확인했다. 다만 다크 테마에서 다섯 개의 세부 요소가 주변 화면과 일관되지 않거나 충분히 구분되지 않는다.

- 더보기 메뉴의 저장 아이콘
- 표 작성 modal bottom sheet의 입력 텍스트
- 달성 여부 Switch의 미선택 thumb
- 완료된 task cell과 6칸 task 영역의 바깥 배경
- 설정 modal bottom sheet의 기본 시스템 폰트

이번 핫픽스는 테마 팔레트나 화면 구조를 다시 설계하지 않는다. 라이트 모드 전용 색상 또는 잘못 연결된 semantic color만 바로잡는다.

## 2. 확인된 원인

| 증상 | 원인 |
|---|---|
| 저장 아이콘이 어두움 | `ic_gallery`가 `#1F2937` stroke/fill을 포함하고, `Icon`이 `Color.Unspecified`로 원본 색을 그대로 표시한다. |
| 입력 문자가 검정색 | `BasicTextField`의 `BottomSheetContent()`가 `Gray900`을 고정 사용한다. |
| 미선택 Switch thumb가 어두움 | thumb에 dark `surface`, track에 dark `surfaceVariant`를 사용해 두 색의 대비가 작다. |
| 완료 task cell이 사라져 보임 | 완료 cell과 6칸 task 영역 wrapper가 모두 `outline`을 사용한다. |
| 설정 화면 텍스트가 기존 화면과 이질적임 | 설정 화면의 모든 `Text`에 font family가 없고 전역 Material typography도 `FontFamily.Default`를 사용한다. |

## 3. 수정 전략

- 저장 아이콘 tint → `onSurface`
- bottom sheet 입력 text와 cursor → `onSurface`
- 미선택 Switch thumb → `onSurfaceVariant`
- 완료 task cell → 기존 `outline` 유지
- 6칸 task 영역 wrapper → `outlineVariant`
- 설정 화면의 제목·섹션·테마 항목·버전 텍스트 → `pretendardFontFamily()`

사용자 선택 main/sub 색상, 완료 check vector 원색, 삭제 아이콘의 error 색상은 변경하지 않는다.

## 4. 검증 전략

현재 프로젝트에는 Compose screenshot test 기반이 없고 semantics tree도 실제 paint color를 노출하지 않는다. 얕은 source-string 테스트나 hotfix를 위한 신규 screenshot framework 도입은 하지 않는다.

자동 검증은 다음 범위로 한정한다.

- Android/iOS에서 변경된 common Compose 코드 컴파일
- Home의 기존 presenter 테스트
- 변경 모듈의 정적 분석과 코드 스타일 검사

최종 시각 판정은 2.2.9 Internal Testing에서 다음 항목을 라이트/다크 모드로 각각 확인한다.

1. 저장 아이콘의 선과 면이 메뉴 배경에서 선명하게 보인다.
2. modal bottom sheet의 입력 문자와 cursor가 보인다.
3. 달성 여부를 끈 상태에서도 thumb와 track 경계가 보인다.
4. 완료 task cell이 6칸 wrapper와 구분되고 완료 check도 유지된다.
5. 라이트 모드에서 기존 의미와 조작 상태가 유지된다.
6. 설정 화면의 제목·섹션·테마 항목·버전 정보가 기존 화면과 같은 Pretendard로 표시된다.

## 5. 완료 조건

- 다섯 요소가 Material color scheme의 의미색 또는 기존 제품 font family를 사용한다.
- 새 hard-coded color를 추가하지 않는다.
- Android/iOS common UI 컴파일과 기존 Home 테스트가 통과한다.
- Internal Testing에서 다섯 시각 회귀 항목을 확인할 수 있도록 앱 버전을 2.2.9(20209)로 준비한다.

## 6. 비범위

- 디자인 시스템 팔레트 전면 조정
- 사용자 지정 chart 색상 변경
- 신규 screenshot test 도구 도입
- Home 이외 화면의 디자인 변경
- production 또는 iOS 배포

## 7. 구현 후 자동 검증 결과

- Home Android host test: 통과
- Home detekt: 통과
- Android 앱 debug Kotlin compile: 통과
- iOS Simulator Arm64 KMP compile: 통과
- 변경된 Kotlin 파일의 Spotless 포맷: 통과

모듈 전체 `spotlessKotlinCheck`는 기준 브랜치부터 존재하는 `CellText.kt` 포맷 위반 한 건 때문에 실패한다. 이번 변경 파일에서는 위반이 검출되지 않으며, PR CI에서는 `origin/main` ratchet 기준으로 변경분을 다시 확인한다.
