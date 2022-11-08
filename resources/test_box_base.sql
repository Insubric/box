
create schema box;

SET search_path = box;

CREATE EXTENSION if not exists citext;


ALTER SCHEMA box OWNER TO postgres;

--
-- Name: email; Type: DOMAIN; Schema: box; Owner: postgres
--

CREATE DOMAIN box.email AS box.citext
    CONSTRAINT email_check CHECK ((VALUE OPERATOR(box.~) '^([a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)?(.+ <[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*>)?$'::box.citext));


ALTER DOMAIN box.email OWNER TO postgres;

--
-- Name: add_translator_user(text, text); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.add_translator_user(_username text, _password text) RETURNS void
    LANGUAGE plpgsql
AS $$
begin
    execute format('drop role if exists %I;',_username);
    execute format('create role %I with password %L;',_username,_password);
    execute format('grant box_translator to %I;',_username);
    execute format('alter role %I login;',_username);


end;
$$;


ALTER FUNCTION box.add_translator_user(_username text, _password text) OWNER TO postgres;


CREATE FUNCTION box.hasrole(rol text) RETURNS boolean
    LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from box.v_roles where lower(rolname) = lower(current_user);
    return rol = any(roles);
END
$$;


ALTER FUNCTION box.hasrole(rol text) OWNER TO postgres;

--
-- Name: hasrole(text, text); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.hasrole(rol text, _user text) RETURNS boolean
    LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from box.v_roles where lower(rolname) = lower(_user);
    return rol = any(roles);
END
$$;


ALTER FUNCTION box.hasrole(rol text, _user text) OWNER TO postgres;

--
-- Name: hasrolein(text[]); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.hasrolein(rol text[]) RETURNS boolean
    LANGUAGE plpgsql
AS $$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from box.v_roles where lower(rolname) = lower(current_user);
    return rol && roles;   --intersection of the 2 arrays
END
$$;


ALTER FUNCTION box.hasrolein(rol text[]) OWNER TO postgres;





--
-- Name: tg_ins_case(); Type: FUNCTION; Schema: box; Owner: postgres
--


create function ui_notification(topic text, users text[], payload json) returns void
    language plpgsql
as
$$
declare
    output text := '';
begin

    output := row_to_json(
            (SELECT ColumnName FROM (SELECT topic,users,payload) AS ColumnName (topic,allowed_users,payload))
        )::text;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('ui_feedback_channel',output);
end;
$$;


ALTER FUNCTION box.ui_notification(topic text, users text[], payload json) OWNER TO postgres;

--
-- Name: ui_notification_forall(text, json); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.ui_notification_forall(topic text, payload json) RETURNS void
    LANGUAGE plpgsql
AS $$
begin
    PERFORM box.ui_notification(topic,'{"ALL_USERS"}'::text[],payload);
end;
$$;


ALTER FUNCTION box.ui_notification_forall(topic text, payload json) OWNER TO postgres;

--
-- Name: v_field_del(); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.v_field_del() RETURNS trigger
    LANGUAGE plpgsql
AS $$
begin
    delete from box.field where field_uuid = old.field_uuid;
    return old;
end;
$$;


ALTER FUNCTION box.v_field_del() OWNER TO postgres;

--
-- Name: v_field_ins(); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.v_field_ins() RETURNS trigger
    LANGUAGE plpgsql
AS $$
begin
    insert into box.field (type, name, widget, "lookupEntity", "lookupValueField", "lookupQuery", "masterFields", "childFields", "childQuery", "default", "conditionFieldId", "conditionValues", params, read_only, required, form_uuid, child_form_uuid, function) values
        (new.type, new.name, new.widget, new."lookupEntity", new."lookupValueField", new."lookupQuery", new."masterFields", new."childFields", new."childQuery", new."default", new."conditionFieldId", new."conditionValues", new.params, new.read_only, new.required, new.form_uuid, new.child_form_uuid, new.function)
    returning field_uuid into new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from box.form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;


ALTER FUNCTION box.v_field_ins() OWNER TO postgres;

--
-- Name: v_field_upd(); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.v_field_upd() RETURNS trigger
    LANGUAGE plpgsql
AS $$
begin
    update box.field
    set type = new.type,
        name = new.name,
        widget = new.widget,
        "lookupEntity" = new."lookupEntity",
        "lookupValueField" = new."lookupValueField",
        "lookupQuery" = new."lookupQuery",
        "masterFields" = new."masterFields",
        "childFields" = new."childFields",
        "childQuery" = new."childQuery",
        "default" = new."default",
        "conditionFieldId" = new."conditionFieldId",
        "conditionValues" = new."conditionValues",
        params = new.params,
        read_only = new.read_only,
        required = new.required,
        form_uuid = new.form_uuid,
        child_form_uuid = new.child_form_uuid,
        function = new.function
    where field_uuid = new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from box.form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;


ALTER FUNCTION box.v_field_upd() OWNER TO postgres;

--
-- Name: v_labels_insert(); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.v_labels_insert() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO box.labels (lang, key, label) values ('de',NEW.key,NEW.de),('fr',NEW.key,NEW.fr),('it',NEW.key,NEW.it);
    RETURN NEW;
END $$;


ALTER FUNCTION box.v_labels_insert() OWNER TO postgres;

--
-- Name: v_labels_update(); Type: FUNCTION; Schema: box; Owner: postgres
--

CREATE FUNCTION box.v_labels_update() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE box.labels SET label = NEW.de WHERE key = NEW.key and lang='de';
    UPDATE box.labels SET label = NEW.fr WHERE key = NEW.key and lang='fr';
    UPDATE box.labels SET label = NEW.it WHERE key = NEW.key and lang='it';
    RETURN NEW;
END $$;


ALTER FUNCTION box.v_labels_update() OWNER TO postgres;



SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: access_level; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.access_level (
                                  access_level_id integer NOT NULL,
                                  access_level character varying NOT NULL
);


ALTER TABLE box.access_level OWNER TO postgres;

--
-- Name: case_entry_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.case_entry_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE box.case_entry_seq OWNER TO postgres;

--
-- Name: conf; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.conf (
                          key character varying NOT NULL,
                          value character varying
);


ALTER TABLE box.conf OWNER TO postgres;

--
-- Name: conf_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.conf_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.conf_id_seq OWNER TO postgres;

--
-- Name: cron; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.cron (
                          name text NOT NULL,
                          cron text NOT NULL,
                          sql text NOT NULL
);


ALTER TABLE box.cron OWNER TO postgres;

--
-- Name: export; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.export (
                            name character varying NOT NULL,
                            function character varying NOT NULL,
                            description character varying,
                            layout character varying,
                            parameters character varying,
                            "order" double precision,
                            access_role text[],
                            export_uuid uuid DEFAULT gen_random_uuid() NOT NULL
);


ALTER TABLE box.export OWNER TO postgres;

--
-- Name: export_export_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.export_export_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.export_export_id_seq OWNER TO postgres;

--
-- Name: export_field; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.export_field (
                                  type character varying NOT NULL,
                                  name character varying NOT NULL,
                                  widget character varying,
                                  "lookupEntity" character varying,
                                  "lookupValueField" character varying,
                                  "lookupQuery" character varying,
                                  "default" character varying,
                                  "conditionFieldId" character varying,
                                  "conditionValues" character varying,
                                  field_uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                  export_uuid uuid NOT NULL
);


ALTER TABLE box.export_field OWNER TO postgres;

--
-- Name: export_field_field_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.export_field_field_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.export_field_field_id_seq OWNER TO postgres;

--
-- Name: export_field_i18n; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.export_field_i18n (
                                       lang character(2) DEFAULT NULL::bpchar,
                                       label character varying,
                                       placeholder character varying,
                                       tooltip character varying,
                                       hint character varying,
                                       "lookupTextField" character varying,
                                       uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                       field_uuid uuid NOT NULL
);


ALTER TABLE box.export_field_i18n OWNER TO postgres;

--
-- Name: export_field_i18n_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.export_field_i18n_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.export_field_i18n_id_seq OWNER TO postgres;

--
-- Name: export_header_i18n_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.export_header_i18n_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.export_header_i18n_id_seq OWNER TO postgres;

--
-- Name: export_i18n; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.export_i18n (
                                 lang character(2) DEFAULT NULL::bpchar,
                                 label character varying,
                                 tooltip character varying,
                                 hint character varying,
                                 function character varying,
                                 uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                 export_uuid uuid NOT NULL
);


ALTER TABLE box.export_i18n OWNER TO postgres;

--
-- Name: export_i18n_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.export_i18n_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.export_i18n_id_seq OWNER TO postgres;

--
-- Name: field; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.field (
                           type character varying NOT NULL,
                           name character varying NOT NULL,
                           widget character varying,
                           "lookupEntity" character varying,
                           "lookupValueField" character varying,
                           "lookupQuery" character varying,
                           "masterFields" character varying,
                           "childFields" character varying,
                           "childQuery" character varying,
                           "default" character varying,
                           "conditionFieldId" character varying,
                           "conditionValues" character varying,
                           params jsonb,
                           read_only boolean DEFAULT false NOT NULL,
                           required boolean,
                           field_uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                           form_uuid uuid NOT NULL,
                           child_form_uuid uuid,
                           function text,
                           min double precision,
                           max double precision
);


