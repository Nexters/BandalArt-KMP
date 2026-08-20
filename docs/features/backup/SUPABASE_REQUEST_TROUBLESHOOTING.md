# Supabase 요청·Realtime 오류 트러블슈팅

## 통신 구분

BandalArt 앱은 Supabase Data API의 PostgREST RPC만 사용한다. Supabase Realtime SDK, 채널, 구독은 사용하지 않는다.

| 주체 | 경로 또는 동작 | 발생 시점 |
| --- | --- | --- |
| 앱 | `POST /rest/v1/rpc/get_device_backup` | 로컬 데이터가 없는 앱 시작, 클라우드 백업 화면 진입, 복원 |
| 앱 | `POST /rest/v1/rpc/put_device_backup` | 사용자가 백업 생성 완료 |
| 앱 | `POST /rest/v1/rpc/delete_device_backup` | 사용자가 백업 삭제 완료 |
| Supabase 내부 Realtime 서버 | Postgres 직접 연결 후 `realtime.*` SQL | Realtime 서비스 초기화, migration, 구독 관리 |

따라서 Postgres 로그의 `relation "realtime.subscription" does not exist`는 앱 RPC가 아니다. Supabase가 관리하는 Realtime 서버와 프로젝트 Postgres 사이의 내부 서버 간 SQL이다.

## 2026-08-20 오류 분석

- 프로젝트 Overview의 최근 24시간 집계: 전체 892건, Postgres 849건, Postgres 오류 833건
- 반복 오류: SQLSTATE `42P01`, `relation "realtime.subscription" does not exist`
- 통합 로그 표본: 2026-08-20 01:00~02:00 UTC에 약 1분 간격으로 43건
- 확인 시점의 원격 DB에는 `realtime.subscription`이 존재하고 Realtime 내부 migration 81개가 적용되어 있었다.
- 2026-08-20 최신 60분 로그에는 Postgres 오류가 없었다.

프로젝트 생성 직후 Realtime 초기화와 내부 schema migration 순서가 어긋난 동안 서비스가 재시도한 것으로 판단한다. 현재 schema가 정상이고 오류가 멈췄으므로 앱 migration에서 `realtime` schema를 수정하거나 테이블을 재생성하지 않는다. 같은 오류가 다시 지속되면 Supabase Support에 프로젝트 ref와 UTC 발생 구간을 전달해 관리형 Realtime 서비스 재시작을 요청한다.

## 요청이 많아 보인 원인

오류 발생 당시 `pg_stat_statements` 누적치는 `get_device_backup` 95회, `put_device_backup` 10회, `delete_device_backup` 1회였다. 기존 앱은 로컬 데이터가 있어 복원 안내가 필요 없는 경우에도 시작할 때마다 `get_device_backup`을 호출했다. 이제 시작 정책은 먼저 로컬 데이터를 확인하고, 로컬 데이터가 비어 있을 때만 원격 조회를 실행한다.

클라우드 백업 화면 진입 시의 최신 백업 정보 조회와 사용자가 누른 생성·복원·삭제 요청은 기능상 필요한 통신이라 유지한다.

## 비용 판단

- 833건은 Realtime 메시지나 앱 API 요청 833건이 아니라 실패한 내부 Postgres SQL 로그다.
- 내부 Realtime 서버와 Postgres 사이 통신은 클라이언트 egress나 Realtime 메시지 사용량으로 계산하지 않는다.
- Free plan의 API 요청 횟수 자체는 무제한이지만 응답 데이터는 egress에 포함된다.
- 실패 로그는 Logs ingest 사용량에 포함될 수 있으므로 반복이 재발하면 운영 측 Realtime 복구를 요청한다.
- 프로젝트 compute 비용은 요청 수가 아니라 프로젝트가 실행된 시간과 compute size를 기준으로 발생한다.

## CLI 점검

프로젝트가 link된 저장소 루트에서 고정된 CLI 버전을 사용한다.

```shell
npx --no-install supabase projects list
npx --no-install supabase migration list --linked
npx --no-install supabase db query --linked \
  "select to_regclass('realtime.subscription') is not null as subscription_exists;"
npx --no-install supabase db query --linked \
  "select count(*) as migration_count, max(version) as latest_version from realtime.schema_migrations;"
npx --no-install supabase db query --linked \
  "select calls, total_exec_time, query from pg_stat_statements where query ilike '%device_backup%' order by calls desc;"
```

`auth`, `storage`, `realtime` 같은 Supabase 관리 schema의 migration table이나 내부 table은 앱 migration에서 직접 변경하지 않는다.

## 참고

- [Supabase Logs](https://supabase.com/docs/guides/monitoring-and-debugging/logs)
- [Logs ingest usage](https://supabase.com/docs/guides/platform/manage-your-usage/logs-ingest)
- [Egress usage](https://supabase.com/docs/guides/platform/manage-your-usage/egress)
- [Realtime pricing](https://supabase.com/docs/guides/realtime/pricing)
- [Realtime egress FAQ](https://supabase.com/docs/guides/troubleshooting/realtime-egress-faq)
