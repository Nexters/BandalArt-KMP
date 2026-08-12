# iOS 1.2.0 App Store 출시 자료

- 관련 이슈: [#332](https://github.com/Nexters/BandalArt-KMP/issues/332)
- 공개 기준 버전: `1.0.1`
- 출시 버전: `1.2.0`
- 위젯 지원: iOS 17 이상

이 문서는 iOS 1.2.0의 한국어·영어·일본어 출시 노트, App Review 메모와 심사 빌드 선택 기준을 정리한다.

## 한국어 출시 노트

홈 화면에서 목표를 더 가까이 확인해 보세요.

• 반다라트 홈 화면 위젯을 새롭게 추가했어요. 마지막으로 본 반다라트의 진행률과 할 일을 확인하고 완료 상태를 변경할 수 있어요.
• 목표 템플릿 5종으로 새 반다라트를 빠르게 시작할 수 있어요.
• 마감일 당일 알림을 받고 해당 반다라트로 바로 이동할 수 있어요.
• 기존 아이콘은 그대로 유지하면서 Fluent Color 이모지 300개를 추가했어요. 카테고리와 최근 사용 목록에서 선택할 수 있어요.
• 태스크 셀을 길게 눌러 완료하거나 해제하고 진동으로 확인할 수 있어요.
• 홈 하단에 광고가 표시되며, 무료 생성 슬롯을 모두 사용하면 광고를 보고 슬롯을 추가할 수 있어요.
• 화면을 다듬고 안정성을 개선했어요.

## English release notes

Keep your goals closer with the new Home Screen widget.

• Added the BandalArt Home Screen widget. View the progress and tasks of your most recently opened BandalArt and update task completion.
• Start a new BandalArt with five goal templates.
• Receive a reminder on the due date and open the related BandalArt directly.
• Kept your existing icons and added 300 Fluent Color emojis, organized by category and recent use.
• Long-press a task cell to complete or reopen it, with vibration feedback.
• Ads now appear at the bottom of Home. After using all free slots, you can watch an ad to add another slot.
• Refined the interface and improved stability.

## 日本語リリースノート

新しいホーム画面ウィジェットで、目標をもっと身近に確認できます。

• BandalArtのホーム画面ウィジェットを追加しました。最後に表示したBandalArtの進捗とタスクを確認し、完了状態を変更できます。
• 5種類の目標テンプレートから新しいBandalArtをすぐに始められます。
• 期限当日に通知を受け取り、対象のBandalArtを直接開けます。
• 既存のアイコンはそのまま維持し、Fluent Color絵文字を300個追加しました。カテゴリーや最近使った項目から選べます。
• タスクセルを長押しして完了・未完了を切り替え、振動で確認できます。
• ホーム下部に広告が表示されます。無料作成枠を使い切った後は、広告を見て枠を追加できます。
• 画面を整え、安定性を改善しました。

## App Review 메모

```text
This update introduces the BandalArt Home Screen widget for the first time.

To test the widget:
1. Open the app and select a BandalArt.
2. Return to the Home Screen.
3. Add the BandalArt widget from the widget gallery.
4. The widget displays the most recently viewed BandalArt.
5. Task completion can be updated directly from the widget.

The widget requires iOS 17 or later. The widget is bundled with the main app as an extension and uses the App Group shared container for on-device data only.
```

## 빌드 선택 기준

- TestFlight 기능 검증용 빌드는 `ios_ads_mode = test`로 업로드한다.
- App Store 심사에 선택할 릴리스 후보는 `ios_ads_mode = production`으로 업로드한다.
- 두 모드는 같은 Release 구성과 서명을 사용하며, production 모드만 `BANDALART_TEST_ADS` 조건을 제거해 운영 AdMob 단위를 사용한다.
- 업로드된 바이너리의 광고 모드는 바꿀 수 없으므로 테스트 광고 빌드를 App Store 심사 빌드로 선택하지 않는다.
