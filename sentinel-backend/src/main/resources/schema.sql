create table if not exists users (
    id bigserial primary key,
    username varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at timestamp with time zone not null default now()
);

alter table users
    add column if not exists username varchar(255);

alter table users
    add column if not exists password_hash varchar(255);

alter table users
    add column if not exists created_at timestamp with time zone default now();

update users
set created_at = now()
where created_at is null;

alter table users
    alter column username set not null,
    alter column password_hash set not null,
    alter column created_at set not null;

create unique index if not exists users_username_unique_idx
    on users (username);

create table if not exists user_sessions (
    id bigserial primary key,
    token_id varchar(255) not null unique,
    user_id bigint not null,
    expires_at timestamp with time zone not null,
    revoked boolean not null default false,
    created_at timestamp with time zone not null default now(),
    constraint user_sessions_user_id_fk
        foreign key (user_id)
        references users (id)
        on delete cascade
);

alter table user_sessions
    add column if not exists token_id varchar(255);

alter table user_sessions
    add column if not exists user_id bigint;

alter table user_sessions
    add column if not exists expires_at timestamp with time zone;

alter table user_sessions
    add column if not exists revoked boolean default false;

alter table user_sessions
    add column if not exists created_at timestamp with time zone default now();

update user_sessions
set revoked = false
where revoked is null;

update user_sessions
set created_at = now()
where created_at is null;

alter table user_sessions
    alter column token_id set not null,
    alter column user_id set not null,
    alter column expires_at set not null,
    alter column revoked set not null,
    alter column created_at set not null;

create unique index if not exists user_sessions_token_id_unique_idx
    on user_sessions (token_id);

create index if not exists user_sessions_user_id_idx
    on user_sessions (user_id);
