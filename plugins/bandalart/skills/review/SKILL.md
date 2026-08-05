---
name: review
description: BandalArt 현재 브랜치의 변경 라인만 base 기준으로 리뷰한다. 사용자가 review, 리뷰해줘, 로컬 리뷰라고 요청할 때 사용한다.
---

# Review

1. base를 명시값, branch history, `main` 순서로 결정하고 fetch한다.
2. `git diff origin/{base}...HEAD`에서 변경된 Kotlin/Gradle/Swift 파일과 관련 전략 문서를 읽는다.
3. 변경된 라인만 correctness, lifecycle/state, navigation, DI ownership, Android/iOS parity와 회귀 위험 기준으로 검토한다.
4. findings를 심각도 순으로 먼저 보고하고 파일과 라인을 명시한다.
5. 문제가 없으면 명확히 말하고 남은 test/build/manual verification gap을 적는다.

리뷰 요청만으로 코드를 수정하거나 전체 빌드를 실행하지 않는다.
