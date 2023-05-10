create or replace function hasrole(rol text) returns boolean
    language plpgsql
    security definer
as
$$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from v_roles where lower(rolname) = lower(current_user);
    return rol = any(roles);
END
$$;

create function hasrolein(rol text[]) returns boolean
    language plpgsql
    security definer
as
$$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from v_roles where lower(rolname) = lower(current_user);
    return rol && roles;   --intersection of the 2 arrays
END
$$;



CREATE OR REPLACE VIEW "v_roles" AS
SELECT r.rolname,
       r.rolsuper,
       r.rolinherit,
       r.rolcreaterole,
       r.rolcreatedb,
       r.rolcanlogin,
       r.rolconnlimit,
       r.rolvaliduntil,
       ARRAY( SELECT b.rolname
              FROM (pg_auth_members m
                       JOIN pg_roles b ON ((m.roleid = b.oid)))
              WHERE (m.member = r.oid)) AS memberof,
       r.rolreplication,
       r.rolbypassrls
FROM pg_roles r
WHERE (r.rolname !~ '^pg_'::text)
ORDER BY r.rolname;