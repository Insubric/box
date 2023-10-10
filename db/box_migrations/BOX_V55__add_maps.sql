set search_path = box;
drop table map_layers;
drop table maps;
create table maps (
    map_id uuid primary key default gen_random_uuid(),
    name text not null,
    parameters text[],
    srid int,
    x_min double precision,
    y_min double precision,
    x_max double precision,
    y_max double precision
);

create table map_layers (
                            layer_id uuid primary key default gen_random_uuid(),
                            map_id uuid not null references maps(map_id) on delete cascade on update cascade,
                            geometry_type text not null,
                            name text not null,
                            z_index int,
                            extra jsonb,
                            editable boolean not null default false,
                            entity text,
                            query jsonb

);

