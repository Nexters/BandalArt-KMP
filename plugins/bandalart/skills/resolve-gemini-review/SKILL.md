---
name: resolve-gemini-review
description: BandalArt PR의 미해결 Gemini Code Assist 리뷰를 검토하고 반영 또는 사유 있는 거절로 처리한다. 사용자가 resolve-gemini-review, Gemini 리뷰 확인해줘, 리뷰 반영해줘라고 요청할 때 사용한다.
---

# Resolve Gemini Review

1. 현재 branch PR 또는 명시된 PR을 찾고 draft가 아닌지 확인한다.
2. GitHub GraphQL로 Gemini Code Assist의 미해결 review thread를 수집한다.
3. security/crash, bug/logic, quality, style 순으로 정렬한다.
4. 각 comment의 실제 코드와 변경 범위를 확인한다.
5. 실제 결함이면 최소 변경으로 반영하고 comment별 commit/push 후 commit link로 답글을 남겨 thread를 resolve한다.
6. 과도한 범위, 의도된 설계, 후속 작업이면 구체적 거절 사유를 작성하되 게시 전 사용자 승인을 받는다.
7. PR에 누적 처리 요약을 남기고 적용/거절 수와 남은 thread를 보고한다.

hook을 우회하지 않고 AI boilerplate를 남기지 않는다. 리뷰 거절 답글은 반드시 사용자 승인 후 게시한다.
