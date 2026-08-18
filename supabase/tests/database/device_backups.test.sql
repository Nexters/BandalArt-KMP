begin;

create extension if not exists pgtap with schema extensions;

select plan(17);

select has_table('public', 'device_backups', 'device_backups table exists');
select has_function(
    'public',
    'get_device_backup',
    array['text'],
    'get_device_backup RPC exists'
);
select has_function(
    'public',
    'put_device_backup',
    array['text', 'integer', 'jsonb', 'integer'],
    'put_device_backup RPC exists'
);
select has_function(
    'public',
    'delete_device_backup',
    array['text'],
    'delete_device_backup RPC exists'
);

select ok(
    not has_table_privilege('anon', 'public.device_backups', 'select'),
    'anon cannot select the backup table directly'
);
select ok(
    not has_table_privilege('anon', 'public.device_backups', 'insert'),
    'anon cannot insert into the backup table directly'
);
select ok(
    has_function_privilege('anon', 'public.get_device_backup(text)', 'execute'),
    'anon can execute get_device_backup'
);
select ok(
    has_function_privilege(
        'anon',
        'public.put_device_backup(text,integer,jsonb,integer)',
        'execute'
    ),
    'anon can execute put_device_backup'
);
select ok(
    has_function_privilege('anon', 'public.delete_device_backup(text)', 'execute'),
    'anon can execute delete_device_backup'
);

select lives_ok(
    $$
        select *
        from public.put_device_backup(
            repeat('a', 64),
            1,
            '{"bandalarts": []}'::jsonb,
            0
        )
    $$,
    'a valid backup can be stored'
);

select results_eq(
    $$
        select schema_version, bandalart_count
        from public.get_device_backup(repeat('a', 64))
    $$,
    $$ values (1, 0) $$,
    'the stored backup metadata can be read'
);

select is(
    (
        select payload
        from public.get_device_backup(repeat('a', 64))
    ),
    '{"bandalarts": []}'::jsonb,
    'the stored backup payload can be read'
);

select lives_ok(
    $$
        select *
        from public.put_device_backup(
            repeat('a', 64),
            2,
            '{"bandalarts": [{"id": 1}]}'::jsonb,
            1
        )
    $$,
    'an existing backup can be replaced'
);

select results_eq(
    $$
        select schema_version, bandalart_count
        from public.get_device_backup(repeat('a', 64))
    $$,
    $$ values (2, 1) $$,
    'replacement metadata is returned'
);

select throws_like(
    $$
        select *
        from public.put_device_backup('raw-ssaid', 1, '{}'::jsonb, 0)
    $$,
    '%invalid device key%',
    'raw or malformed device keys are rejected'
);

select is(
    public.delete_device_backup(repeat('a', 64)),
    true,
    'an existing backup can be deleted'
);
select is(
    public.delete_device_backup(repeat('a', 64)),
    false,
    'deleting an absent backup is idempotent'
);

select * from finish();
rollback;
