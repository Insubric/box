


create function dump_grant_for_role(_schemaname text, _rolname text) returns text language sql as
$$
WITH
    target_role AS (
        -- Replace with the role you want to export, or keep as :'role_name' for psql \set variable
        SELECT _rolname AS name
    ),
    target AS (
        SELECT r.oid AS oid, r.rolname
        FROM pg_roles r
                 JOIN target_role tr ON tr.name = r.rolname
    ),
    grants AS (

        /* ===== TABLES ===== */
        SELECT
            n.nspname as schema_name,
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON TABLE ' || quote_ident(n.nspname) || '.' || quote_ident(c.relname) ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';' as grant_statement
        FROM pg_class c
                 JOIN pg_namespace n ON n.oid = c.relnamespace
                 JOIN LATERAL aclexplode(c.relacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        WHERE c.relkind IN ('r','p','v','m','f')
        GROUP BY n.nspname, c.relname

        UNION ALL

        /* ===== SEQUENCES ===== */
        SELECT
            n.nspname as schema_name,
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON SEQUENCE ' || quote_ident(n.nspname) || '.' || quote_ident(c.relname) ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_class c
                 JOIN pg_namespace n ON n.oid = c.relnamespace
                 JOIN LATERAL aclexplode(c.relacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        WHERE c.relkind = 'S'
        GROUP BY n.nspname, c.relname

        UNION ALL

        /* ===== SCHEMAS ===== */
        SELECT
            ns.nspname as schema_name,
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON SCHEMA ' || quote_ident(ns.nspname) ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_namespace ns
                 JOIN LATERAL aclexplode(ns.nspacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        GROUP BY ns.nspname

        UNION ALL

        /* ===== FUNCTIONS ===== */
        SELECT
            n.nspname as schema_name,
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON FUNCTION ' || quote_ident(n.nspname) || '.' || p.proname || '(' ||
            pg_get_function_identity_arguments(p.oid) || ')' ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_proc p
                 JOIN pg_namespace n ON n.oid = p.pronamespace
                 JOIN LATERAL aclexplode(p.proacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        GROUP BY n.nspname, p.proname, p.oid

        UNION ALL

        /* ===== DATABASES ===== */
        SELECT
            'global',
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON DATABASE ' || quote_ident(d.datname) ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_database d
                 JOIN LATERAL aclexplode(d.datacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        GROUP BY d.datname

        UNION ALL

        /* ===== LANGUAGES ===== */
        SELECT
            'global',
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON LANGUAGE ' || quote_ident(l.lanname) ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_language l
                 JOIN LATERAL aclexplode(l.lanacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        GROUP BY l.lanname

        UNION ALL

        /* ===== TYPES ===== */
        SELECT
            'global',
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON TYPE ' || quote_ident(n.nspname) || '.' || quote_ident(ti.typname) ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_type ti
                 JOIN pg_namespace n ON n.oid = ti.typnamespace
                 JOIN LATERAL aclexplode(ti.typacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        GROUP BY n.nspname, ti.typname


        UNION ALL

        /* ===== FOREIGN DATA WRAPPERS ===== */
        SELECT
            'global',
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON FOREIGN DATA WRAPPER ' || quote_ident(fdw.fdwname) ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_foreign_data_wrapper fdw
                 JOIN LATERAL aclexplode(fdw.fdwacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        GROUP BY fdw.fdwname

        UNION ALL

        /* ===== FOREIGN SERVERS ===== */
        SELECT
            'global',
            'GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON FOREIGN SERVER ' || quote_ident(s.srvname) ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_foreign_server s
                 JOIN LATERAL aclexplode(s.srvacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        GROUP BY s.srvname

        UNION ALL

        /* ===== ROLE MEMBERSHIPS ===== */
        SELECT
            'global',
            'GRANT ' || quote_ident(r.rolname) || ' TO ' || quote_ident((SELECT rolname FROM target)) ||
            CASE WHEN m.admin_option THEN ' WITH ADMIN OPTION;' ELSE ';' END
        FROM pg_auth_members m
                 JOIN pg_roles r ON r.oid = m.roleid
                 JOIN target t ON t.oid = m.member

        UNION ALL

        /* ===== DEFAULT PRIVILEGES ===== */
        SELECT
            n.nspname as schema_name,
            'ALTER DEFAULT PRIVILEGES FOR ROLE ' || quote_ident(r.rolname) ||
            COALESCE(' IN SCHEMA ' || quote_ident(n.nspname), '') ||
            ' GRANT ' || string_agg(DISTINCT a.privilege_type, ', ' ORDER BY a.privilege_type) ||
            ' ON ' ||
            CASE def.defaclobjtype
                WHEN 'r' THEN 'TABLES'
                WHEN 'S' THEN 'SEQUENCES'
                WHEN 'f' THEN 'FUNCTIONS'
                WHEN 'T' THEN 'TYPES'
                WHEN 'n' THEN 'SCHEMAS'
                END ||
            ' TO ' || quote_ident((SELECT rolname FROM target)) || ';'
        FROM pg_default_acl def
                 JOIN pg_roles r ON r.oid = def.defaclrole
                 LEFT JOIN pg_namespace n ON n.oid = def.defaclnamespace
                 JOIN LATERAL aclexplode(def.defaclacl) a ON TRUE
                 JOIN target t ON t.oid = a.grantee
        GROUP BY r.rolname, n.nspname, def.defaclobjtype
    )
SELECT string_agg(grant_statement,e'\n' order by grant_statement)
FROM grants
where schema_name=_schemaname;



$$
