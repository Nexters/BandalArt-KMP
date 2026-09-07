# Maestro QA 결과: 2026-09-07

이 보고서는 `docs/maestro-ui-test-guide` 브랜치의 UI 자동화 결과를 기록한다. 테스트 작업 트리는 `25a90a2a5874`를 기준으로 완료 화면 접근성 수정과 Maestro flow 변경을 포함했다.

## Android 완료 화면

| 항목 | 결과 |
| --- | --- |
| 앱 | `com.nexters.bandalart.dev` 2.4.9 debug |
| 기기 | Samsung SM-S936N (`R3CY70P3F4H`) |
| flow | `.maestro/android/completion-screen.yaml` |
| HTML 실행 | 성공, 32초 |
| JUnit 반복 실행 | 성공, 36초 |

결과 파일:

- [상세 HTML 보고서](android-completion/report.html)
- [JUnit 보고서](android-completion/report.xml)

flow는 알림창 같은 시스템 오버레이를 닫고 앱을 시작한다. 달성률이 100%이면 `last task`를 미완료로 되돌린 뒤 앱을 재실행한다. 마지막 실행 목표와 상위 목표가 미완료로 집계된 `달성률 88%`를 확인하고 완료 화면의 제목, 저장, 공유와 뒤로가기를 검사한다.

## Android 알림 설정

| 항목 | 결과 |
| --- | --- |
| 기기 | Samsung SM-S936N (`R3CY70P3F4H`) |
| flow | `.maestro/android/notification-settings.yaml` |
| HTML 실행 | 성공, 43초 |
| 시스템 권한 | `POST_NOTIFICATIONS` 허용 확인 |

- [상세 HTML 보고서](android-notification-settings/report.html)

flow는 마감일 알림 활성화 확인창을 거쳐 설정을 켜고, 앱을 재실행해 활성 상태가 유지되는지 확인한 다음 원래 비활성 상태로 복원한다. 설정 화면의 테스트 알림 버튼은 `4fe34232`에서 의도적으로 숨겼으므로 공개 UI 검증 범위에 넣지 않았다.

알림 계획·예약·전송과 위젯 갱신 로직은 다음 Android host test와 함께 검증했다.

| Gradle task | 결과 |
| --- | --- |
| `:androidApp:testDebugUnitTest` | 성공 |
| `:composeApp:testAndroidHostTest` | 성공 |
| `:core:data:testAndroidHostTest` | 성공 |
| `:core:domain:testAndroidHostTest` | 성공 |
| `:feature:complete:testAndroidHostTest` | 성공 |
| `:feature:home:testAndroidHostTest` | 성공 |

총 53개 suite, 213개 test가 실행됐고 실패·오류·건너뜀은 모두 0개다. 이 중 Android 위젯 전용 test는 22개다.

## Android 위젯

| 항목 | 결과 |
| --- | --- |
| 기기 | Samsung SM-S936N (`R3CY70P3F4H`) |
| 런처 | Samsung One UI |
| 크기 | 2×2 |
| 시스템 등록 | App widget ID 15 등록 확인 |
| 마지막 조회 반영 | `완료 화면 테스트`, 100% 표시 성공 |
| cold-start 전환 | A↔B 10/10 성공 |
| active-session 전환 | A↔B 6/6 성공 |
| stale 재현 | 0/16 |

위젯 추가 직후에는 설정 과정에서 선택한 ID 1이 앱의 `recent_bandalart_id`에도 저장되어 `이름 없는 목표`, 0%가 표시됐다. 이는 이전 반다라트가 남은 것이 아니라 메인 목표가 비어 있는 ID 1의 정상 fallback 표시다.

마지막 조회 동기화는 ID 1과 ID 3을 교대로 선택해 검증했다. 각 회차에서 앱 목록의 선택 완료, DataStore protobuf의 `recent_bandalart_id`, 홈 이동 3초 후 위젯의 OCR 제목을 차례로 비교했다. ID 1은 `recent_bandalart_id=1`과 `이름 없는 목표`, ID 3은 `recent_bandalart_id=3`과 `완료 화면 테스트`가 일치했다.

- 앱을 매번 종료하고 다시 시작하는 cold-start 경로: 10회 모두 성공
- 앱 프로세스와 Glance composition을 유지하는 active-session 경로: 6회 모두 성공

현재 브랜치에는 갱신 요청을 직렬화하는 `c888d15b`와 활성 Glance 세션이 최근 선택 Flow를 구독하도록 한 `fe5e615c`가 모두 포함되어 있다. Samsung One UI 실기기에서 16회 동안 이전 대상이 유지되는 현상은 재현되지 않았으므로, 이 동기화 문제를 위한 추가 코드 패치나 긴급 앱 업데이트는 필요하지 않다고 판단한다.

Samsung 런처의 Glance 위젯은 UI hierarchy에 내부 텍스트를 노출하지 않아 Maestro text assertion을 사용할 수 없었다. 화면 OCR로 기대 제목을 판정했으며, 홈 화면의 다른 정보가 포함된 원본 스크린샷은 저장 범위에서 제외했다.

## iOS 완료 화면

| 항목 | 결과 |
| --- | --- |
| 앱 | `com.nexters.bandalart.iosApp` debug |
| 기기 | iPhone 17 Pro Simulator, iOS 26.2 |
| flow | `.maestro/ios/completion-screen.yaml` |
| HTML 실행 | 성공, 8초 |

- [상세 HTML 보고서](ios-completion/report.html)

Compose Multiplatform 셀의 iOS `longPressOn`은 일반 탭으로 처리되므로 사용자가 완료 화면에 한 번 수동 진입했다. 진입 후 완료 문구, 제목, 저장, 공유와 뒤로가기는 Maestro로 자동 검사했다.

## 실행 중 수정한 테스트 오류

- `달성률 96%` 기대값을 `88%`로 수정했다. 마지막 실행 목표와 상위 서브목표·메인목표가 함께 달성률에 반영된다.
- Samsung 알림창이 앱 위에 남는 환경을 처리하도록 `HOME` 키 이후 앱을 시작한다.
- 완료 화면 뒤로가기는 접근성 문구 `뒤로 가기`로 선택한다.

위 항목은 테스트 flow와 접근성 문구 수정이다. 기능 크래시, 데이터 손상 또는 완료 화면 렌더링 회귀는 발견하지 못했다.

## 저장 범위

PR에는 HTML, JUnit과 이 요약만 포함한다. 실기기 알림 내용이 포함될 수 있는 원본 스크린샷, 기기 로그와 전체 명령 artifact는 커밋하지 않는다.

## 남은 수동 검증

- Android 실기기에서 마감일이 있는 실제 목표의 오전 9시 알림 수신 확인
- iOS 실기기의 알림 예약·수신과 위젯 표시·갱신 확인
