create or replace function hasrole(rol text,_user text) returns boolean
    security definer
    language plpgsql
as
$$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from v_roles where lower(rolname) = lower(_user);
    return rol = any(roles);
END
$$;

create or replace function hasrole(rol text) returns boolean
    language plpgsql
as
$$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from v_roles where lower(rolname) = lower(current_user);
    return rol = any(roles);
END
$$;

create or replace function hasrolein(rol text[]) returns boolean
    language plpgsql
as
$$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from v_roles where lower(rolname) = lower(current_user);
    return rol && roles;   --intersection of the 2 arrays
END
$$;