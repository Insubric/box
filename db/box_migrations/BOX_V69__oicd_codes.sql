create table oidc_codes (
    state text primary key,
    challenge text not null,
    verifier text not null,
    create_at timestamp not null default now()
);