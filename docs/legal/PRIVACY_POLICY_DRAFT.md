<!--
법률 자문이 아닌 검토용 초안입니다. 공개 전 대한민국·미국·일본의 적용 법령과 실제
Firebase/AdMob 콘솔 설정을 변호사 또는 개인정보보호 전문가에게 확인받아야 합니다.
korean-privacy-terms e390f7b9feb825e368c26726363ea5ce11a34083의 KR-PIPA 구조를 기준으로 작성했습니다.
-->

# 반다라트 개인정보처리방침

시행 예정일: 2026년 8월 10일

이지훈(이하 “운영자”)은 반다라트 모바일 애플리케이션(이하 “서비스”) 이용자의 개인정보를 중요하게 생각합니다. 이 방침은 대한민국 개인정보 보호법을 기본으로 하며, 미국 및 일본 이용자에게도 아래의 지역별 권리를 제공합니다.

서비스는 만 14세 이상만 이용할 수 있습니다. 운영자는 만 14세 미만 아동의 개인정보를 고의로 수집하지 않으며, 해당 사실을 알게 되면 지체 없이 삭제합니다.

## 1. 처리하는 정보, 목적 및 보유기간

| 구분 | 처리 항목 | 목적 | 보유기간 |
|---|---|---|---|
| 기기 내 서비스 데이터 | 목표·세부 목표·진행 상태·색상·이모지·메모·마감일·알림 설정 | 목표 관리 및 로컬 알림 제공 | 이용자가 앱에서 삭제하거나 앱 데이터 삭제 또는 앱 제거 시까지. 이 데이터는 운영자 서버로 전송되지 않습니다. |
| 이메일 문의 | 발신 이메일 주소, 문의 제목·본문, 이용자가 직접 첨부한 파일 | 문의·불만 처리 및 답변 | 문의 처리 완료 후 1년. 분쟁 또는 법령상 보존 의무가 있는 경우 해당 기간까지 보관 후 삭제합니다. |
| Firebase Analytics | 앱 인스턴스 ID, 앱 실행·세션·화면 조회·상호작용, 앱 버전, 운영체제·기기 정보, 마스킹된 IP 주소에서 파생된 대략적 지역 | 서비스 이용 현황 분석 및 품질 개선 | 사용자·이벤트 단위 데이터는 2개월. 집계 보고서는 개인을 식별하지 않는 형태로 더 오래 보관될 수 있습니다. |
| Firebase Crashlytics | 충돌 스택 추적, 충돌 당시 앱 상태, 기기·운영체제 정보, 앱 버전, 충돌 시각, Crashlytics 설치 UUID·Firebase 설치 ID·세션 ID | 오류 진단, 안정성 및 보안 개선 | 수집일부터 90일 후 삭제 절차를 시작합니다. |
| Firebase Remote Config | 국가·언어·시간대 코드, 운영체제·플랫폼 버전, 앱 ID·패키지명·앱 버전·SDK 버전, Firebase 설치 ID | 앱 업데이트 없이 기능 설정을 안전하게 제공하고 서비스 품질을 유지 | 서비스 제공 중 보유하며, 삭제 요청에 따라 Firebase 설치 ID 삭제를 요청합니다. Google은 삭제 요청 후 라이브·백업 시스템에서 관련 데이터를 제거하는 데 최대 180일이 걸릴 수 있습니다. |
| Google AdMob — Android·iOS | IP 주소와 그로부터 파생된 대략적 위치, 광고 노출·탭·동영상 조회와 같은 상호작용, 앱·광고 SDK 성능 및 진단 정보, 광고 ID 또는 앱·개발자 범위의 기기 식별자, 기기·브라우저 정보 | 비맞춤형 문맥 광고 제공, 노출 빈도 제한, 집계 보고, 광고 부정 사용 방지 | 사용자 활동 보고서는 90일, 광고 활동·코호트·개인정보 메시지 보고서는 최대 2,555일. 광고 서버 로그의 IP 일부는 9개월 후, 쿠키 정보는 18개월 후 익명화됩니다. 보안·부정 사용 방지 또는 법적 의무에 필요한 정보는 Google 정책에 따라 더 오래 보관될 수 있습니다. |

운영자는 회원가입, 로그인 또는 운영자 서버를 통한 목표 데이터 동기화 기능을 제공하지 않습니다. 사용자가 사진 보관함 저장을 선택하면 생성된 이미지는 사용자의 기기에만 저장되며 운영자에게 전송되지 않습니다.

