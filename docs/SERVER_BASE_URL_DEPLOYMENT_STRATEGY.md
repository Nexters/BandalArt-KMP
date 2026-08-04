# SERVER_BASE_URL 배포 의존성 제거 전략

## 배경

현재 앱의 주요 데이터 흐름은 로컬 DB와 DataStore를 사용하지만 `core:network`는
`SERVER_BASE_URL`을 `BuildConfig`로 노출하기 위해 Secrets Gradle Plugin을 사용한다.
이 때문에 서버 API를 사용하지 않는 배포에서도 `secrets.properties`와
GitHub Actions의 `SERVER_BASE_URL` secret이 없으면 빌드가 중단된다.

## 목표

- 네트워크 코드와 `BuildConfig.SERVER_BASE_URL` 참조는 유지한다.
- 로컬 개발자는 필요할 때 `secrets.properties`로 실제 URL을 덮어쓸 수 있다.
- CI와 Play Store 배포는 `SERVER_BASE_URL` secret 없이 빌드할 수 있다.
- 기본값이 실수로 실제 서버를 호출하지 않도록 비운영 URL을 사용한다.

## 변경

1. 추적 가능한 `local.defaults.properties`에 `https://localhost/` 기본값을 둔다.
2. Secrets Gradle Plugin의 입력 파일을 `secrets.properties`, fallback 파일을
   `local.defaults.properties`로 명시한다.
3. GitHub Actions에서 `SERVER_BASE_URL`로 `secrets.properties`를 생성하는 단계를 제거한다.

## 검증

- `secrets.properties`가 없는 환경에서 Gradle 설정이 완료되는지 확인한다.
- `BuildConfig.SERVER_BASE_URL` 생성에 필요한 기본 키가 유지되는지 확인한다.
- 변경 diff에 실제 서버 URL이나 credential이 포함되지 않았는지 확인한다.

