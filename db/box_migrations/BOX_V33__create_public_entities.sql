CREATE TABLE if not exists "public_entities" (
    entity text primary key,
    insert boolean default false,
    update boolean default false
);