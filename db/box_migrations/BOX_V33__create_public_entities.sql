CREATE TABLE if not exists "box"."public_entities" (
    entity text primary key,
    insert boolean default false,
    update boolean default false
);