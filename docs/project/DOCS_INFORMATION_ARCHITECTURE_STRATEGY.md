# 문서 정보 구조 정리 전략

- 작성일: 2026-08-08
- 기준 브랜치: `origin/main`
- 대상: `docs/` 아래 Markdown 문서 41개와 저장소 내부 참조

## 배경

기능·마이그레이션·배포 문서가 모두 `docs/` 루트에 쌓여 문서의 성격과 최신 기준을 구분하기 어려워졌다. 특히 날짜 피커 작업 전략에 포함된 상태 보존 규칙이 사실상 프로젝트 공통 규칙으로 읽히면서, 이후 AdMob과 이모지 선택기에서 늘어난 상태의 수명을 판단할 단일 기준도 사라졌다.

## 목표

1. `docs/` 루트는 탐색용 `README.md`와 자주 직접 여는 `BACKLOG_EXECUTION_STRATEGY.md`만 유지한다.
2. 문서를 architecture, features, releases, project 아래의 주제 폴더로 묶는다.
3. `remember`, `rememberRetained`, `rememberSaveable`, durable persistence의 선택 기준을 living guide로 분리한다.
4. 저장소 안의 Markdown 링크와 경로 참조를 새 위치로 모두 갱신한다.

## 분류 원칙

- `GUIDE`: 반복해서 적용하는 현재의 프로젝트 규칙이다. 구현이 바뀌면 함께 갱신한다.
- `STRATEGY`: 특정 이슈나 변경의 범위·결정·검증 기록이다. 완료 뒤에도 당시 결정 기록으로 남긴다.
- `RESEARCH`, `SPIKE`, `BASELINE`: 결정을 뒷받침한 조사와 측정 결과다.
- 파일명은 검색과 Git history 추적을 위해 유지하고 디렉터리만 이동한다.
- 폴더별 README는 만들지 않는다. 전체 진입점은 `docs/README.md` 하나로 유지한다.

## 목표 구조

```text
docs/
├── README.md
├── BACKLOG_EXECUTION_STRATEGY.md
├── architecture/
│   ├── circuit/
│   ├── kmp/
│   ├── metro/
│   └── state/
├── features/
│   ├── ads/
│   ├── emoji/
│   ├── home/
│   ├── settings/
│   └── updates/
├── releases/
│   ├── android/
│   └── ios/
└── project/
```

아직 `main`에 없는 iOS TestFlight, Fastlane CD, Fluent Emoji 카테고리 문서는 각 작업 브랜치가 최신 `main`을 반영할 때 각각 `releases/ios`, `releases/automation`, `features/emoji`에 둔다. 이 정리 PR에서 다른 worktree의 미완료 문서를 가져오지 않는다.

## 이동과 참조 갱신

- `AGENTS.md`의 KMP 테스트 가이드 경로를 갱신하고 상태 관리 가이드를 프로젝트 규칙으로 연결한다.
- Circuit·Metro·KMP 문서의 상호 링크와 본문에 적힌 `docs/...` 경로를 갱신한다.
- Fluent Emoji resource spike가 참조하는 `tools/` 상대 경로는 깊어진 디렉터리에 맞게 수정한다.
- `BACKLOG_EXECUTION_STRATEGY.md`의 경로는 외부 진입점 호환성을 위해 바꾸지 않는다.

## 비범위

- 전략 문서의 역사적 완료 상태나 날짜를 일괄 재작성하지 않는다.
- 미완료 worktree의 문서를 복사하거나 병합하지 않는다.
- 코드, 빌드 설정, 앱 동작을 변경하지 않는다.
- 문서 제목과 파일명을 일괄 개명하지 않는다.

## 검증

1. `docs/README.md`에서 모든 추적 문서에 도달할 수 있어야 한다.
2. 저장소 내부의 상대 Markdown 링크가 모두 실제 파일을 가리켜야 한다.
3. 이동 전의 `docs/<파일명>.md` 경로가 `AGENTS.md`와 문서 본문에 남지 않아야 한다.
4. `git diff --check`가 통과해야 한다.
5. 작성과 별도의 검토 단계에서 상태 정책과 이동 맵을 확인한다.
