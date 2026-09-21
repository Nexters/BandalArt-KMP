# iOS AdMob app-ads.txt 인증 조사

- 조사일: 2026-08-18
- App Store 앱 ID: `6743101965`
- iOS bundle ID: `com.nexters.bandalart.iosApp`
- AdMob publisher ID: `pub-5570932833347277`

## 결론

현재 외부에서 관측되는 App Store 메타데이터와 `app-ads.txt` 설정은 서로 일치한다. 따라서 iOS 1.2.0 바이너리나 파일 형식의 명백한 오류보다는 AdMob의 마지막 크롤 결과가 갱신되지 않았거나, 광고 요청량이 적어서 반영이 지연되는 경우가 가장 유력하다. 그다음으로는 AdMob의 해당 앱 레코드가 다른 Store ID, bundle ID 또는 계정에 연결된 경우를 확인해야 한다.

앱 버전 업데이트 자체가 인증 조건은 아니다. AdMob은 App Store에 공개된 개발자 웹사이트의 호스트를 찾고, 그 호스트 루트의 `app-ads.txt`에서 현재 AdMob 계정의 publisher ID를 확인한다.

## 관측 결과

### App Store

Apple의 공개 Lookup API는 다음 값을 반환한다.

- 버전: `1.2.0`
- 현재 버전 공개일: `2026-08-12T22:37:04Z`
- bundle ID: `com.nexters.bandalart.iosApp`
- seller URL: `https://bandal-art-fe.vercel.app/`

한국, 미국, 일본 storefront 모두 같은 seller URL을 반환했다.

### 개발자 웹사이트

`https://bandal-art-fe.vercel.app/app-ads.txt`의 실제 응답은 다음과 같다.

- 상태: `HTTP 200`
- Content-Type: `text/plain; charset=utf-8`
- Content-Length: `59`
- 본문: `google.com, pub-5570932833347277, DIRECT, f08c47fec0942fa0`
- `Google-adstxt` User-Agent에도 같은 `200` 응답
- `robots.txt`: `User-agent: *` / `Allow: /`

AdMob 화면에 표시된 개인화 스니펫과 공개 파일 본문은 정확히 일치한다. 웹사이트 저장소에는 이 파일이 2026-08-07부터 존재한다.

HTTP URL은 HTTPS의 동일 파일로 `308` 리다이렉트된다. Google 공식 안내는 다른 위치로의 리다이렉트를 지원하며, 현재 HTTPS 최종 응답은 `200`이다.

### 앱 소스

현재 저장소의 iOS 설정도 같은 publisher ID를 사용한다.

- GAD App ID: `ca-app-pub-5570932833347277~5025827437`
- production banner ID: `ca-app-pub-5570932833347277/4693940934`
- production rewarded ID: `ca-app-pub-5570932833347277/2754173309`

다만 App Store에 실제 배포된 바이너리가 `BANDALART_TEST_ADS` 없이 빌드됐는지는 소스만으로 확정할 수 없다. AdMob의 최근 쿼리 수나 App Store 설치본의 실제 광고 요청으로 확인해야 한다.

## 해석

Google은 일반적으로 크롤 및 인증에 최대 24시간이 걸릴 수 있다고 안내한다. 별도의 크롤 가능성 문서에서는 변경 반영에 며칠이 걸릴 수 있고, 광고 요청이 적은 사이트는 최대 한 달까지 걸릴 수 있다고 명시한다.

현재 화면의 문구는 AdMob이 과거 크롤 시점에 publisher ID를 찾지 못했거나, 최신 파일을 아직 인증 상태에 반영하지 못했다는 뜻으로 보는 것이 합리적이다. 현재 공개 파일에서 publisher ID 누락이나 형식 오류는 재현되지 않는다. 검색엔진에 남은 App Store 페이지 캐시 일부도 1.0.1 시절 정보로 관측된 반면 Apple의 현재 Lookup API는 1.2.0을 반환했으므로, 외부 수집 시스템의 메타데이터 갱신 지연 가능성과도 일관된다. 다만 이것만으로 AdMob 내부 캐시를 직접 입증할 수는 없다.

## 다음 확인 순서

1. AdMob 앱 설정에서 App Store ID `6743101965`와 bundle ID `com.nexters.bandalart.iosApp`가 연결됐는지 확인한다.
2. AdMob의 `앱 > 모든 앱 보기 > app-ads.txt`에서 해당 iOS 앱을 펼쳐 `app-ads.txt URL`, `마지막 크롤`, `최근 7일 쿼리 수`를 확인한다.
3. 표시 URL이 `bandal-art-fe.vercel.app/app-ads.txt`인지 확인한다. 다른 URL이면 그 URL을 현재 파일로 리다이렉트한다.
4. `업데이트 확인`을 한 번 누르고 최소 24~48시간 기다린다. 반복 클릭은 필요하지 않다.
5. 최근 7일 쿼리 수가 0이면 App Store의 1.2.0 설치본에서 홈 배너를 열어 실제 운영 광고 요청이 발생하는지 확인한다. 계속 0이면 배포 바이너리의 광고 모드와 AdMob App ID를 확인한다.
6. 마지막 크롤이 최신이고 쿼리도 존재하는데 같은 상태가 수일 이상 계속되면 현재의 Apple Lookup 응답, `app-ads.txt` HTTP 응답, AdMob의 마지막 크롤 시각을 첨부해 AdMob 지원에 문의한다.

## 출처

- [Google AdMob: Set up an app-ads.txt file](https://support.google.com/admob/answer/9363762?hl=en)
- [Google AdMob: Verify your app with app-ads.txt](https://support.google.com/admob/answer/14538460?hl=en)
- [Google AdMob: Ensure your app-ads.txt files can be crawled](https://support.google.com/admob/answer/9679128?hl=en)
- [Google AdMob: Understand app-ads.txt file statuses](https://support.google.com/admob/answer/9788846?hl=en)
- [Google AdMob: Resolve issues with app-ads.txt](https://support.google.com/admob/answer/9776740?hl=en)
- [Apple: Platform version information and Marketing URL](https://developer.apple.com/help/app-store-connect/reference/app-information/platform-version-information)
- [Apple Lookup API: BandalArt, Korea](https://itunes.apple.com/lookup?id=6743101965&country=kr)
- [BandalArt public app-ads.txt](https://bandal-art-fe.vercel.app/app-ads.txt)
- [BandalArt-FE app-ads.txt publication commit](https://github.com/easyhooon/BandalArt-FE/commit/b80e059724ff92a22a028bd13f3dd087d4a64f28)
