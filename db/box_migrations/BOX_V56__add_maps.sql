
create table maps (
    map_id uuid primary key default gen_random_uuid(),
    name text not null,
    parameters text[],
    srid int not null,
    x_min double precision not null,
    y_min double precision not null,
    x_max double precision not null,
    y_max double precision not null,
    max_zoom double precision not null
);

create table map_layer_vector_db (
                layer_id uuid primary key default gen_random_uuid(),
                map_id uuid not null references maps(map_id) on delete cascade on update cascade,
                entity text not null,
                field text not null,
                geometry_type text not null,
                srid int not null,
                z_index int not null,
                extra jsonb,
                editable boolean not null default false,
                query jsonb,
                autofocus boolean not null default false,
                color text not null
);

create table map_layer_wmts (
              layer_id uuid primary key default gen_random_uuid(),
              map_id uuid not null references maps(map_id) on delete cascade on update cascade,
              capabilities_url text not null,
              wmts_layer_id text not null,
              srid int not null,
              z_index int not null,
              extra jsonb
);



alter table field add column map_uuid uuid;