ALTER TABLE box.field OWNER TO postgres;

--
-- Name: field_field_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.field_field_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.field_field_id_seq OWNER TO postgres;

--
-- Name: field_file; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.field_file (
                                file_field character varying NOT NULL,
                                thumbnail_field character varying,
                                name_field character varying NOT NULL,
                                field_uuid uuid NOT NULL
);


ALTER TABLE box.field_file OWNER TO postgres;

--
-- Name: field_i18n; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.field_i18n (
                                lang character(2) DEFAULT NULL::bpchar,
                                label character varying,
                                placeholder character varying,
                                tooltip character varying,
                                hint character varying,
                                "lookupTextField" character varying,
                                uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                field_uuid uuid NOT NULL
);


ALTER TABLE box.field_i18n OWNER TO postgres;

--
-- Name: field_i18n_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.field_i18n_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.field_i18n_id_seq OWNER TO postgres;

--
-- Name: flyway_schema_history_box; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.flyway_schema_history_box (
                                               installed_rank integer NOT NULL,
                                               version character varying(50),
                                               description character varying(200) NOT NULL,
                                               type character varying(20) NOT NULL,
                                               script character varying(1000) NOT NULL,
                                               checksum integer,
                                               installed_by character varying(100) NOT NULL,
                                               installed_on timestamp without time zone DEFAULT now() NOT NULL,
                                               execution_time integer NOT NULL,
                                               success boolean NOT NULL
);


ALTER TABLE box.flyway_schema_history_box OWNER TO postgres;

--
-- Name: form; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.form (
                          name character varying NOT NULL,
                          entity character varying NOT NULL,
                          description character varying,
                          layout character varying,
                          "tabularFields" character varying,
                          query character varying,
                          exportfields character varying,
                          guest_user text,
                          edit_key_field text,
                          view_table text,
                          view_id text,
                          show_navigation boolean DEFAULT true NOT NULL,
                          props text,
                          form_uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                          params jsonb
);


ALTER TABLE box.form OWNER TO postgres;

--
-- Name: form_actions; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.form_actions (
                                  action text NOT NULL,
                                  importance text NOT NULL,
                                  after_action_goto text,
                                  label text NOT NULL,
                                  update_only boolean DEFAULT false NOT NULL,
                                  insert_only boolean DEFAULT false NOT NULL,
                                  reload boolean DEFAULT false NOT NULL,
                                  confirm_text text,
                                  uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                  form_uuid uuid NOT NULL,
                                  execute_function text,
                                  action_order double precision NOT NULL,
                                  condition jsonb,
                                  html_check boolean DEFAULT true NOT NULL
);


ALTER TABLE box.form_actions OWNER TO postgres;

--
-- Name: form_form_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.form_form_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.form_form_id_seq OWNER TO postgres;

--
-- Name: form_i18n; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.form_i18n (
                               lang character(2) DEFAULT NULL::bpchar,
                               label character varying,
                               view_table text,
                               dynamic_label text,
                               uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                               form_uuid uuid NOT NULL
);


ALTER TABLE box.form_i18n OWNER TO postgres;

--
-- Name: form_i18n_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.form_i18n_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.form_i18n_id_seq OWNER TO postgres;

--
-- Name: form_navigation_actions; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.form_navigation_actions (
                                             action text NOT NULL,
                                             importance text NOT NULL,
                                             after_action_goto text,
                                             label text NOT NULL,
                                             update_only boolean DEFAULT false NOT NULL,
                                             insert_only boolean DEFAULT false NOT NULL,
                                             reload boolean DEFAULT false NOT NULL,
                                             confirm_text text,
                                             execute_function text,
                                             action_order double precision NOT NULL,
                                             uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                             form_uuid uuid NOT NULL
);


ALTER TABLE box.form_navigation_actions OWNER TO postgres;

--
-- Name: function; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.function (
                              name character varying NOT NULL,
                              mode character varying DEFAULT 'table'::character varying NOT NULL,
                              function character varying NOT NULL,
                              presenter character varying,
                              description character varying,
                              layout character varying,
                              "order" double precision,
                              access_role text[],
                              function_uuid uuid DEFAULT gen_random_uuid() NOT NULL
);


ALTER TABLE box.function OWNER TO postgres;

--
-- Name: function_field; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.function_field (
                                    type character varying NOT NULL,
                                    name character varying NOT NULL,
                                    widget character varying,
                                    "lookupEntity" character varying,
                                    "lookupValueField" character varying,
                                    "lookupQuery" character varying,
                                    "default" character varying,
                                    "conditionFieldId" character varying,
                                    "conditionValues" character varying,
                                    field_uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                    function_uuid uuid NOT NULL
);


ALTER TABLE box.function_field OWNER TO postgres;

--
-- Name: function_field_field_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.function_field_field_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.function_field_field_id_seq OWNER TO postgres;

--
-- Name: function_field_i18n; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.function_field_i18n (
                                         lang character(2) DEFAULT NULL::bpchar,
                                         label character varying,
                                         placeholder character varying,
                                         tooltip character varying,
                                         hint character varying,
                                         "lookupTextField" character varying,
                                         uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                         field_uuid uuid NOT NULL
);


ALTER TABLE box.function_field_i18n OWNER TO postgres;

--
-- Name: function_field_i18n_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.function_field_i18n_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.function_field_i18n_id_seq OWNER TO postgres;

--
-- Name: function_function_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.function_function_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.function_function_id_seq OWNER TO postgres;

--
-- Name: function_i18n; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.function_i18n (
                                   lang character(2) DEFAULT NULL::bpchar,
                                   label character varying,
                                   tooltip character varying,
                                   hint character varying,
                                   function character varying,
                                   uuid uuid DEFAULT gen_random_uuid() NOT NULL,
                                   function_uuid uuid NOT NULL
);


ALTER TABLE box.function_i18n OWNER TO postgres;

--
-- Name: function_i18n_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.function_i18n_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.function_i18n_id_seq OWNER TO postgres;

--
-- Name: image_cache; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.image_cache (
                                 key text NOT NULL,
                                 data bytea NOT NULL
);


ALTER TABLE box.image_cache OWNER TO postgres;

--
-- Name: labels; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.labels (
                            lang character varying NOT NULL,
                            key character varying NOT NULL,
                            label character varying
);


ALTER TABLE box.labels OWNER TO postgres;

--
-- Name: labels_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.labels_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.labels_id_seq OWNER TO postgres;

--
-- Name: log_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.log_id_seq OWNER TO postgres;

--
-- Name: log; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.log (
                         id integer DEFAULT nextval('box.log_id_seq'::regclass) NOT NULL,
                         filename character varying NOT NULL,
                         classname character varying NOT NULL,
                         line integer NOT NULL,
                         message character varying NOT NULL,
                         "timestamp" bigint NOT NULL
);


ALTER TABLE box.log OWNER TO postgres;

--
-- Name: mails; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.mails (
                           id uuid DEFAULT gen_random_uuid() NOT NULL,
                           send_at timestamp without time zone NOT NULL,
                           sent_at timestamp without time zone,
                           mail_from text NOT NULL,
                           mail_to text[] NOT NULL,
                           subject text NOT NULL,
                           html text NOT NULL,
                           text text,
                           params jsonb,
                           created timestamp without time zone NOT NULL,
                           wished_send_at timestamp without time zone NOT NULL,
                           mail_cc text[] DEFAULT ARRAY[]::text[] NOT NULL,
                           mail_bcc text[] DEFAULT ARRAY[]::text[] NOT NULL
);


ALTER TABLE box.mails OWNER TO postgres;

--
-- Name: news; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.news (
                          datetime timestamp without time zone DEFAULT now() NOT NULL,
                          author character varying(2000),
                          news_uuid uuid DEFAULT gen_random_uuid() NOT NULL
);


ALTER TABLE box.news OWNER TO postgres;

--
-- Name: news_i18n; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.news_i18n (
                               lang character varying(2) NOT NULL,
                               text text NOT NULL,
                               title text,
                               news_uuid uuid NOT NULL
);


ALTER TABLE box.news_i18n OWNER TO postgres;

--
-- Name: public_entities; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.public_entities (
                                     entity text NOT NULL,
                                     insert boolean DEFAULT false,
                                     update boolean DEFAULT false
);


ALTER TABLE box.public_entities OWNER TO postgres;

--
-- Name: ui; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.ui (
                        key character varying NOT NULL,
                        value character varying NOT NULL,
                        access_level_id integer NOT NULL
);


ALTER TABLE box.ui OWNER TO postgres;

--
-- Name: ui_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.ui_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.ui_id_seq OWNER TO postgres;

--
-- Name: ui_src; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.ui_src (
                            file bytea,
                            mime character varying,
                            name character varying,
                            access_level_id integer NOT NULL,
                            uuid uuid DEFAULT gen_random_uuid() NOT NULL
);


ALTER TABLE box.ui_src OWNER TO postgres;

