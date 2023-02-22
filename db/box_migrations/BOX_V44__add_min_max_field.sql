do
$do$
    begin
        if exists (
            select from information_schema.columns
                where table_schema = 'box' and table_name='field' and column_name='min'
        ) then
            alter table field alter column min type double precision using min::double precision;
        else
            alter table field add column min double precision;
        end if;
    end
$do$;

do
$do$
    begin
        if exists (
                select from information_schema.columns
                where table_schema = 'box' and table_name='field' and column_name='max'
            ) then
            alter table field alter column max type double precision using max::double precision;
        else
            alter table field add column max double precision;
        end if;
    end
$do$;
