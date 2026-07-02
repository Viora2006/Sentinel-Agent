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

create table if not exists projects (
    id bigserial primary key,
    user_id bigint not null,
    name varchar(255) not null,
    description text,
    github_url varchar(2048) not null,
    repo_name varchar(255) not null,
    owner_name varchar(255) not null,
    default_branch varchar(255),
    total_files integer not null default 0,
    total_size_bytes bigint not null default 0,
    status varchar(50) not null,
    error_message text,
    created_at timestamp with time zone not null default now(),
    last_imported_at timestamp with time zone,
    constraint projects_user_id_fk
        foreign key (user_id)
        references users (id)
        on delete cascade
);

alter table projects
    add column if not exists user_id bigint;

alter table projects
    add column if not exists name varchar(255);

alter table projects
    add column if not exists description text;

alter table projects
    add column if not exists github_url varchar(2048);

alter table projects
    add column if not exists repo_name varchar(255);

alter table projects
    add column if not exists owner_name varchar(255);

alter table projects
    add column if not exists default_branch varchar(255);

alter table projects
    add column if not exists total_files integer default 0;

alter table projects
    add column if not exists total_size_bytes bigint default 0;

alter table projects
    add column if not exists status varchar(50);

alter table projects
    add column if not exists error_message text;

alter table projects
    add column if not exists created_at timestamp with time zone default now();

alter table projects
    add column if not exists last_imported_at timestamp with time zone;

update projects set name = repo_name where name is null and repo_name is not null;
update projects set total_files = 0 where total_files is null;
update projects set total_size_bytes = 0 where total_size_bytes is null;
update projects set status = 'FAILED' where status is null;
update projects set created_at = now() where created_at is null;

alter table projects
    alter column user_id set not null,
    alter column name set not null,
    alter column github_url set not null,
    alter column repo_name set not null,
    alter column owner_name set not null,
    alter column total_files set not null,
    alter column total_size_bytes set not null,
    alter column status set not null,
    alter column created_at set not null;

create table if not exists project_files (
    id bigserial primary key,
    project_id bigint not null,
    file_path varchar(2048) not null,
    file_name varchar(512) not null,
    extension varchar(100),
    language varchar(100) not null,
    file_type varchar(50) not null,
    size_bytes bigint not null,
    content_hash varchar(64) not null,
    content text,
    created_at timestamp with time zone not null default now(),
    constraint project_files_project_id_fk
        foreign key (project_id)
        references projects (id)
        on delete cascade
);

alter table project_files
    add column if not exists project_id bigint;

alter table project_files
    add column if not exists file_path varchar(2048);

alter table project_files
    add column if not exists file_name varchar(512);

alter table project_files
    add column if not exists extension varchar(100);

alter table project_files
    add column if not exists language varchar(100);

alter table project_files
    add column if not exists file_type varchar(50);

alter table project_files
    add column if not exists size_bytes bigint;

alter table project_files
    add column if not exists content_hash varchar(64);

alter table project_files
    add column if not exists content text;

alter table project_files
    add column if not exists created_at timestamp with time zone default now();

update project_files set language = 'Unknown' where language is null;
update project_files set file_type = 'UNKNOWN' where file_type is null;
update project_files set size_bytes = 0 where size_bytes is null;
update project_files set content_hash = repeat('0', 64) where content_hash is null;
update project_files set created_at = now() where created_at is null;

alter table project_files
    alter column project_id set not null,
    alter column file_path set not null,
    alter column file_name set not null,
    alter column language set not null,
    alter column file_type set not null,
    alter column size_bytes set not null,
    alter column content_hash set not null,
    alter column created_at set not null;

create index if not exists project_files_project_id_idx
    on project_files (project_id);

create index if not exists projects_user_id_idx
    on projects (user_id);
