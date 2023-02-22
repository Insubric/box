CREATE TABLE if not exists "cron" (
       name text NOT NULL,
       cron text NOT NULL,
       sql text NOT NULL,
       PRIMARY KEY ( name ) );