--
-- Name: ui_src_id_seq; Type: SEQUENCE; Schema: box; Owner: postgres
--

CREATE SEQUENCE box.ui_src_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE box.ui_src_id_seq OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: box; Owner: postgres
--

CREATE TABLE box.users (
                           username character varying NOT NULL,
                           access_level_id integer NOT NULL
);


ALTER TABLE box.users OWNER TO postgres;

--
-- Name: v_field; Type: VIEW; Schema: box; Owner: postgres
--

CREATE VIEW box.v_field AS
SELECT fi.type,
       fi.name,
       fi.widget,
       fi."lookupEntity",
       fi."lookupValueField",
       fi."lookupQuery",
       fi."masterFields",
       fi."childFields",
       fi."childQuery",
       fi."default",
       fi."conditionFieldId",
       fi."conditionValues",
       fi.params,
       fi.read_only,
       fi.required,
       fi.field_uuid,
       fi.form_uuid,
       fi.child_form_uuid,
       fi.function,
       ( SELECT (count(*) > 0)
         FROM information_schema.columns
         WHERE (((columns.table_name)::name = (f.entity)::text) AND ((columns.column_name)::name = (fi.name)::text))) AS entity_field
FROM (box.field fi
    LEFT JOIN box.form f ON ((fi.form_uuid = f.form_uuid)));


ALTER TABLE box.v_field OWNER TO postgres;

--
-- Name: v_labels; Type: VIEW; Schema: box; Owner: postgres
--

CREATE VIEW box.v_labels AS
WITH keys AS (
    SELECT DISTINCT labels.key
    FROM box.labels
)
SELECT keys.key,
       de.label AS de,
       fr.label AS fr,
       it.label AS it
FROM (((keys
    LEFT JOIN box.labels de ON ((((keys.key)::text = (de.key)::text) AND ((de.lang)::text = 'de'::text))))
    LEFT JOIN box.labels fr ON ((((keys.key)::text = (fr.key)::text) AND ((fr.lang)::text = 'fr'::text))))
    LEFT JOIN box.labels it ON ((((keys.key)::text = (it.key)::text) AND ((it.lang)::text = 'it'::text))));


ALTER TABLE box.v_labels OWNER TO postgres;

--
-- Name: v_roles; Type: VIEW; Schema: box; Owner: postgres
--

CREATE VIEW box.v_roles AS
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


ALTER TABLE box.v_roles OWNER TO postgres;

--
-- Data for Name: access_level; Type: TABLE DATA; Schema: box; Owner: postgres
--

insert into box.access_level (access_level_id, access_level)
values  (-1, 'Not logged user'),
        (1, 'Read-Only User'),
        (1000, 'Administrator');


--
-- Data for Name: conf; Type: TABLE DATA; Schema: box; Owner: postgres
--

insert into box.conf (key, value)
values  ('host', '0.0.0.0'),
        ('server-secret', 'changeMe-sadf-09fd65465653445se554d6554d65r54d65r54d65r546d5r5dasdfiasdf897sdf-as-s9d8fd9f8s09fku'),
        ('cookie.name', '_boxsession_myapp'),
        ('pks.edit', 'false'),
        ('fks.lookup.labels', 'default = firstNoPKField'),
        ('fks.lookup.rowsLimit', '50'),
        ('display.index.news', 'false'),
        ('filter.precision.double', '1'),
        ('page.length', '30'),
        ('notification.timeout', '6'),
        ('cache.enable', 'false'),
        ('color.danger', '#C54E13'),
        ('color.warning', '#EB883E'),
        ('color.main', '#006268'),
        ('color.link', '#fdf5c9'),
        ('display.index.html', 'true'),
        ('filter.precision.datetime', 'DATE'),
        ('langs', 'it,en'),
        ('port', '8080'),
        ('max-age', '100000'),
        ('logger.level', 'warn'),
        ('form.requiredFontSize', '16'),
        ('map.options', '{
    "features": {
        "point": true,
        "multiPoint": true,
        "line": false,
        "multiLine": false,
        "polygon": true,
        "multiPolygon": true,
        "geometryCollection": false
    },
    "projections": [
        {
            "name": "EPSG:21781",
            "proj": "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=600000 +y_0=200000 +ellps=bessel +towgs84=674.4,15.1,405.3,0,0,0,0 +units=m +no_defs",
            "unit": "m",
            "extent": [
                485071.54,
                75346.36,
                828515.78,
                299941.84
            ]
        },
        {
            "name": "EPSG:2056",
            "proj": "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=2600000 +y_0=1200000 +ellps=bessel +towgs84=674.374,15.056,405.346,0,0,0,0 +units=m +no_defs ",
            "unit": "m",
            "extent": [
                2485071.58,
                1075346.31,
                2828515.82,
                1299941.79
            ]
        }
    ],
    "defaultProjection": "EPSG:21781",
    "baseLayers": [
        {
            "name": "Swisstopo",
            "capabilitiesUrl": "https://wmts.geo.admin.ch/EPSG/21781/1.0.0/WMTSCapabilities.xml",
            "layerId": "ch.swisstopo.pixelkarte-farbe"
        },
        {
            "name": "SwissImage",
            "capabilitiesUrl": "https://wmts.geo.admin.ch/EPSG/21781/1.0.0/WMTSCapabilities.xml",
            "layerId": "ch.swisstopo.swissimage"
         }
     ],
     "precision": 0.01
}');

