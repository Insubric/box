CREATE OR REPLACE FUNCTION create_role_if_not_exists(p_role_name text)
    RETURNS void
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = p_role_name
    ) THEN
        EXECUTE format('CREATE ROLE %I', p_role_name);
    END IF;
END;
$$;