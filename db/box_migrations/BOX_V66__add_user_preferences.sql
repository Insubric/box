
create table user_preferences (
    username text primary key,
    preferences jsonb not null
);