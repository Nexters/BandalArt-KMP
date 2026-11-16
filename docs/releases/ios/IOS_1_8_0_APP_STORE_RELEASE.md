# iOS 1.8.0 App Store 출시 자료

- 관련 이슈: [#463](https://github.com/Nexters/BandalArt-KMP/issues/463), [#462](https://github.com/Nexters/BandalArt-KMP/issues/462), [#466](https://github.com/Nexters/BandalArt-KMP/issues/466)
- 관련 PR: #464 (연속 달성·위젯), #467 (주간 회고 알림), #468 (달성 기록 히트맵), #496 (넓은 화면 헤더 줄바꿈 수정)
- 이전 공개 버전: `1.7.1`
- 출시 버전: `1.8.0`
- 같은 기능의 Android 버전: `2.7.0 (20701)`
- 위젯 지원: iOS 17 이상

이 문서는 iOS 1.8.0의 한국어·영어·일본어 "이번 버전의 새로운 기능" 문구, App Review 메모와 심사 빌드 선택 기준을 정리한다. 앱 화면 이름은 `core/designsystem/src/commonMain/composeResources/values-en/strings.xml`과 `iosApp/BandalartWidget/en.lproj/Localizable.strings`의 영어 문구에 맞췄다.

App Store Connect의 "이 버전에서 업그레이드된 사항"은 `🔥` 같은 이모지를 유효하지 않은 문자로 거부한다. 출시 노트에는 이모지를 쓰지 않는다. App Review 메모에는 이모지를 써도 저장된다.

## 한국어 출시 노트

매일의 실천을 이어 가는 재미를 더했어요.

• 할 일을 완료한 날이 이어지면 홈과 위젯에 'N일 연속'이 표시돼요.
• 주간 회고: 이번 주 완료한 셀과 반다라트별 진행을 한눈에 볼 수 있어요. 매주 일요일 저녁 8시에 알림도 받을 수 있어요.
• 최근 16주 달성 기록을 잔디처럼 색으로 보여 줘요.
• 위젯에 남은 할 일 수와 연속 달성 일수가 함께 표시돼요.
• 화면을 다듬고 안정성을 개선했어요.

## English release notes

Keep your daily progress going.

• Complete tasks on consecutive days to see an "N-day streak" on Home and in widgets.
• Weekly review: see this week's completed cells and the progress of each Bandalart at a glance, with an optional reminder every Sunday at 8 PM.
• A heatmap shows your completions over the last 16 weeks.
• Widgets now show how many tasks are left and your current streak.
• Polished the interface and improved stability.

## 日本語リリースノート

毎日の積み重ねがもっと楽しくなりました。

• タスクを毎日完了すると、ホームとウィジェットに「N日連続」が表示されます。
• 週間ふりかえり：今週完了したセルとバンダラートごとの進捗をひと目で確認でき、毎週日曜日の夜8時に通知も受け取れます。
• 直近16週間の達成記録をヒートマップで表示します。
• ウィジェットに残りのタスク数と連続達成日数が表示されます。
• 画面を改善し、安定性を向上しました。

## App Review 메모

```text
No sign-in is required. All features in this version work on-device.

How to review the new features in 1.8.0:
1. On Home, tap the center cell to enter a main goal, then tap a sub goal cell and a task cell to add titles.
2. Complete a task: press and hold a task cell, or open it, set "Achievement Status" to "Achieved" and tap "Done". "🔥 1-day streak" appears next to "Completion Rate" in the Home header.
3. Tap "🔥 1-day streak", or open "Settings" (gear icon) and tap "View this week's review", to open the "Weekly review" sheet. It shows this week's completed cells, the progress of each Bandalart, and a "Last 16 weeks" completion heatmap.
4. In "Settings" > "Notifications", turn on "Weekly review reminder". The app asks for notification permission and schedules a local notification every Sunday at 8 PM. Tapping the notification opens the "Weekly review" sheet.
5. Add the Bandalart Home Screen widget (iOS 17 or later). It shows the last Bandalart viewed in the app with its completion rate, "N tasks left" and "🔥 N-day streak".

Streak and heatmap data are stored only on the device and are not sent to any server. Ads are shown with Google AdMob in the banner at the bottom of Home.
```

## 빌드 선택 기준

- App Store 심사에는 `ios_ads_mode = production`으로 업로드한 `1.8.0` build를 선택한다.
- `1.8.0 (1)`은 #496 수정 전 source(`4b5da4bb`)로 만든 build이므로 심사에 선택하지 않는다.
- #496 수정이 포함된 source `d997fa76`의 production build(Release CD run `43800176522`)를 선택한다. build 번호는 업로드 검증 로그 `TestFlight upload verified: 1.8.0 (<build>)`에서 확인한다.
- 테스트 광고 build는 심사 build로 선택하지 않는다.

## 제출 절차

이 저장소의 fastlane에는 App Store 심사 제출 lane이 없으므로 App Store Connect에서 직접 제출한다.

1. App Store Connect에서 iOS 버전 `1.8.0`을 만든다.
2. 위 출시 노트를 언어별 "이번 버전의 새로운 기능"에 입력한다.
3. 빌드 선택 기준에 맞는 build를 선택한다.
4. App Review 정보의 메모에 위 App Review 메모를 입력한다.
5. 심사에 제출한다.

## 제출 기록

- 2026-11-16: build `1.8.0 (2)`로 심사 제출. 출시 방식은 승인 후 자동 출시, 모든 사용자에게 즉시 업데이트, 기존 평가 유지.
- 노르웨이어 현지화는 기존에 한국어 내용으로 채워져 있어 업데이트 문구도 한국어와 같다.
