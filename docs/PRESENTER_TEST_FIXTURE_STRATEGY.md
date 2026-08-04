# Presenter Test Fixture 분리 전략

## 목표

- Presenter 테스트 파일에는 테스트 시나리오와 검증만 남긴다.
- Repository 대역과 테스트 데이터 생성 코드를 역할별 파일로 분리한다.
- 기존 테스트 동작과 가시성 범위는 유지한다.

## 대상

- `CompletePresenterTest`: `RecordingBandalartRepository` 분리
- `HomePresenterTest`: `FakeBandalartRepository`, `FakeInAppUpdateRepository`, Bandalart fixture 분리
- `OnboardingPresenterTest`: `FakeOnboardingRepository` 분리
- `SplashPresenterTest`: `FakeOnboardingRepository` 분리

## 원칙

- 대역은 해당 feature의 test source set 안에서만 접근 가능한 `internal`로 둔다.
- 서로 다른 feature에서 모양이 다른 대역을 무리하게 공용화하지 않는다.
- 테스트 시나리오와 검증 내용은 변경하지 않는다.

## 검증

- 각 feature Presenter 테스트 실행
- 변경된 모듈의 코드 스타일 검사 실행
