# Supabase 반다라트 백업 전략

## 목적

Android 또는 iOS 동일 기기에서 앱 데이터 삭제 또는 재설치 후 사용자가 수동으로 저장한 반다라트 백업을 복원한다. Room은 앱의 단일 로컬 데이터 원본으로 유지하고 Supabase는 최신 백업 스냅샷 한 건을 보관한다.

## 지원 범위

- Android SSAID에서 파생한 `deviceKey`로 동일 기기 백업을 식별한다.
- iOS Keychain에 보존한 랜덤 seed에서 파생한 `deviceKey`로 동일 기기 백업을 식별한다.
- 설정에서 사용자가 `지금 백업하기`를 실행할 때만 원격 스냅샷을 갱신한다.
- Android와 iOS 앱 시작 시 원격 백업 존재 여부를 조회한다.
- Room이 비어 있고 원격 백업이 있을 때만 `백업에서 복원 / 새로 시작`을 제안한다.
- 로컬 데이터가 있으면 자동으로 덮어쓰지 않는다.
- 실시간 동기화, 다른 기기 복원, Android와 iOS 간 복원은 포함하지 않는다.

## 데이터 모델

원격에는 `device_backups` 한 테이블만 둔다.

- `device_key`: 앱 namespace와 Android SSAID 또는 iOS Keychain seed에서 파생한 해시
- `schema_version`: 백업 payload 형식 버전
- `payload`: Room 반다라트와 셀, 복원 대상 DataStore 값을 담는 JSONB
- `bandalart_count`: 복원 안내에 표시할 반다라트 개수
- `updated_at`: 마지막 백업 시각

테마 선택값 `themeMode`(`system`, `light`, `dark`)도 `payload` 안의 DataStore 복원 대상에 포함한다. 백업을 불러오면 현재 기기의 테마 선택을 백업 값으로 덮어쓰고 앱에 즉시 반영한다. `themeMode`가 `null`이면 저장된 선택을 지우고 시스템 설정 모드를 사용한다.

테이블 직접 권한은 닫고 `get_device_backup`, `put_device_backup`, `delete_device_backup` RPC만 공개한다. 이 구조는 SSAID 파생 키를 인증 자격 증명으로 사용한다는 한계를 받아들이는 저민감도 MVP이며 원본 SSAID를 저장하거나 로그에 남기지 않는다.

## 저장소 구성

- Supabase CLI는 루트 `package.json`의 고정 버전을 `npx supabase` 또는 npm script로 실행한다.
- KMP 클라이언트는 기존 `kotlinx-datetime 0.6.x` ABI와 호환되는 `supabase-kt 3.2.0`을 사용한다.
- `supabase/config.toml`, `supabase/migrations`, `supabase/tests`를 버전 관리한다.
- secret과 CLI 임시 상태는 Git에 추가하지 않는다.
- 로컬 전체 스택은 Docker 호환 런타임이 있을 때만 실행한다.

Android와 iOS 앱의 Data API 연결값은 다음 순서로 KMP 빌드에 주입한다.

1. Gradle property `bandalart.supabaseUrl`, `bandalart.supabasePublishableKey`
2. 환경 변수 `BANDALART_SUPABASE_URL`, `BANDALART_SUPABASE_PUBLISHABLE_KEY`
3. Git에서 제외된 루트 `local.properties`의 같은 Gradle property

연결값이 없으면 앱은 클라우드 백업을 지원하지 않는 상태로 동작한다. publishable key만 앱에 포함하며 `secret` 또는 `service_role` key는 앱이나 저장소에 넣지 않는다.

## 단계

1. CLI 초기화와 최초 백업 스키마 migration을 추가한다.
2. 로컬 DB reset과 database test로 권한 및 RPC 동작을 검증한다.
3. 별도 Supabase 프로젝트를 생성 또는 선택하고 CLI로 link한다.
4. `db push --dry-run` 확인 후 migration을 원격에 적용한다.
5. KMP 백업 snapshot, Android SSAID provider, Supabase client와 복원 UI를 구현한다.

## 완료 조건

- 새 환경에서 의존성 설치 후 동일 CLI 버전을 실행할 수 있다.
- migration만으로 백업 테이블과 RPC를 재현할 수 있다.
- 공개 Data API에서 테이블을 직접 나열하거나 수정할 수 없다.
- 올바른 `deviceKey`로 백업 저장, 조회, 삭제가 가능하다.
- payload 크기와 schema version이 서버에서 검증된다.
- 원격 연결 정보와 secret이 Git에 포함되지 않는다.