## 2. 자동 수집 기술과 비맞춤형 광고

Android 및 iOS 버전은 Google AdMob의 비맞춤형 광고만 요청합니다. 광고는 과거의 앱·웹 이용 이력을 이용한 관심사 기반 타기팅이 아니라 현재 앱의 문맥과 대략적 지역 같은 정보로 선택됩니다.

비맞춤형 광고도 다음 목적에는 광고 ID, 앱 식별자 또는 이와 유사한 기술을 사용할 수 있습니다.

- 동일 광고의 과도한 노출 방지
- 집계된 광고 성과 측정
- 무효 트래픽, 사기 및 보안 위협 탐지

운영자는 AdMob의 Publisher first-party ID를 비활성화하고, Firebase Analytics의 광고 ID 수집(Android) 및 광고 개인화 신호(Android·iOS)를 비활성화합니다. 운영자는 건강, 학교·교실 정보 또는 목표·메모 내용을 광고 타기팅에 사용하지 않습니다.

이용자는 Android의 광고 개인정보 설정에서 광고 ID를 재설정 또는 삭제할 수 있고, iOS의 개인정보 보호 및 추적 설정을 관리할 수 있으며, 이메일로 데이터 처리 중지 또는 삭제를 요청할 수 있습니다. 기술적으로 광고 제공에 반드시 필요한 처리를 거부하면 광고가 제한되거나 제공되지 않을 수 있습니다.

## 3. 개인정보의 제3자 제공 및 처리위탁

운영자는 개인정보를 판매하지 않습니다. 서비스 운영에 필요한 범위에서 다음 사업자가 정보를 처리합니다.

| 처리자 | 업무 | 처리 정보 | 처리 장소 |
|---|---|---|---|
| Google LLC 및 Firebase 재수탁자 | Firebase Analytics, Crashlytics, Remote Config 제공 | 제1조의 Firebase 항목 | 미국 및 Google 또는 그 대리인이 시설을 운영하는 국가 |
| Google LLC 및 Google의 승인된 광고 기술 제공업체 | Android·iOS 비맞춤형 광고 제공·측정·부정 사용 방지 | 제1조의 AdMob 항목. 비맞춤형 입찰 요청에서는 광고 ID와 Google 사용자 ID 같은 식별자를 제거하지만, 축약된 IP 주소와 user-agent 같은 신호가 전송될 수 있습니다. | 미국 및 각 광고 기술 제공업체의 처리 국가 |

처리자 목록과 소재 국가는 서비스 구성 및 공급자 정책 변경에 따라 달라질 수 있습니다. 최신 Google 재수탁자와 광고 기술 제공업체는 다음 페이지에서 확인할 수 있습니다.

- Firebase 재수탁자: https://firebase.google.com/terms/subprocessors
- Google 광고 기술 제공업체: https://support.google.com/admob/answer/9012903

법령에서 요구하는 경우 운영자는 별도 동의를 받거나 이용자가 선택권을 행사할 수 있는 방법을 제공합니다.

## 4. 개인정보의 국외 이전

| 수령인 및 문의처 | 이전 국가 | 이전 시점·방법 | 이전 항목 | 목적 | 보유기간 |
|---|---|---|---|---|---|
| Google LLC, Google Privacy Help: https://support.google.com/policies/troubleshooter/7575787 | 미국 및 Google 또는 대리인이 시설을 운영하는 국가 | 앱 실행, 분석 이벤트·충돌 발생, 원격 설정 조회 또는 Android·iOS 광고 요청 시 암호화된 네트워크로 전송 | 제1조의 Firebase 및 AdMob 항목 | 분석, 오류 진단, 원격 설정, 비맞춤형 광고, 보안·부정 사용 방지 | 제1조의 각 보유기간 |

이용자는 운영자에게 국외 이전 중지 또는 삭제를 요청할 수 있습니다. 필수적인 원격 설정·오류 진단·광고 처리를 거부하면 일부 분석 기능이 중단되고 광고가 제한되거나 제공되지 않을 수 있으나, 기기 내 목표 관리 기능은 계속 사용할 수 있습니다.

## 5. 파기 절차와 방법

