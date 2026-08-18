create table public.device_backups (
    device_key text primary key,
    schema_version integer not null,
    payload jsonb not null,
    bandalart_count integer not null,
    updated_at timestamp with time zone not null default now(),
    constraint device_backups_device_key_format
        check (device_key ~ '^[0-9a-f]{64}$'),
    constraint device_backups_schema_version_range
        check (schema_version between 1 and 1000),
    constraint device_backups_payload_object
        check (jsonb_typeof(payload) = 'object'),
    constraint device_backups_payload_size
        check (octet_length(payload::text) <= 1048576),
    constraint device_backups_bandalart_count_range
        check (bandalart_count between 0 and 10000)
);

comment on table public.device_backups is
    'Latest manual BandalArt backup for a SHA-256 device key.';
comment on column public.device_backups.device_key is
    'Lowercase SHA-256 hex derived from the app namespace and Android SSAID.';
comment on column public.device_backups.payload is
    'Versioned Room and DataStore backup envelope. Limited to 1 MiB.';

alter table public.device_backups enable row level security;

revoke all on table public.device_backups from public, anon, authenticated;
grant all on table public.device_backups to service_role;

create or replace function public.get_device_backup(p_device_key text)
returns table (
    schema_version integer,
    payload jsonb,
    bandalart_count integer,
    updated_at timestamp with time zone
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        backups.schema_version,
        backups.payload,
        backups.bandalart_count,
        backups.updated_at
    from public.device_backups as backups
    where p_device_key ~ '^[0-9a-f]{64}$'
      and backups.device_key = p_device_key;
$$;

create or replace function public.put_device_backup(
    p_device_key text,
    p_schema_version integer,
    p_payload jsonb,
    p_bandalart_count integer
)
returns table (
    schema_version integer,
    bandalart_count integer,
    updated_at timestamp with time zone
)
language plpgsql
security definer
set search_path = ''
as $$
begin
    if p_device_key is null or p_device_key !~ '^[0-9a-f]{64}$' then
        raise exception 'invalid device key' using errcode = '22023';
    end if;

    if p_schema_version is null or p_schema_version not between 1 and 1000 then
        raise exception 'invalid schema version' using errcode = '22023';
    end if;

    if p_payload is null
        or jsonb_typeof(p_payload) <> 'object'
        or octet_length(p_payload::text) > 1048576
    then
        raise exception 'invalid backup payload' using errcode = '22023';
    end if;

    if p_bandalart_count is null or p_bandalart_count not between 0 and 10000 then
        raise exception 'invalid bandalart count' using errcode = '22023';
    end if;

    insert into public.device_backups as backups (
        device_key,
        schema_version,
        payload,
        bandalart_count,
        updated_at
    )
    values (
        p_device_key,
        p_schema_version,
        p_payload,
        p_bandalart_count,
        now()
    )
    on conflict (device_key) do update
    set schema_version = excluded.schema_version,
        payload = excluded.payload,
        bandalart_count = excluded.bandalart_count,
        updated_at = excluded.updated_at;

    return query
    select
        backups.schema_version,
        backups.bandalart_count,
        backups.updated_at
    from public.device_backups as backups
    where backups.device_key = p_device_key;
end;
$$;

create or replace function public.delete_device_backup(p_device_key text)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    affected_rows integer;
begin
    if p_device_key is null or p_device_key !~ '^[0-9a-f]{64}$' then
        raise exception 'invalid device key' using errcode = '22023';
    end if;

    delete from public.device_backups as backups
    where backups.device_key = p_device_key;

    get diagnostics affected_rows = row_count;
    return affected_rows = 1;
end;
$$;

revoke all on function public.get_device_backup(text) from public;
revoke all on function public.put_device_backup(text, integer, jsonb, integer) from public;
revoke all on function public.delete_device_backup(text) from public;

grant execute on function public.get_device_backup(text) to anon, authenticated, service_role;
grant execute on function public.put_device_backup(text, integer, jsonb, integer) to anon, authenticated, service_role;
grant execute on function public.delete_device_backup(text) to anon, authenticated, service_role;