insert into box.flyway_schema_history_box (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
values  (1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', null, 'null', '2020-09-02 14:27:01.419007', 0, true),
        (2, '2', 'Add view form', 'SQL', 'BOX_V2__Add_view_form.sql', 1948859857, 'postgres', '2020-09-02 14:37:31.474133', 21, true),
        (3, '3', 'Move table view in i18n', 'SQL', 'BOX_V3__Move_table_view_in_i18n.sql', 2140385301, 'postgres', '2020-09-02 15:25:49.960194', 26, true),
        (4, '4', 'Add dynamic field params', 'SQL', 'BOX_V4__Add_dynamic_field_params.sql', 356082386, 'postgres', '2020-09-15 09:45:39.260415', 16, true),
        (5, '5', 'Add form actions', 'SQL', 'BOX_V5__Add_form_actions.sql', -1699988107, 'postgres', '2020-09-25 16:50:31.612004', 49, true),
        (6, '6', 'Add guest user', 'SQL', 'BOX_V6__Add_guest_user.sql', 535455469, 'postgres', '2020-09-28 19:08:23.172173', 33, true),
        (7, '07', 'Add field read only', 'SQL', 'BOX_V07__Add_field_read_only.sql', 1239925726, 'postgres', '2020-10-15 12:29:26.736576', 12, true),
        (8, '08', 'Add edit key fields', 'SQL', 'BOX_V08__Add_edit_key_fields.sql', 810939949, 'postgres', '2020-11-02 16:50:17.000035', 17, true),
        (9, '09', 'Ui notification functions', 'SQL', 'BOX_V09__Ui_notification_functions.sql', 1202179201, 'postgres', '2020-12-07 14:23:02.976774', 18, true),
        (10, '10', 'Add linked label fields', 'SQL', 'BOX_V10__Add_linked_label_fields.sql', 1854433744, 'postgres', '2020-12-16 10:33:02.269251', 15, true),
        (11, '11', 'Add news table', 'SQL', 'BOX_V11__Add_news_table.sql', -1459896838, 'postgres', '2020-12-30 18:38:43.022193', 55, true),
        (12, '12', 'Add image cache', 'SQL', 'BOX_V12__Add_image_cache.sql', 118027770, 'postgres', '2021-01-03 03:00:38.388169', 12, true),
        (13, '13', 'send mail function', 'SQL', 'BOX_V13__send_mail_function.sql', -767241768, 'postgres', '2021-02-01 14:18:31.744082', 10, true),
        (14, '14', 'add cron', 'SQL', 'BOX_V14__add_cron.sql', -863512071, 'postgres', '2021-02-01 14:18:31.774440', 6, true),
        (15, '15', 'fix field widget', 'SQL', 'BOX_V15__fix_field_widget.sql', 925596291, 'postgres', '2021-02-16 11:31:05.331319', 18, true),
        (16, '16', 'remove useless field', 'SQL', 'BOX_V16__remove_useless_field.sql', 1897350339, 'postgres', '2021-02-16 11:31:05.364141', 7, true),
        (17, '18', 'refactor widgets', 'SQL', 'BOX_V18__refactor_widgets.sql', -1382705248, 'postgres', '2021-02-16 11:31:05.387033', 10, true),
        (18, '19', 'label natural key', 'SQL', 'BOX_V19__label_natural_key.sql', -1527674366, 'postgres', '2021-02-22 15:23:47.251354', 39, true),
        (19, '20', 'remove linked label fields', 'SQL', 'BOX_V20__remove_linked_label_fields.sql', 2031478406, 'postgres', '2021-02-24 18:03:52.887008', 28, true),
        (20, '21', 'conf natural key', 'SQL', 'BOX_V21__conf_natural_key.sql', -1312096307, 'postgres', '2021-02-24 18:03:52.936127', 23, true),
        (21, '22', 'remove static content', 'SQL', 'BOX_V22__remove_static_content.sql', -1842450304, 'postgres', '2021-03-17 09:12:53.833950', 13, true),
        (22, '23', 'add show navigation', 'SQL', 'BOX_V23__add_show_navigation.sql', -861760598, 'postgres', '2021-03-17 09:12:53.864411', 7, true),
        (23, '24', 'add form i18n dynamic field', 'SQL', 'BOX_V24__add_form_i18n_dynamic_field.sql', -941527902, 'postgres', '2021-04-20 06:46:30.425278', 10, true),
        (24, '25', 'add child props', 'SQL', 'BOX_V25__add_child_props.sql', -328731711, 'postgres', '2021-04-20 06:46:30.452934', 5, true),
        (25, '26', 'add required field', 'SQL', 'BOX_V26__add_required_field.sql', 2024820560, 'postgres', '2021-04-20 06:46:30.472543', 4, true),
        (27, '28', 'mail transactional', 'SQL', 'BOX_V28__mail_transactional.sql', 668796536, 'postgres', '2021-05-10 08:27:06.519894', 56, true),
        (28, '29', 'add function to field', 'SQL', 'BOX_V29__add_function_to_field.sql', 832974588, 'postgres', '2021-07-02 09:09:51.991832', 10, true),
        (29, '30', 'add params to form', 'SQL', 'BOX_V30__add_params_to_form.sql', -2078533563, 'postgres', '2021-07-23 08:37:56.227490', 17, true),
        (30, '31', 'mail notification with feedback', 'SQL', 'BOX_V31__mail_notification_with_feedback.sql', 143753309, 'postgres', '2021-09-09 22:26:51.311079', 47, true),
        (31, '32', 'more mailing functions', 'SQL', 'BOX_V32__more_mailing_functions.sql', 1955273899, 'postgres', '2021-09-15 07:25:43.165266', 20, true),
        (32, '33', 'create public entities', 'SQL', 'BOX_V33__create_public_entities.sql', 1926177993, 'postgres', '2021-10-08 09:59:18.282413', 67, true),
        (33, '35', 'generic has role', 'SQL', 'BOX_V35__generic_has_role.sql', 1190293949, 'postgres', '2021-11-01 09:31:21.624484', 19, true),
        (34, '36', 'action review', 'SQL', 'BOX_V36__action_review.sql', -361744506, 'postgres', '2021-11-01 09:31:21.673955', 71, true),
        (35, '37', 'entity field', 'SQL', 'BOX_V37__entity_field.sql', 40250892, 'postgres', '2021-12-08 23:13:25.601343', 39, true),
        (36, '38', 'action conditional', 'SQL', 'BOX_V38__action_conditional.sql', 1004747907, 'postgres', '2021-12-17 15:07:04.798990', 11, true),
        (37, '39', 'action check', 'SQL', 'BOX_V39__action_check.sql', -654725261, 'postgres', '2021-12-17 15:07:04.820177', 6, true),
        (26, '27', 'use uuid', 'SQL', 'BOX_V27__use_uuid.sql', -495052968, 'postgres', '2021-05-10 08:27:05.693753', 766, true),
        (38, '40', 'add mail domain', 'SQL', 'BOX_V40__add_mail_domain.sql', 1585907546, 'postgres', '2022-01-14 15:30:17.025606', 96, true),
        (39, '41', 'merge box headers', 'SQL', 'BOX_V41__merge_box_headers.sql', 1832309564, 'postgres', '2022-01-31 16:28:14.990122', 19, true),
        (40, '42', 'box translator user', 'SQL', 'BOX_V42__box_translator_user.sql', 1680420515, 'postgres', '2022-04-04 11:55:23.706153', 27, true),
        (41, '43', 'Add CC BCC', 'SQL', 'BOX_V43__Add_CC_BCC.sql', -1055045347, 'postgres', '2022-07-13 13:12:01.996730', 45, true),
        (42, '44', 'add min max field', 'SQL', 'BOX_V44__add_min_max_field.sql', 955387933, 'postgres', '2022-10-06 14:25:22.385940', 7, true);

insert into box.labels (lang, key, label)
values  ('en', 'entity.duplicate', 'Copy'),
        ('it', 'entity.duplicate', 'Duplica'),
        ('fr', 'survey.no_data', 'Vous avez rapporté un organisme / dégât sous «Présence » - veuillez également remplir la distribution  et la gravité des dégâts.'),
        ('de', 'entity.duplicate', 'Kopie'),
        ('en', 'entity.new', 'New'),
        ('it', 'entity.new', 'Nuovo'),
        ('fr', 'survey.back', 'Retour'),
        ('de', 'entity.new', 'Neu'),
        ('en', 'entity.search', 'Search'),
        ('it', 'entity.search', 'Cerca'),
        ('de', 'entity.search', 'Suchen'),
        ('en', 'entity.select', 'Select'),
        ('it', 'entity.select', 'Seleziona'),
        ('de', 'entity.select', 'Auswählen'),
        ('en', 'entity.table', 'Table'),
        ('it', 'entity.table', 'Tabella'),
        ('de', 'entity.table', 'Tabelle'),
        ('en', 'entity.title', 'Tables/Views'),
        ('it', 'entity.title', 'Tabelle/Views'),
        ('fr', 'entity.select', 'Sélectionner'),
        ('de', 'entity.title', 'Tabellen/Views'),
        ('en', 'error.notfound', 'URL not found!'),
        ('it', 'error.notfound', 'URL non trovato!'),
        ('de', 'error.notfound', 'URL nicht gefunden!'),
        ('en', 'exports.search', 'Search statistics'),
        ('it', 'exports.search', 'Cerca statistica'),
        ('de', 'exports.search', 'Statistik suchen'),
        ('en', 'exports.select', 'Select a statistic'),
        ('it', 'exports.select', 'Scegliere una statistica'),
        ('fr', 'login.username', 'Nom d''utilisateur'),
        ('de', 'exports.select', 'Statistik auswählen'),
        ('en', 'exports.title', 'Statistics'),
        ('it', 'exports.title', 'Statistiche'),
        ('fr', 'exports.title', 'Statistiques'),
        ('de', 'exports.title', 'Statistiken'),
        ('en', 'form.changed', 'Data changed'),
        ('it', 'form.changed', 'Dati modificati'),
        ('de', 'form.changed', 'Daten abgeändert'),
        ('en', 'form.save', 'Save'),
        ('it', 'form.save', 'Salva'),
        ('fr', 'header.exports', 'Statistiques'),
        ('de', 'form.save', 'Speichern'),
        ('en', 'form.save_add', 'Save and insert next'),
        ('it', 'form.save_add', 'Salva e aggiungi'),
        ('de', 'form.save_add', 'Speichern und einfügen'),
        ('en', 'form.save_table', 'Save and back to table'),
        ('it', 'form.save_table', 'Salva e ritorna alla tabella'),
        ('fr', 'form.changed', 'Données modifiées'),
        ('de', 'form.save_table', 'Speichern und zurück zur Tabelle'),
        ('en', 'header.entities', 'Entities'),
        ('it', 'header.entities', 'Entità'),
        ('de', 'header.entities', 'Entität'),
        ('en', 'header.exports', 'Exports'),
        ('it', 'header.exports', 'Statistiche'),
        ('de', 'header.exports', 'Statistiken'),
        ('en', 'header.forms', 'Forms'),
        ('it', 'header.forms', 'Maschere'),
        ('fr', 'header.entities', 'Entité'),
        ('de', 'header.forms', 'Masken'),
        ('en', 'header.functions', 'Functions'),
        ('it', 'header.functions', 'Funzioni'),
        ('de', 'header.functions', 'Funktionen'),
        ('en', 'header.home', 'Home'),
        ('it', 'header.home', 'Home'),
        ('de', 'header.home', 'Home'),
        ('en', 'header.lang', 'Language'),
        ('it', 'header.lang', 'Lingua'),
        ('de', 'header.lang', 'Sprache'),
        ('en', 'header.tables', 'Tables'),
        ('it', 'header.tables', 'Tabelle'),
        ('de', 'header.tables', 'Tabellen'),
        ('en', 'header.views', 'Views'),
        ('it', 'header.views', 'Views'),
        ('de', 'header.views', 'Views'),
        ('en', 'login.button', 'Login'),
        ('it', 'login.button', 'Login'),
        ('de', 'login.button', 'Login'),
        ('en', 'login.failed', 'Login failed'),
        ('it', 'login.failed', 'Login fallito'),
        ('de', 'login.failed', 'Fehlgeschlagene Anmeldung'),
        ('en', 'login.password', 'Password'),
        ('it', 'login.password', 'Password'),
        ('de', 'login.password', 'Password'),
        ('en', 'login.title', 'Sign In'),
        ('it', 'login.title', 'Sign in'),
        ('de', 'login.title', 'Sign in'),
        ('en', 'login.username', 'Username'),
        ('it', 'login.username', 'Nome utente'),
        ('de', 'login.username', 'Benutzername'),
        ('en', 'message.confirm', 'Are you sure?'),
        ('it', 'message.confirm', 'Sei sicuro?'),
        ('de', 'message.confirm', 'Sind Sie sicher?'),
        ('en', 'messages.confirm', 'Do you really want to delete?'),
        ('it', 'messages.confirm', 'Vuoi veramente cancellare?'),
        ('de', 'messages.confirm', 'Möchten Sie wirklich löschen?'),
        ('en', 'navigation.first', '◅'),
        ('it', 'navigation.first', '◅'),
        ('de', 'navigation.first', '◅'),
        ('en', 'navigation.goAway', 'Leave page without saving changes?'),
        ('it', 'navigation.goAway', 'Vuoi lasciare la pagina senza salvare i cambiamenti?'),
        ('de', 'navigation.goAway', 'Seite verlassen ohne zu speichern?'),
        ('en', 'navigation.last', '▻'),
        ('it', 'navigation.last', '▻'),
        ('de', 'navigation.last', '▻'),
        ('en', 'navigation.loading', 'Loading'),
        ('it', 'navigation.loading', 'Caricamento'),
        ('de', 'navigation.loading', 'Aktualisierung'),
        ('en', 'navigation.next', '►'),
        ('it', 'navigation.next', '►'),
        ('fr', 'sort.ignore', '-'),
        ('de', 'navigation.next', '►'),
        ('en', 'navigation.of', 'of'),
        ('it', 'navigation.of', 'di'),
        ('fr', 'ui.index.title', '<h1>BOX database</h1> Bienvenue a BOX.'),
        ('de', 'navigation.of', 'von'),
        ('en', 'navigation.page', 'Page'),
        ('it', 'navigation.page', 'Pagina'),
        ('de', 'navigation.page', 'Seite'),
        ('en', 'navigation.previous', '◄'),
        ('it', 'navigation.previous', '◄'),
        ('de', 'navigation.previous', '◄'),
        ('en', 'navigation.record', 'Record'),
        ('it', 'navigation.record', 'Evento'),
        ('fr', 'table.actions', 'Actions'),
        ('en', 'navigation.recordFound', 'Found records'),
        ('it', 'navigation.recordFound', 'Righe trovate'),
        ('fr', 'navigation.loading', 'Actualisation'),
        ('de', 'navigation.recordFound', 'Gefundene Zeilen'),
        ('en', 'sort.asc', '▼'),
        ('it', 'sort.asc', '▼'),
        ('de', 'sort.asc', '▼'),
        ('en', 'sort.desc', '▲'),
        ('it', 'sort.desc', '▲'),
        ('de', 'sort.desc', '▲'),
        ('en', 'subform.add', 'Add'),
        ('it', 'subform.add', 'Aggiungi'),
        ('de', 'subform.add', 'Einfügen'),
        ('en', 'subform.remove', 'Remove'),
        ('it', 'subform.remove', 'Rimuovi'),
        ('fr', 'exports.csv', 'Exporter CSV'),
        ('de', 'subform.remove', 'Löschen'),
        ('en', 'table.actions', 'Actions'),
        ('it', 'table.actions', 'Azioni'),
        ('de', 'table.actions', 'Aktionen'),
        ('en', 'table.csv', 'Download CSV'),
        ('it', 'table.csv', 'Scarica CSV'),
        ('de', 'table.csv', 'CSV herunterladen'),
        ('en', 'table.confirmDelete', 'Do you really want to delete the record?'),
        ('it', 'table.confirmDelete', 'Vuole veramente cancellare i dati?'),
        ('de', 'table.confirmDelete', 'Möchten Sie den Datensatz wirklich löschen?'),
        ('en', 'table.confirmRevert', 'Do you really want to discard the changes?'),
        ('it', 'table.confirmRevert', 'Vuole veramente annullare i cambiamenti?'),
        ('de', 'table.confirmRevert', 'Möchten Sie die Änderungen wirklich verwerfen?'),
        ('en', 'table.delete', 'Delete'),
        ('it', 'table.delete', 'Elimina'),
        ('fr', 'table.delete', 'Supprimer'),
        ('de', 'table.delete', 'Löschen'),
        ('en', 'table.edit', 'Edit'),
        ('it', 'table.edit', 'Modifica'),
        ('fr', 'table.edit', 'Modifier'),
        ('de', 'table.edit', 'Editieren'),
        ('en', 'table.no_action', 'No action'),
        ('it', 'table.no_action', 'Nessuna azione'),
        ('fr', 'exports.load', 'Exporter'),
        ('de', 'table.no_action', 'Keine Aktion'),
        ('en', 'table.show', 'Show'),
        ('it', 'table.show', 'Mostrare'),
        ('fr', 'exports.html', 'Exporter HTML'),
        ('de', 'table.show', 'Anzeigen'),
        ('en', 'ui.index.title', '<h1>BOX database</h1> Welcome to BOX.'),
        ('it', 'ui.index.title', '<h1>BOX database</h1> Benvenuti a BOX'),
        ('de', 'ui.index.title', '<h1>BOX database</h1> Wilkommen zur BOX.      |'),
        ('de', 'form.add_date', null),
        ('fr', 'form.add_date', null),
        ('it', 'form.add_date', null),
        ('en', 'form.add_date', null),
        ('de', 'form.remove_date', null),
        ('fr', 'form.remove_date', null),
        ('it', 'form.remove_date', null),
        ('en', 'form.remove_date', null),
        ('de', 'form.remove-map', null),
        ('fr', 'form.remove-map', null),
        ('it', 'form.remove-map', null),
        ('en', 'form.remove-map', null),
        ('de', 'form.remove-image', null),
        ('fr', 'form.remove-image', null),
        ('it', 'form.remove-image', null),
        ('en', 'form.remove-image', null),
        ('fr', 'error.session_expired', null),
        ('it', 'error.session_expired', 'Sessione scaduta'),
        ('en', 'error.session_expired', 'Session expired'),
        ('it', 'exports.csv', 'Export CSV'),
        ('en', 'exports.csv', 'Export CSV'),
        ('it', 'exports.html', 'Export HTML'),
        ('en', 'exports.html', 'Export HTML'),
        ('it', 'exports.load', 'Export'),
        ('en', 'exports.load', 'Export'),
        ('fr', 'exports.pdf', null),
        ('it', 'exports.pdf', 'Export PDF'),
        ('en', 'exports.pdf', 'Export PDF'),
        ('fr', 'exports.shp', null),
        ('it', 'exports.shp', 'Export SHP'),
        ('en', 'exports.shp', 'Export SHP'),
        ('fr', 'login.chose_lang', null),
        ('it', 'login.chose_lang', 'Seleziona lingua'),
        ('en', 'login.chose_lang', 'Select language'),
        ('fr', 'lookup.not_found', null),
        ('it', 'lookup.not_found', 'Non trovato'),
        ('en', 'lookup.not_found', 'Not found'),
        ('fr', 'popup.close', null),
        ('it', 'popup.close', 'Chiudi'),
        ('en', 'popup.close', 'Close'),
        ('fr', 'popup.search', null),
        ('it', 'popup.search', 'Cerca'),
        ('en', 'popup.search', 'Search'),
        ('de', 'login.chose_lang', 'Sprache auswählen'),
        ('de', 'lookup.not_found', ' Nicht gefunden'),
        ('de', 'navigation.record', 'Record'),
        ('de', 'popup.close', 'Schließen'),
        ('it', 'sort.ignore', '-'),
        ('en', 'sort.ignore', '-'),
        ('fr', 'table.filter.equals', '='),
        ('fr', 'table.filter.between', null),
        ('it', 'table.filter.between', 'Tra'),
        ('en', 'table.filter.between', 'Between'),
        ('fr', 'form.print', 'Imprimer'),
        ('it', 'table.filter.contains', 'Contiene'),
        ('en', 'table.filter.contains', 'Contains'),
        ('de', 'table.filter.equals', '='),
        ('it', 'table.filter.equals', '='),
        ('en', 'table.filter.equals', '='),
        ('fr', 'form.insert.mail.mismatch', 'Adresses courriels non identiques'),
        ('it', 'table.filter.gt', 'Maggiore'),
        ('en', 'table.filter.gt', 'Greater'),
        ('fr', 'ui.map.edit', 'Editer'),
        ('it', 'table.filter.gte', 'Maggiore o uguale'),
        ('en', 'table.filter.gte', 'Greater or equals'),
        ('fr', 'ui.map.goToGPS', 'Votre position (GPS)'),
        ('fr', 'table.filter.in', null),
        ('it', 'table.filter.in', 'In'),
        ('en', 'table.filter.in', 'In'),
        ('fr', 'table.filter.lt', null),
        ('it', 'table.filter.lt', 'Minore'),
        ('en', 'table.filter.lt', 'Less'),
        ('fr', 'validation.any_result', 'Un cas ne peut être marqué comme „terminé“ uniquement si il contient un résultat. Veuillez indiquer un organisme/dommage dans la liste proposée. - Si l''organisme n''est pas encore dans la liste, choisissez "Autre" et insèrez l''organisme avec son nom en français et en latin dans le champ qui s''ouvre. - Si vous n’avez pas de résultat, sélectionnez "Pas de résultat" - Si vous avez une suspicion de quarantaine, mais que celle-ci est négative et que vous n’avez pas d''autre dommage/résultat, sélectionnez "Négatif / non présent" dans la liste'),
        ('fr', 'table.filter.lte', null),
        ('it', 'table.filter.lte', 'Minore o uguale'),
        ('en', 'table.filter.lte', 'Less or equals'),
        ('fr', 'table.filter.none', null),
        ('it', 'table.filter.none', 'Nessuno'),
        ('en', 'table.filter.none', 'None'),
        ('fr', 'table.filter.not', null),
        ('it', 'table.filter.not', 'Non'),
        ('en', 'table.filter.not', 'Not'),
        ('fr', 'table.filter.notin', null),
        ('it', 'table.filter.notin', 'Non in'),
        ('en', 'table.filter.notin', 'Not in'),
        ('it', 'table.filters', 'Filtri'),
        ('en', 'table.filters', 'Filters'),
        ('fr', 'table.filter.without', null),
        ('it', 'table.filter.without', 'Senza'),
        ('en', 'table.filter.without', 'Without'),
        ('fr', 'table.revert', null),
        ('it', 'table.revert', 'Ripristina'),
        ('en', 'table.revert', 'Revert'),
        ('fr', 'table.xls', null),
        ('it', 'table.xls', 'Scarica XLS'),
        ('en', 'table.xls', 'Download XLS'),
        ('en', 'form.print', null),
        ('it', 'form.print', null),
        ('de', 'ui.map.addLine', null),
        ('de', 'ui.map.addPoint', 'Einen Punkt setzen'),
        ('de', 'ui.map.addPolygon', 'Ein Polygon zeichnen'),
        ('de', 'ui.map.goTo', 'Koordinaten eingeben'),
        ('de', 'ui.map.panZoom', 'Karte bewegen'),
        ('de', 'ui.map.move', 'Punkt / Polygon bewegen'),
        ('de', 'ui.map.delete', 'Löschen'),
        ('de', 'ui.map.edit', 'Editieren'),
        ('de', 'form.insert.mail.mismatch', 'E-Mail-Adressen nicht identisch'),
        ('de', 'ui.map.insertPointGPS', null),
        ('de', 'user.form.save', 'Senden'),
        ('de', 'ui.map.goToGPS', 'Eigene Position'),
        ('de', 'ui.map.insertPoint', 'Koordinaten eingeben'),
        ('de', 'ui.map.drawOnMap', 'Einen Polygon auf der Karte zeichnen'),
        ('de', 'ui.map.addPolygonHole', ' '),
        ('de', 'ui.map.drawOrEnter', 'Einen Punkt auf der Karte setzen oder die Koordinaten unten eingeben'),
        ('it', 'form.insert.mail.mismatch', 'Indirizzi E-Mail non uguali'),
        ('de', 'ui.map.addPointGPS', 'Einen Punkt setzen (GPS/Mobile Geräte)'),
        ('it', 'ui.map.goTo', 'Vai a (inserisci coordinate)'),
        ('it', 'ui.map.addPoint', 'Inserisci punto'),
        ('it', 'ui.map.addPointGPS', 'Inserisci punto (GPS/Mobile)'),
        ('it', 'ui.map.addPolygon', 'Inserisci poligono'),
        ('it', 'ui.map.drawOrEnter', 'Disegna punto sulla mappa o inserisci sotto le coordinate'),
        ('it', 'ui.map.insertPoint', 'Inserisci coordinate'),
        ('it', 'ui.map.drawOnMap', 'Disegna sulla mappa'),
        ('it', 'ui.map.move', 'Sposta'),
        ('it', 'ui.map.edit', 'Modifica'),
        ('it', 'ui.map.addPolygonHole', 'Inserisci buco'),
        ('it', 'user.form.save', 'Invia'),
        ('it', 'ui.map.goToGPS', 'Posizione attuale da GPS'),
        ('fr', 'ui.map.goTo', 'Entrez les coordonnées'),
        ('fr', 'ui.map.delete', 'Effacer'),
        ('fr', 'ui.map.insertPoint', 'Entrez les coordonnées'),
        ('fr', 'table.filters', 'FIltre'),
        ('fr', 'table.filter.gt', 'Plus grand'),
        ('fr', 'table.filter.gte', 'Plus grand ou égal'),
        ('de', 'exports.pdf', 'PDF exportieren'),
        ('de', 'exports.shp', 'SHP exportieren'),
        ('de', 'error.session_expired', 'Session abgelaufen'),
        ('de', 'exports.html', 'HTML exportieren'),
        ('de', 'exports.csv', 'CSV exportieren'),
        ('de', 'exports.load', 'Exportieren'),
        ('de', 'table.revert', 'Zurückkehren'),
        ('de', 'table.filter.notin', 'Nicht in'),
        ('de', 'table.filter.not', 'Nicht'),
        ('de', 'table.filter.none', 'Keine'),
        ('de', 'table.xls', 'XLS herunterladen'),
        ('de', 'table.filter.without', 'Ohne'),
        ('de', 'table.filters', 'Filter'),
        ('de', 'form.back', 'Zurück'),
        ('de', 'form.print', 'Drucken'),
        ('de', 'table.filter.lt', 'Less'),
        ('de', 'table.filter.between', 'Zwischen'),
        ('de', 'table.filter.gte', 'Größer oder gleich'),
        ('de', 'table.filter.lte', 'Less oder gleich'),
        ('de', 'table.filter.gt', 'Größer'),
        ('de', 'popup.search', 'Suche'),
        ('de', 'table.filter.contains', 'Enthält'),
        ('de', 'table.filter.in', 'In'),
        ('en', 'form.required', '*'),
        ('it', 'form.required', '*'),
        ('de', 'form.required', '*'),
        ('de', 'sort.ignore', '-'),
        ('de', 'form.drop', null),
        ('fr', 'form.drop', null),
        ('it', 'form.drop', null),
        ('en', 'form.drop', null),
        ('de', 'validation.suspiction_no_result', 'Du hast einen Quarantäneverdacht angegeben. Bitte gebe an, ob dieser verdacht positiv oder negativ ist.'),
        ('de', 'validation.any_result', 'Ein Fall kann nur mit einem Resultat als “Abgeschlossen” markiert werden. Bitte gib einen Organismus/Schaden aus der vorgegeben Liste an.

   • Wenn der Organismus noch nicht auf der Liste steht, wähle bitte “Andere” und füge den Organismus mit deutschem und lateinischem Namen in das sich öffnende Feld ein.

   • Wenn du kein Resultat hast, wähle bitte “Kein Resultat”

   • Wenn du einen Quarantäneverdacht hast, dieser aber negativ ist und du sonst keinen anderen Schaden/Resultat hast, wähle “Negativ / nicht präsent” aus der Liste aus'),
        ('de', 'validation.closed_case', 'Um den Fall rückwirkend zu bearbeiten, bitte zuerst den Status auf "In Bearbeitung" setzen. Vielen Dank.'),
        ('fr', 'form.required', '*'),
        ('de', 'form.trueLabel', null),
        ('de', 'form.falseLabel', null),
        ('fr', 'ui.map.addLine', null),
        ('fr', 'ui.map.insertPointGPS', null),
        ('fr', 'ui.map.addPolygonHole', null),
        ('fr', 'form.back', null),
        ('fr', 'validation.closed_case', null),
        ('fr', 'validation.suspiction_no_result', 'Vous avez mentionné uns suspicion d''organisme de quarantaine. Veuillez svpl indiquer si le cas est positif ou négatif.'),
        ('fr', 'form.trueLabel', null),
        ('fr', 'form.falseLabel', null),
        ('it', 'ui.map.addLine', null),
        ('it', 'ui.map.panZoom', null),
        ('it', 'ui.map.delete', null),
        ('it', 'ui.map.insertPointGPS', null),
        ('it', 'form.back', null),
        ('it', 'validation.suspiction_no_result', null),
        ('it', 'validation.any_result', null),
        ('it', 'validation.closed_case', null),
        ('it', 'form.trueLabel', null),
        ('it', 'form.falseLabel', null),
        ('en', 'ui.map.addLine', null),
        ('en', 'ui.map.addPoint', null),
        ('en', 'ui.map.addPolygon', null),
        ('en', 'ui.map.goTo', null),
        ('en', 'ui.map.panZoom', null),
        ('en', 'ui.map.move', null),
        ('en', 'ui.map.delete', null),
        ('en', 'ui.map.edit', null),
        ('en', 'form.insert.mail.mismatch', null),
        ('en', 'ui.map.insertPointGPS', null),
        ('en', 'user.form.save', null),
        ('en', 'ui.map.goToGPS', null),
        ('en', 'ui.map.insertPoint', null),
        ('en', 'ui.map.drawOnMap', null),
        ('en', 'ui.map.addPolygonHole', null),
        ('en', 'ui.map.drawOrEnter', null),
        ('en', 'ui.map.addPointGPS', null),
        ('en', 'form.back', null),
        ('en', 'validation.suspiction_no_result', null),
        ('en', 'validation.any_result', null),
        ('en', 'validation.closed_case', null),
        ('en', 'form.trueLabel', null),
        ('en', 'form.falseLabel', null),
        ('de', 'table.showMore', 'mehr Anzeigen'),
        ('de', 'table.showLess', 'weniger Anzeigen'),
        ('de', 'entity.back', '↩'),
        ('de', 'entity.table_back', 'zurück zur Tabelle'),
        ('it', 'table.showMore', null),
        ('fr', 'table.showMore', null),
        ('it', 'table.showLess', null),
        ('it', 'entity.back', null),
        ('fr', 'table.showLess', null),
        ('it', 'entity.table_back', null),
        ('fr', 'entity.back', null),
        ('fr', 'entity.table_back', null),
        ('en', 'table.showMore', null),
        ('en', 'table.showLess', null),
        ('en', 'entity.back', null),
        ('en', 'entity.table_back', null),
        ('de', 'popup.remove', null),
        ('it', 'popup.remove', null),
        ('fr', 'popup.remove', null),
        ('en', 'popup.remove', null),
        ('de', 'boolean.yes', 'Ja'),
        ('de', 'boolean.no', 'Nein'),
        ('it', 'boolean.yes', 'Si'),
        ('it', 'boolean.no', 'No'),
        ('de', 'survey.back', 'Zurück'),
        ('de', 'survey.next', 'Weiter'),
        ('it', 'survey.back', 'Indietro'),
        ('it', 'survey.next', 'Avanti'),
        ('en', 'boolean.yes', null),
        ('en', 'boolean.no', null),
        ('en', 'survey.back', null),
        ('en', 'survey.next', null),
        ('de', 'survey.no_data', 'Sie haben einen Schadorganismus / Schaden unter Vorkommen gemeldet – bitte füllen Sie auch Verbreitung und Befallsintensität aus.'),
        ('it', 'survey.no_data', 'Hai segnalato un organismo/danno sotto "Presenza" - per favore inserisci anche l’area di diffusione e l’intensità del danno.'),
        ('de', 'survey.invalid_data', 'Sie haben bei einem Schaden eine Verbreitung und eine Befallsintensität angegeben, jedoch unter Vorkommen angegeben, dass der Schaden nicht vorhanden ist. Bitte korrigieren Sie Ihre Eingabe an der entsprechenden Stelle.'),
        ('fr', 'ui.map.drawOrEnter', 'Définir un point sur la carte ou entrer les coordonnées'),
        ('fr', 'ui.map.addPoint', 'Définir un point'),
        ('fr', 'ui.map.addPointGPS', 'Définir un point (GPS/mobile)'),
        ('fr', 'table.filter.contains', 'Contient'),
        ('fr', 'boolean.yes', 'Oui'),
        ('fr', 'ui.map.panZoom', 'Déplacer la carte'),
        ('it', 'survey.invalid_data', 'Hai indicato una area di diffusione e una intensità del danno per un organismo/danno, ma sotto "Presenza" hai indicato che l''organismo è "Non osservata". Si prega di correggere la voce nel posto appropriato.'),
        ('en', 'survey.no_data', null),
        ('en', 'survey.invalid_data', null),
        ('it', 'table.shp', null),
        ('fr', 'table.shp', null),
        ('de', 'table.shp', null),
        ('en', 'table.shp', null),
        ('fr', 'ui.map.drawOnMap', 'Dessiner un polygone sur la carte'),
        ('fr', 'subform.add', 'Ajouter'),
        ('fr', 'login.failed', 'Login échoué'),
        ('fr', 'header.functions', 'Fonctions'),
        ('fr', 'navigation.recordFound', 'LIgnes trouvées'),
        ('fr', 'header.home', 'Home'),
        ('fr', 'table.no_action', 'Pas d’actions'),
        ('fr', 'entity.duplicate', 'Copie'),
        ('fr', 'login.button', 'Login'),
        ('fr', 'subform.remove', 'Effacer'),
        ('fr', 'header.forms', 'Masques'),
        ('fr', 'table.confirmDelete', 'Voulez-vous vraiment effacer las données?'),
        ('fr', 'table.confirmRevert', 'Voulez-vous vraiment annuler les changements?'),
        ('fr', 'messages.confirm', 'Voulez-vous vraiment effacer?'),
        ('fr', 'boolean.no', 'Non'),
        ('fr', 'entity.new', 'Nuoveau'),
        ('fr', 'login.password', 'Password'),
        ('fr', 'ui.map.move', 'Déplacer'),
        ('fr', 'navigation.record', 'Feu'),
        ('fr', 'navigation.page', 'Page'),
        ('fr', 'navigation.goAway', 'Abandonner la page sans sauver les changements?'),
        ('fr', 'user.form.save', 'Envoyer'),
        ('fr', 'survey.invalid_data', 'Vous avez indiqué une distribution et une gravité des dégâts pour un organisme / dégât, mais vous avez indiqué sous «Présence» que l’organisme est «Non constaté». Veuillez corriger votre indication à l''endroit correspondant.'),
        ('fr', 'login.title', 'Sign in'),
        ('fr', 'message.confirm', 'Vous êtes sûrs?'),
        ('fr', 'form.save', 'Sauvegarder'),
        ('fr', 'form.save_add', 'Sauvegarder et ajouter'),
        ('fr', 'form.save_table', 'Sauvegarder et retourner au tableau'),
        ('fr', 'header.lang', 'Langue'),
        ('fr', 'exports.select', 'Choisir une statistique'),
        ('fr', 'exports.search', 'Chercher statistique'),
        ('fr', 'entity.search', 'Cercher'),
        ('fr', 'entity.table', 'Tableau'),
        ('fr', 'header.tables', 'Tableaux'),
        ('fr', 'entity.title', 'Tableaux/Views'),
        ('fr', 'error.notfound', 'URL pas trouvé!'),
        ('fr', 'header.views', 'Views'),
        ('fr', 'survey.next', 'Suivant'),
        ('fr', 'navigation.of', 'de'),
        ('fr', 'sort.desc', '▲'),
        ('fr', 'navigation.next', '►'),
        ('fr', 'navigation.last', '▻'),
        ('fr', 'sort.asc', '▼'),
        ('fr', 'navigation.previous', '◄'),
        ('fr', 'navigation.first', '◅'),
        ('fr', 'table.show', 'Montrer'),
        ('fr', 'table.csv', 'Télécharger CSV'),
        ('fr', 'ui.map.addPolygon', 'Dessiner un polygone');

insert into box.ui (key, value, access_level_id)
values  ('enableAllTables', 'false', 1),
        ('showEntitiesSidebar', 'false', 1),
        ('logo', 'api/v1/uiFile/logo', -1),
        ('info', 'ui.info', 1),
        ('footerCopyright', 'TEST', -1),
        ('title', 'TEST', -1),
        ('debug', 'false', 1),
        ('showEntitiesSidebar', 'true', 1000),
        ('enableAllTables', 'true', 1000),
        ('menu', '[]', 1),
        ('filters.enable', ',=,not,>=,<=,like,dislike,FKlike,FKdislike', 1),
        ('filters.enable', 'all', 999);



--
-- Data for Name: users; Type: TABLE DATA; Schema: box; Owner: postgres
--

insert into box.users (username, access_level_id)
values  ('postgres', 1000);





ALTER TABLE ONLY box.access_level
    ADD CONSTRAINT access_level_pk PRIMARY KEY (access_level_id);


--
-- Name: conf conf_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.conf
    ADD CONSTRAINT conf_pkey PRIMARY KEY (key);


--
-- Name: cron cron_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.cron
    ADD CONSTRAINT cron_pkey PRIMARY KEY (name);


--
-- Name: export_field_i18n export_field_i18n_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.export_field_i18n
    ADD CONSTRAINT export_field_i18n_pkey PRIMARY KEY (uuid);


--
-- Name: export_field export_field_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.export_field
    ADD CONSTRAINT export_field_pkey PRIMARY KEY (field_uuid);


--
-- Name: export_i18n export_i18n_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.export_i18n
    ADD CONSTRAINT export_i18n_pkey PRIMARY KEY (uuid);


--
-- Name: export export_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.export
    ADD CONSTRAINT export_pkey PRIMARY KEY (export_uuid);


--
-- Name: field_file field_file_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.field_file
    ADD CONSTRAINT field_file_pkey PRIMARY KEY (field_uuid);


--
-- Name: field_i18n field_i18n_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.field_i18n
    ADD CONSTRAINT field_i18n_pkey PRIMARY KEY (uuid);


--
-- Name: field field_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.field
    ADD CONSTRAINT field_pkey PRIMARY KEY (field_uuid);


--
-- Name: flyway_schema_history_box flyway_schema_history_box_pk; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.flyway_schema_history_box
    ADD CONSTRAINT flyway_schema_history_box_pk PRIMARY KEY (installed_rank);


--
-- Name: form_actions form_actions_pk; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.form_actions
    ADD CONSTRAINT form_actions_pk PRIMARY KEY (uuid);


--
-- Name: form_i18n form_i18n_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.form_i18n
    ADD CONSTRAINT form_i18n_pkey PRIMARY KEY (uuid);


--
-- Name: form_navigation_actions form_navigation_actions_pk; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.form_navigation_actions
    ADD CONSTRAINT form_navigation_actions_pk PRIMARY KEY (uuid);


--
-- Name: form form_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.form
    ADD CONSTRAINT form_pkey PRIMARY KEY (form_uuid);


--
-- Name: function_field_i18n function_field_i18n_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.function_field_i18n
    ADD CONSTRAINT function_field_i18n_pkey PRIMARY KEY (uuid);


--
-- Name: function_field function_field_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.function_field
    ADD CONSTRAINT function_field_pkey PRIMARY KEY (field_uuid);


--
-- Name: function_i18n function_i18n_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.function_i18n
    ADD CONSTRAINT function_i18n_pkey PRIMARY KEY (uuid);


--
-- Name: function function_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.function
    ADD CONSTRAINT function_pkey PRIMARY KEY (function_uuid);


--
-- Name: image_cache image_cache_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.image_cache
    ADD CONSTRAINT image_cache_pkey PRIMARY KEY (key);


--
-- Name: labels labels_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.labels
    ADD CONSTRAINT labels_pkey PRIMARY KEY (lang, key);


--
-- Name: log log_pk; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.log
    ADD CONSTRAINT log_pk PRIMARY KEY (id);


--
-- Name: mails mails_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.mails
    ADD CONSTRAINT mails_pkey PRIMARY KEY (id);


--
-- Name: news_i18n news_i18n_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.news_i18n
    ADD CONSTRAINT news_i18n_pkey PRIMARY KEY (news_uuid, lang);


--
-- Name: news news_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.news
    ADD CONSTRAINT news_pkey PRIMARY KEY (news_uuid);


--
-- Name: public_entities public_entities_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.public_entities
    ADD CONSTRAINT public_entities_pkey PRIMARY KEY (entity);


--
-- Name: ui ui_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.ui
    ADD CONSTRAINT ui_pkey PRIMARY KEY (access_level_id, key);


--
-- Name: ui_src ui_src_pkey; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.ui_src
    ADD CONSTRAINT ui_src_pkey PRIMARY KEY (uuid);


--
-- Name: users users_pk; Type: CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.users
    ADD CONSTRAINT users_pk PRIMARY KEY (username);


--
-- Name: flyway_schema_history_box_s_idx; Type: INDEX; Schema: box; Owner: postgres
--

CREATE INDEX flyway_schema_history_box_s_idx ON box.flyway_schema_history_box USING btree (success);


--
-- Name: v_field v_field_del; Type: TRIGGER; Schema: box; Owner: postgres
--

CREATE TRIGGER v_field_del INSTEAD OF DELETE ON box.v_field FOR EACH ROW EXECUTE FUNCTION box.v_field_del();


--
-- Name: v_field v_field_ins; Type: TRIGGER; Schema: box; Owner: postgres
--

CREATE TRIGGER v_field_ins INSTEAD OF INSERT ON box.v_field FOR EACH ROW EXECUTE FUNCTION box.v_field_ins();


--
-- Name: v_field v_field_upd; Type: TRIGGER; Schema: box; Owner: postgres
--

CREATE TRIGGER v_field_upd INSTEAD OF UPDATE ON box.v_field FOR EACH ROW EXECUTE FUNCTION box.v_field_upd();


--
-- Name: v_labels v_labels_insert; Type: TRIGGER; Schema: box; Owner: postgres
--

CREATE TRIGGER v_labels_insert INSTEAD OF INSERT ON box.v_labels FOR EACH ROW EXECUTE FUNCTION box.v_labels_insert();


--
-- Name: v_labels v_labels_update; Type: TRIGGER; Schema: box; Owner: postgres
--

CREATE TRIGGER v_labels_update INSTEAD OF UPDATE ON box.v_labels FOR EACH ROW EXECUTE FUNCTION box.v_labels_update();


--
-- Name: field_file field_file_fielf_id_fk; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.field_file
    ADD CONSTRAINT field_file_fielf_id_fk FOREIGN KEY (field_uuid) REFERENCES box.field(field_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: field_i18n fkey_field; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.field_i18n
    ADD CONSTRAINT fkey_field FOREIGN KEY (field_uuid) REFERENCES box.field(field_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: function_field_i18n fkey_field; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.function_field_i18n
    ADD CONSTRAINT fkey_field FOREIGN KEY (field_uuid) REFERENCES box.function_field(field_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: export_field_i18n fkey_field; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.export_field_i18n
    ADD CONSTRAINT fkey_field FOREIGN KEY (field_uuid) REFERENCES box.export_field(field_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: field fkey_form; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.field
    ADD CONSTRAINT fkey_form FOREIGN KEY (form_uuid) REFERENCES box.form(form_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: form_i18n fkey_form; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.form_i18n
    ADD CONSTRAINT fkey_form FOREIGN KEY (form_uuid) REFERENCES box.form(form_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: function_i18n fkey_form; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.function_i18n
    ADD CONSTRAINT fkey_form FOREIGN KEY (function_uuid) REFERENCES box.function(function_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: function_field fkey_form; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.function_field
    ADD CONSTRAINT fkey_form FOREIGN KEY (function_uuid) REFERENCES box.function(function_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: export_i18n fkey_form; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.export_i18n
    ADD CONSTRAINT fkey_form FOREIGN KEY (export_uuid) REFERENCES box.export(export_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: export_field fkey_form; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.export_field
    ADD CONSTRAINT fkey_form FOREIGN KEY (export_uuid) REFERENCES box.export(export_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: field fkey_form_child; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.field
    ADD CONSTRAINT fkey_form_child FOREIGN KEY (child_form_uuid) REFERENCES box.form(form_uuid);


--
-- Name: news_i18n fkey_news_i18n; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.news_i18n
    ADD CONSTRAINT fkey_news_i18n FOREIGN KEY (news_uuid) REFERENCES box.news(news_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: form_actions form_actions_form_form_id_fk; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.form_actions
    ADD CONSTRAINT form_actions_form_form_id_fk FOREIGN KEY (form_uuid) REFERENCES box.form(form_uuid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: form_navigation_actions form_navigation_actions_form_form_id_fk; Type: FK CONSTRAINT; Schema: box; Owner: postgres
--

ALTER TABLE ONLY box.form_navigation_actions
    ADD CONSTRAINT form_navigation_actions_form_form_id_fk FOREIGN KEY (form_uuid) REFERENCES box.form(form_uuid) ON UPDATE CASCADE ON DELETE CASCADE;

CREATE FUNCTION box.check_mail_sent(_mail_id uuid) RETURNS boolean
    LANGUAGE sql SECURITY DEFINER
AS $$
select coalesce((select true from box.mails where id=_mail_id and sent_at is not null),false);
$$;


ALTER FUNCTION box.check_mail_sent(_mail_id uuid) OWNER TO postgres;

CREATE FUNCTION box.mail_notification(_mail_from box.email, _mail_to box.email[], _mail_cc box.email[], _mail_bcc box.email[], _subject text, _text text, _html text, _params jsonb) RETURNS uuid
    LANGUAGE plpgsql
AS $$
declare
    _mail_id uuid;
begin

    insert into box.mails (send_at,wished_send_at,mail_from,mail_to,mail_cc,mail_bcc,subject,text,html,params,created) values
        (now(),now(),_mail_from, _mail_to, _mail_cc,_mail_bcc, _subject, _text, _html,_params, now()) returning id into _mail_id;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel','{"sendMail": true}');

    return _mail_id;
end;
$$;


ALTER FUNCTION box.mail_notification(_mail_from box.email, _mail_to box.email[], _mail_cc box.email[], _mail_bcc box.email[], _subject text, _text text, _html text, _params jsonb) OWNER TO postgres;

CREATE FUNCTION box.mail_notification(_mail_from box.email, _mail_to box.email[], _subject text, _text text, _html text, _params jsonb) RETURNS uuid
    LANGUAGE sql
AS $$
select box.mail_notification(_mail_from,_mail_to,array[]::box.email[],array[]::box.email[],_subject,_text,_html,_params);
$$;


ALTER FUNCTION box.mail_notification(_mail_from box.email, _mail_to box.email[], _subject text, _text text, _html text, _params jsonb) OWNER TO postgres;




CREATE FUNCTION box.mail_notification(_mail_from box.email, _mail_to box.email[], _subject text, _text text, _html text) RETURNS uuid
    LANGUAGE sql
AS $$
select box.mail_notification(_mail_from,_mail_to,_subject,_text,_html,'{}'::jsonb);
$$;


ALTER FUNCTION box.mail_notification(_mail_from box.email, _mail_to box.email[], _subject text, _text text, _html text) OWNER TO postgres;




CREATE FUNCTION box.mail_sent_at(_mail_id uuid) RETURNS timestamp without time zone
    LANGUAGE sql
AS $$
select sent_at from box.mails where id=_mail_id;
$$;


ALTER FUNCTION box.mail_sent_at(_mail_id uuid) OWNER TO postgres;