보유기간이 끝나거나 처리 목적이 달성된 정보는 복구하기 어렵도록 삭제합니다. 이메일 문의는 메일함과 별도 보관본에서 삭제합니다. 제3자 SDK가 처리하는 정보는 각 공급자의 삭제 절차에 따라 삭제 또는 익명화됩니다. 기기 내 서비스 데이터는 이용자가 항목을 삭제하거나 운영체제 설정에서 앱 데이터를 삭제하거나 앱을 제거하여 직접 삭제할 수 있습니다.

## 6. 이용자의 권리와 행사 방법

이용자는 자신의 개인정보에 대해 열람, 정정, 삭제, 처리정지, 동의 철회 및 사본 제공을 요청할 수 있습니다. 요청은 `mraz3068@gmail.com`으로 접수할 수 있으며, 운영자는 신원과 요청 범위를 확인한 뒤 적용 법령이 정한 기간 안에 처리합니다. 법률상 보존 의무, 타인의 권리 보호 또는 보안상 필요한 경우 요청의 일부가 제한될 수 있으며 그 사유를 안내합니다.

## 7. 미국 거주자의 추가 권리

적용되는 미국 주 개인정보보호법에 따라 거주자는 다음 권리를 가질 수 있습니다.

- 개인정보의 범주·출처·목적·제공받는 자에 대한 확인 및 접근
- 부정확한 정보의 정정, 삭제 및 이동 가능한 사본 요청
- 개인정보의 판매·공유 또는 타기팅 광고에 대한 거부
- 권리 행사로 인한 차별을 받지 않을 권리와, 적용되는 경우 결정에 대한 이의 제기

운영자는 개인정보를 판매하지 않고, 과거 행동에 기반한 타기팅 광고를 제공하지 않습니다. 권리 요청은 제6조의 이메일로 제출할 수 있습니다.

## 8. 일본 거주자의 추가 권리

일본의 개인정보보호법(APPI)이 적용되는 범위에서 이용자는 보유 개인데이터의 이용 목적 통지, 공개, 정정·추가·삭제, 이용 정지·소거 및 제3자 제공 정지를 요청할 수 있습니다. 요청 방법은 제6조와 같으며, 요청을 거절하거나 일부 제한하는 경우 적용 법령에 따라 사유를 안내합니다.

## 9. 안전성 확보조치

운영자는 다음 조치를 적용합니다.

- SDK 전송 구간 암호화(TLS/HTTPS)
- Firebase 및 Google 계정 접근권한 최소화와 인증 보호
- 앱의 목표·메모 데이터를 기본적으로 기기에만 저장
- 분석 데이터의 광고 ID 수집 및 광고 개인화 신호 비활성화
- 접근 기록 확인과 SDK·의존성 보안 업데이트
- 보유기간이 지난 문의 정보의 정기 삭제

## 10. 개인정보 보호책임자 및 구제수단

- 개인정보 보호책임자: 이지훈
- 이메일: `mraz3068@gmail.com`

