CREATE TABLE if not exists "box"."cron" (
       name text NOT NULL,
       cron text NOT NULL,
       sql text NOT NULL,
       PRIMARY KEY ( name ) );