대한민국 이용자는 개인정보침해 신고센터(https://privacy.kisa.or.kr, 국번 없이 118), 개인정보분쟁조정위원회(https://www.kopico.go.kr, 1833-6972), 경찰청 사이버범죄 신고시스템(https://ecrm.police.go.kr, 국번 없이 182)을 이용할 수 있습니다.

## 11. 방침의 변경

이 방침의 내용이 바뀌면 시행 전에 앱 또는 공개된 개인정보처리방침 페이지에서 알립니다. 이용자 권리에 중대한 영향을 주는 변경은 합리적인 사전 통지 기간을 둡니다.

- 최초 시행: 2024년 11월 22일
- 전면 개정 예정: 2026년 8월 10일

---

# Bandalart Privacy Policy

Planned effective date: August 10, 2026

Jihoon Lee (the “Operator”) respects the privacy of users of the Bandalart mobile application (the “Service”). This Policy is based primarily on the Personal Information Protection Act of South Korea and also provides the regional rights described below to users in the United States and Japan.

The Service is intended only for users aged 14 or older. The Operator does not knowingly collect personal information from children under 14 and will delete it without undue delay if discovered.

## 1. Information processed, purposes, and retention

| Category | Information | Purpose | Retention |
|---|---|---|---|
| On-device service data | Goals, sub-goals, progress, colors, emoji, notes, deadlines, and reminder settings | Goal management and local reminders | Until deleted by the user, app data is cleared, or the app is uninstalled. It is not transmitted to the Operator’s server. |
| Email inquiries | Sender email address, subject, message, and files voluntarily attached | Support, complaints, and responses | One year after completion, unless longer retention is required for a dispute or by law. |
| Firebase Analytics | App-instance ID; app launches, sessions, screen views and interactions; app version; OS and device data; general region derived from masked IP | Usage analytics and service improvement | User- and event-level data for 2 months. Non-identifying aggregate reports may remain longer. |
| Firebase Crashlytics | Crash stack traces and app state; device and OS data; app version and crash time; Crashlytics installation UUID, Firebase installation ID, and session ID | Error diagnosis, reliability, and security | Deletion begins 90 days after collection. |
| Firebase Remote Config | Country, language and time-zone codes; OS and platform version; app ID, package name, app and SDK versions; Firebase installation ID | Secure remote configuration and service quality | While needed to provide the Service. Following a deletion request, removal from Google’s live and backup systems may take up to 180 days. |
| Google AdMob — Android and iOS | IP address and approximate location; ad impressions, taps and video views; app and SDK diagnostics; advertising ID or other app- or developer-bounded device identifiers; device and browser data | Contextual non-personalized ads, frequency capping, aggregate reporting, fraud prevention | User Activity reports: 90 days. Ads Activity, Cohort, and Privacy & Messaging reports: up to 2,555 days. Google anonymizes part of IP addresses in advertising server logs after 9 months and cookie data after 18 months. Limited security, fraud-prevention, and legal records may be kept longer under Google’s policies. |

The Service has no account, login, or Operator-hosted goal synchronization. Images saved to the photo library at the user’s request remain on the device and are not sent to the Operator.

## 2. Automatic technologies and non-personalized ads

The Android and iOS apps request only non-personalized ads. Ads are selected using current context and approximate location, not a user’s historical app or web activity. Identifiers may still be used for frequency capping, aggregate measurement, fraud prevention, and security.

The Operator disables AdMob Publisher first-party ID, Android Analytics advertising-ID collection, and advertising-personalization signals for Analytics on Android and iOS. Health data, school or classroom data, and the content of goals or notes are never used for ad targeting.

Users may reset or delete their advertising ID in Android privacy settings, manage privacy and tracking settings in iOS, and request cessation or deletion by email. Refusing processing strictly necessary for ad delivery may limit or disable ads, but does not prevent on-device goal management.

## 3. Service providers and disclosure

The Operator does not sell personal information. Google LLC and its subprocessors provide Firebase Analytics, Crashlytics, Remote Config, and AdMob for Android and iOS. Data described in Section 1 may be processed in the United States and other locations where Google, its agents, or approved ad technology providers operate facilities.

For non-personalized bid requests, identifiers such as advertising ID and Google user ID are removed, while signals such as truncated IP address and user-agent may still be sent. Current provider lists are available at:

- Firebase subprocessors: https://firebase.google.com/terms/subprocessors
- Google ad technology providers: https://support.google.com/admob/answer/9012903

Where required by law, the Operator will obtain separate consent or provide a method to exercise applicable choices.

## 4. International transfers

Data is transferred to Google LLC in the United States and other Google processing locations over encrypted networks when the app launches, an analytics event or crash occurs, Remote Config is queried, or the Android or iOS app requests an ad. The information, purposes, and retention periods are listed in Section 1. Google privacy contact: https://support.google.com/policies/troubleshooter/7575787.

Users may request cessation or deletion of an international transfer. Firebase analytics or diagnostics may stop and ads may be limited or unavailable, but on-device goal management remains available.

## 5. Deletion

Information is securely deleted or de-identified when its purpose or retention period ends. Email inquiries are removed from mailboxes and retained copies. SDK data follows each provider’s deletion process. Users can delete on-device data by deleting items, clearing app data, or uninstalling the app.

## 6. User rights

Users may request access, correction, deletion, restriction, withdrawal of consent, and a copy of their information by emailing `mraz3068@gmail.com`. The Operator will verify identity and scope and respond within the period required by applicable law. A request may be limited where retention is legally required or necessary to protect others or maintain security, and the reason will be explained.

## 7. Additional rights for U.S. residents

Where applicable, U.S. residents may request access, correction, deletion, portability, and information about categories, sources, purposes, and recipients; opt out of sale, sharing, or targeted advertising; exercise rights without discrimination; and appeal eligible decisions. The Operator does not sell personal information or provide ads targeted from past behavior.

## 8. Additional rights for residents of Japan

To the extent Japan’s APPI applies, users may request notice of purpose, disclosure, correction, addition, deletion, cessation of use, erasure, and cessation of third-party provision of retained personal data. Requests use the contact in Section 6.

## 9. Security

The Operator uses encrypted transmission, least-privilege access to Google accounts, on-device storage by default, disabled ad-personalization signals, dependency security updates, access review, and scheduled deletion of expired inquiries.

## 10. Contact and complaints

- Privacy officer: Jihoon Lee
- Email: `mraz3068@gmail.com`

South Korean users may also contact the Privacy Infringement Report Center (https://privacy.kisa.or.kr), the Personal Information Dispute Mediation Committee (https://www.kopico.go.kr), or the Korean National Police cybercrime reporting system (https://ecrm.police.go.kr).

## 11. Changes

Changes will be announced in the app or on the published policy page before they take effect. Material changes affecting user rights will receive reasonable advance notice.

- First effective: November 22, 2024
- Full revision planned: August 10, 2026

---

# Bandalart プライバシーポリシー

施行予定日：2026年8月10日

イ・ジフン（以下「運営者」）は、Bandalartモバイルアプリ（以下「本サービス」）の利用者のプライバシーを尊重します。本ポリシーは韓国の個人情報保護法を主な基準とし、米国および日本の利用者にも以下の地域別権利を提供します。

本サービスは14歳以上の方のみを対象とします。運営者は14歳未満の児童の個人情報を意図的に収集せず、その事実を確認した場合は遅滞なく削除します。

## 1. 取り扱う情報、目的および保存期間

| 区分 | 情報 | 目的 | 保存期間 |
|---|---|---|---|
| 端末内のサービスデータ | 目標、詳細目標、進捗、色、絵文字、メモ、期限、通知設定 | 目標管理およびローカル通知 | 利用者による削除、アプリデータの消去、またはアンインストールまで。運営者のサーバーには送信されません。 |
| メールでのお問い合わせ | 送信元メールアドレス、件名、本文、利用者が任意に添付したファイル | 問い合わせ・苦情対応 | 対応完了後1年間。紛争または法令上必要な場合はその期間まで。 |
| Firebase Analytics | アプリインスタンスID、起動・セッション・画面表示・操作、アプリバージョン、OS・端末情報、マスクされたIPから推定されるおおよその地域 | 利用状況の分析および品質改善 | ユーザー・イベント単位データは2か月。個人を識別しない集計レポートはより長く保存される場合があります。 |
| Firebase Crashlytics | クラッシュのスタックトレース、アプリ状態、端末・OS情報、アプリバージョン、発生時刻、CrashlyticsインストールUUID、FirebaseインストールID、セッションID | 不具合診断、安定性・セキュリティ向上 | 収集から90日後に削除処理を開始します。 |
| Firebase Remote Config | 国・言語・タイムゾーン、OS・プラットフォーム、アプリID・パッケージ名・アプリ/SDKバージョン、FirebaseインストールID | 安全な遠隔設定および品質維持 | サービス提供中。削除依頼後、Googleの稼働・バックアップシステムからの削除に最大180日かかる場合があります。 |
| Google AdMob（Android・iOS） | IPアドレスとおおよその位置、広告表示・タップ・動画視聴、アプリ・SDK診断情報、広告IDまたはアプリ・開発者単位の端末識別子、端末・ブラウザ情報 | 文脈に基づく非パーソナライズ広告、表示回数制限、集計、詐欺防止 | User Activityは90日、Ads Activity・Cohort・Privacy & Messagingは最大2,555日。広告サーバーログのIPの一部は9か月後、Cookie情報は18か月後に匿名化されます。セキュリティ、詐欺防止、法令対応の記録はGoogleの方針により長く保存される場合があります。 |

本サービスにはアカウント、ログイン、運営者サーバーへの目標同期はありません。利用者が写真ライブラリへの保存を選択した画像は端末内に保存され、運営者へ送信されません。

## 2. 自動収集技術と非パーソナライズ広告

Android版およびiOS版は非パーソナライズ広告のみをリクエストします。広告は過去のアプリ・ウェブ行動ではなく、現在の文脈やおおよその地域に基づいて選択されます。ただし、表示回数制限、集計測定、詐欺防止およびセキュリティのために識別子が使用される場合があります。

運営者はAdMobのPublisher first-party ID、Android Analyticsの広告ID収集、およびAndroid・iOSのAnalytics広告パーソナライズシグナルを無効にします。健康情報、学校・教室情報、目標やメモの内容を広告ターゲティングに使用しません。

利用者はAndroidの広告プライバシー設定で広告IDをリセットまたは削除し、iOSのプライバシーおよびトラッキング設定を管理できます。また、メールで処理停止・削除を依頼できます。広告配信に必要な処理を拒否した場合、広告が制限または停止することがありますが、端末内の目標管理は引き続き利用できます。

## 3. 委託先および第三者への提供

運営者は個人情報を販売しません。Google LLCとその再委託先はFirebase Analytics、Crashlytics、Remote ConfigおよびAndroid・iOS向けAdMobを提供し、第1項の情報を米国その他Google、代理人または承認済み広告技術事業者の施設所在地で処理する場合があります。

非パーソナライズ入札リクエストでは、広告IDやGoogleユーザーIDなどは除去されますが、短縮されたIPアドレスやuser-agentなどは送信される場合があります。最新の事業者一覧：

- Firebase再委託先：https://firebase.google.com/terms/subprocessors
- Google広告技術事業者：https://support.google.com/admob/answer/9012903

法令上必要な場合、運営者は別途同意を取得し、または選択権を行使する方法を提供します。

## 4. 国外移転

アプリの起動、分析イベント・クラッシュの発生、Remote Configの照会、またはAndroid・iOS広告リクエスト時に、第1項の情報が暗号化通信によりGoogle LLC（米国およびその他の処理拠点）へ移転されます。目的と保存期間は第1項のとおりです。Googleへのプライバシー問い合わせ：https://support.google.com/policies/troubleshooter/7575787

利用者は国外移転の停止または削除を依頼できます。その場合、分析・診断が停止し、広告が制限または提供されないことがありますが、端末内の目標管理は引き続き利用できます。

## 5. 削除

目的の達成または保存期間の終了後、情報を安全に削除または匿名化します。メール問い合わせはメールボックスおよび保存コピーから削除します。SDKデータは各提供者の削除手続に従います。端末内データは、項目の削除、アプリデータの消去、またはアンインストールにより削除できます。

## 6. 利用者の権利

利用者は、アクセス、訂正、削除、処理停止、同意撤回およびコピーの提供を`mraz3068@gmail.com`へ依頼できます。運営者は本人確認と範囲確認を行い、適用法令の期間内に対応します。法的保存義務、他者の権利保護またはセキュリティ上必要な場合は一部を制限し、その理由を説明します。

## 7. 米国居住者の追加権利

適用される州法に基づき、米国居住者は情報のカテゴリー・出所・目的・受領者の確認、アクセス、訂正、削除、ポータビリティ、販売・共有・ターゲティング広告からのオプトアウト、差別されない権利および該当する決定への異議申立てを行える場合があります。運営者は個人情報を販売せず、過去の行動に基づくターゲティング広告を提供しません。

## 8. 日本居住者の追加権利

個人情報保護法（APPI）が適用される範囲で、利用者は保有個人データの利用目的の通知、開示、訂正・追加・削除、利用停止・消去および第三者提供停止を請求できます。請求先は第6項と同じです。

## 9. 安全管理措置

運営者は、通信の暗号化、Googleアカウントへの最小権限アクセス、端末内保存の原則、広告パーソナライズシグナルの無効化、依存関係のセキュリティ更新、アクセス確認および保存期間を過ぎた問い合わせの定期削除を実施します。

## 10. 問い合わせおよび苦情

- 個人情報保護責任者：イ・ジフン
- メール：`mraz3068@gmail.com`

韓国の利用者は、個人情報侵害申告センター（https://privacy.kisa.or.kr）、個人情報紛争調整委員会（https://www.kopico.go.kr）、警察庁サイバー犯罪申告システム（https://ecrm.police.go.kr）も利用できます。

## 11. 本ポリシーの変更

変更は施行前にアプリまたは公開ポリシーページで告知します。利用者の権利に重大な影響を与える変更については、合理的な事前通知期間を設けます。

- 初回施行：2024年11月22日
- 全面改定予定：2026年8月10日
