package ch.wsl.box.model

import java.util.Base64

import boxentities._
import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.services.Services
import net.ceedubs.ficus.Ficus._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object BuildBox {

  println("Installing box schema")

  final val logo = "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"


  def install(boxDb:Database,username:String) = {

    val createSchemaStatement = s"create schema if not exists ${BoxSchema.schema.getOrElse("box")};"


    //!!!!!!!!! never change this schema use flyway migration if you need to change the schema
    for{

      _ <- boxDb.run(sqlu"""
      
      
      #$createSchemaStatement




CREATE TABLE box.access_level (
    access_level_id serial NOT NULL,
    access_level text NOT NULL
);


CREATE TABLE box.conf (
    id serial NOT NULL,
    key character varying NOT NULL,
    value character varying
);


CREATE TABLE box.export (
    export_id serial NOT NULL,
    name character varying NOT NULL,
    function character varying NOT NULL,
    description character varying,
    layout character varying,
    parameters character varying,
    "order" double precision,
    access_role text[]
);



CREATE TABLE box.export_field (
    field_id serial NOT NULL,
    export_id integer NOT NULL,
    type character varying NOT NULL,
    name character varying NOT NULL,
    widget character varying,
    "lookupEntity" character varying,
    "lookupValueField" character varying,
    "lookupQuery" character varying,
    "default" character varying,
    "conditionFieldId" character varying,
    "conditionValues" character varying
);



CREATE TABLE box.export_field_i18n (
    id serial NOT NULL,
    field_id integer,
    lang character(2) DEFAULT NULL::bpchar,
    label character varying,
    placeholder character varying,
    tooltip character varying,
    hint character varying,
    "lookupTextField" character varying
);


CREATE TABLE box.export_header_i18n (
    id serial NOT NULL,
    key character varying NOT NULL,
    lang character varying NOT NULL,
    label character varying NOT NULL
);



CREATE TABLE box.export_i18n (
    id serial NOT NULL,
    export_id integer,
    lang character(2) DEFAULT NULL::bpchar,
    label character varying,
    tooltip character varying,
    hint character varying,
    function character varying
);


CREATE TABLE box.field (
    field_id serial NOT NULL,
    form_id integer NOT NULL,
    type character varying NOT NULL,
    name character varying NOT NULL,
    widget character varying,
    "lookupEntity" character varying,
    "lookupValueField" character varying,
    "lookupQuery" character varying,
    child_form_id integer,
    "masterFields" character varying,
    "childFields" character varying,
    "childQuery" character varying,
    "default" character varying,
    "conditionFieldId" character varying,
    "conditionValues" character varying
);


CREATE TABLE box.field_file (
    field_id integer NOT NULL,
    file_field character varying NOT NULL,
    thumbnail_field character varying,
    name_field character varying NOT NULL
);



CREATE TABLE box.field_i18n (
    id serial NOT NULL,
    field_id integer,
    lang character(2) DEFAULT NULL::bpchar,
    label character varying,
    placeholder character varying,
    tooltip character varying,
    hint character varying,
    "lookupTextField" character varying
);



CREATE TABLE box.form (
    form_id serial NOT NULL,
    name character varying NOT NULL,
    entity character varying NOT NULL,
    description character varying,
    layout character varying,
    "tabularFields" character varying,
    query character varying,
    exportfields character varying
);


CREATE TABLE box.form_i18n (
    id serial NOT NULL,
    form_id integer,
    lang character(2) DEFAULT NULL::bpchar,
    label character varying,
    tooltip character varying,
    hint character varying
);



CREATE TABLE box.function (
    function_id serial NOT NULL,
    name character varying NOT NULL,
    function character varying NOT NULL,
    description character varying,
    layout character varying,
    "order" double precision,
    access_role text[],
    presenter text,
    mode text DEFAULT 'table'::text NOT NULL
);


CREATE TABLE box.function_field (
    field_id serial NOT NULL,
    function_id integer NOT NULL,
    type character varying NOT NULL,
    name character varying NOT NULL,
    widget character varying,
    "lookupEntity" character varying,
    "lookupValueField" character varying,
    "lookupQuery" character varying,
    "default" character varying,
    "conditionFieldId" character varying,
    "conditionValues" character varying
);



CREATE TABLE box.function_field_i18n (
    id serial NOT NULL,
    field_id integer,
    lang character(2) DEFAULT NULL::bpchar,
    label character varying,
    placeholder character varying,
    tooltip character varying,
    hint character varying,
    "lookupTextField" character varying
);



CREATE TABLE box.function_i18n (
    id serial NOT NULL,
    function_id integer,
    lang character(2) DEFAULT NULL::bpchar,
    label character varying,
    tooltip character varying,
    hint character varying,
    function character varying
);



CREATE TABLE box.labels (
    id serial NOT NULL,
    lang character varying NOT NULL,
    key character varying NOT NULL,
    label character varying
);



CREATE TABLE box.ui (
    id serial NOT NULL,
    key character varying NOT NULL,
    value character varying NOT NULL,
    access_level_id integer NOT NULL
);


CREATE TABLE box.ui_src (
    id serial NOT NULL,
    file bytea,
    mime character varying,
    name character varying,
    access_level_id integer NOT NULL
);



CREATE TABLE box.users (
    username character varying NOT NULL,
    access_level_id integer NOT NULL
);

ALTER TABLE ONLY box.access_level
    ADD CONSTRAINT access_level_pkey PRIMARY KEY (access_level_id);


ALTER TABLE ONLY box.conf
    ADD CONSTRAINT conf_pkey PRIMARY KEY (id);

ALTER TABLE ONLY box.export_field_i18n
    ADD CONSTRAINT export_field_i18n_pkey PRIMARY KEY (id);


ALTER TABLE ONLY box.export_field
    ADD CONSTRAINT export_field_pkey PRIMARY KEY (field_id);


ALTER TABLE ONLY box.export_header_i18n
    ADD CONSTRAINT export_header_i18n_pkey PRIMARY KEY (id);


ALTER TABLE ONLY box.export_i18n
    ADD CONSTRAINT export_i18n_pkey PRIMARY KEY (id);


ALTER TABLE ONLY box.export
    ADD CONSTRAINT export_pkey PRIMARY KEY (export_id);



ALTER TABLE ONLY box.field_file
    ADD CONSTRAINT field_file_pkey PRIMARY KEY (field_id);



ALTER TABLE ONLY box.field_i18n
    ADD CONSTRAINT field_i18n_pkey PRIMARY KEY (id);



ALTER TABLE ONLY box.field
    ADD CONSTRAINT field_pkey PRIMARY KEY (field_id);




ALTER TABLE ONLY box.form_i18n
    ADD CONSTRAINT form_i18n_pkey PRIMARY KEY (id);



ALTER TABLE ONLY box.form
    ADD CONSTRAINT form_pkey PRIMARY KEY (form_id);



ALTER TABLE ONLY box.function_field_i18n
    ADD CONSTRAINT function_field_i18n_pkey PRIMARY KEY (id);



ALTER TABLE ONLY box.function_field
    ADD CONSTRAINT function_field_pkey PRIMARY KEY (field_id);


ALTER TABLE ONLY box.function_i18n
    ADD CONSTRAINT function_i18n_pkey PRIMARY KEY (id);


ALTER TABLE ONLY box.function
    ADD CONSTRAINT function_pkey PRIMARY KEY (function_id);






ALTER TABLE ONLY box.labels
    ADD CONSTRAINT labels_pkey PRIMARY KEY (id);





ALTER TABLE ONLY box.ui
    ADD CONSTRAINT ui_pkey PRIMARY KEY (id);



ALTER TABLE ONLY box.ui_src
    ADD CONSTRAINT ui_src_pkey PRIMARY KEY (id);



ALTER TABLE ONLY box.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (username);



ALTER TABLE ONLY box.field_file
    ADD CONSTRAINT field_file_fielf_id_fk FOREIGN KEY (field_id) REFERENCES box.field(field_id);


ALTER TABLE ONLY box.export_i18n
    ADD CONSTRAINT fkey_export FOREIGN KEY (export_id) REFERENCES box.export(export_id);



ALTER TABLE ONLY box.export_field
    ADD CONSTRAINT fkey_export FOREIGN KEY (export_id) REFERENCES box.export(export_id);



ALTER TABLE ONLY box.export_field_i18n
    ADD CONSTRAINT fkey_field FOREIGN KEY (field_id) REFERENCES box.export_field(field_id);


ALTER TABLE ONLY box.field_i18n
    ADD CONSTRAINT fkey_field FOREIGN KEY (field_id) REFERENCES box.field(field_id) ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY box.function_field_i18n
    ADD CONSTRAINT fkey_field FOREIGN KEY (field_id) REFERENCES box.function_field(field_id) ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY box.field
    ADD CONSTRAINT fkey_form FOREIGN KEY (form_id) REFERENCES box.form(form_id) ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY box.form_i18n
    ADD CONSTRAINT fkey_form FOREIGN KEY (form_id) REFERENCES box.form(form_id) ON UPDATE CASCADE ON DELETE CASCADE;




ALTER TABLE ONLY box.function_i18n
    ADD CONSTRAINT fkey_function FOREIGN KEY (function_id) REFERENCES box.function(function_id) ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY box.function_field
    ADD CONSTRAINT fkey_function FOREIGN KEY (function_id) REFERENCES box.function(function_id) ON UPDATE CASCADE ON DELETE CASCADE;






              """)
      _ <- boxDb.run(BoxAccessLevel.BoxAccessLevelTable.insertOrUpdateAll(Seq(
        BoxAccessLevel.BoxAccessLevel_row(-1,"Not logged user"),
        BoxAccessLevel.BoxAccessLevel_row(1,"Read-Only User"),
        BoxAccessLevel.BoxAccessLevel_row(10,"Regular User"),
        BoxAccessLevel.BoxAccessLevel_row(100,"Power User"),
        BoxAccessLevel.BoxAccessLevel_row(1000,"Administrator")
      )))
      _ <- boxDb.run(BoxConf.BoxConfTable ++= Seq(
        BoxConf.BoxConf_row(key = "host", value = Some("0.0.0.0")),
        BoxConf.BoxConf_row(key = "port", value = Some("8080")),
        BoxConf.BoxConf_row(key = "server-secret", value = Some("changeMe-sadf-09fd65465443554345654e565e45653445se554d6554d65r54d65r54d65r546d5r5dasdfiasdf897sdf-as-s9d8fd9f8s09fku")),
        BoxConf.BoxConf_row(key = "cookie.name", value = Some("_boxsession_myapp")),
        BoxConf.BoxConf_row(key = "langs", value = Some("en")),
        BoxConf.BoxConf_row(key = "pks.edit", value = Some("false")),
        BoxConf.BoxConf_row(key = "fks.lookup.labels", value = Some("default = firstNoPKField")),
        BoxConf.BoxConf_row(key = "fks.lookup.rowsLimit", value = Some("50")),
        BoxConf.BoxConf_row(key = "display.index.news", value = Some("false")),
        BoxConf.BoxConf_row(key = "display.index.html", value = Some("false")),
        BoxConf.BoxConf_row(key = "filter.precision.datetime", value = Some("DATETIME")),
        BoxConf.BoxConf_row(key = "filter.precision.double", value = Some("1")),
        BoxConf.BoxConf_row(key = "page.length", value = Some("30")),
        BoxConf.BoxConf_row(key = "notification.timeout", value = Some("6")),
        BoxConf.BoxConf_row(key = "cache.enable", value = Some("true")),
        BoxConf.BoxConf_row(key = "color.main", value = Some("#343C4B")),
        BoxConf.BoxConf_row(key = "color.link", value = Some("#7C8277")),
        BoxConf.BoxConf_row(key = "color.danger", value = Some("#C54E13")),
        BoxConf.BoxConf_row(key = "color.warning", value = Some("#EB883E")),
        BoxConf.BoxConf_row(key = "logger.level", value = Some("warn"))
      ))
      _ <- boxDb.run(BoxUIsrcTable.BoxUIsrcTable += BoxUIsrcTable.BoxUIsrc_row(file = Some(Base64.getDecoder.decode(logo)),mime = Some("image/png"),name = Some("logo"),accessLevel = -1))
      _ <- boxDb.run(BoxUITable.BoxUITable ++= Seq(
        BoxUITable.BoxUI_row(key = "title", value = "Box Framework", accessLevel = -1),
        BoxUITable.BoxUI_row(key = "footerCopyright", value = "Box framework", accessLevel = -1),
        BoxUITable.BoxUI_row(key = "enableAllTables", value = "false", accessLevel = 1),
        BoxUITable.BoxUI_row(key = "enableAllTables", value = "true", accessLevel = 1000),
        BoxUITable.BoxUI_row(key = "showEntitiesSidebar", value = "false", accessLevel = 1),
        BoxUITable.BoxUI_row(key = "showEntitiesSidebar", value = "true", accessLevel = 1000),
        BoxUITable.BoxUI_row(key = "menu", value = "[]", accessLevel = 1),
        BoxUITable.BoxUI_row(key = "debug", value = "false", accessLevel = 1),
        BoxUITable.BoxUI_row(key = "logo", value = "api/v1/uiFile/logo", accessLevel = -1),
        BoxUITable.BoxUI_row(key = "info", value = "ui.info", accessLevel = 1),
      ))
      _ <- boxDb.run(BoxUser.BoxUserTable += BoxUser.BoxUser_row(username,1000))
      _ <- boxDb.run(BoxLabels.BoxLabelsTable ++= DefaultLabels.labels)

      _ <- boxDb.run{sql"""CREATE OR REPLACE VIEW "box"."v_roles" AS
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
       """.as[Boolean].headOption}

      _ <- boxDb.run{sql"""CREATE OR REPLACE FUNCTION box.hasrole(rol text)
                                       RETURNS boolean
                                       LANGUAGE plpgsql
                                       AS $$function$$
                                       DECLARE
                                            roles text[];
                                       BEGIN
                                             select memberof into roles from box.v_roles where lower(rolname) = lower(current_user);
                                             return rol = any(roles);
                                      END
                                      $$function$$
      """.as[Boolean].headOption}

      _ <- boxDb.run{sql"""CREATE OR REPLACE FUNCTION box.hasrolein(rol text[])
                                       RETURNS boolean
                                       LANGUAGE plpgsql
                                        AS $$function$$
                                        DECLARE
                                           roles text[];
                                        BEGIN
                                          select memberof into roles from box.v_roles where lower(rolname) = lower(current_user);
                                          return rol && roles;   --intersection of the 2 arrays
                                        END
                                        $$function$$
      """.as[Boolean].headOption}

    } yield "ok"
  }

  def main(args: Array[String]): Unit = {
    println("Dropping box tables")

    DefaultModule.injector.build[Services] { services =>
      val installShowError = install(services.connection.dbConnection,services.connection.adminUser).recover{ case t:Throwable => t.printStackTrace()}

      Await.result(installShowError, 30 seconds)
      services.connection.close()

      println("Box schema ready")
    }
  }

}

object DefaultLabels {



  private val rawLabels =
    """
      |en	entity.duplicate	Copy
      |it	entity.duplicate	Duplica
      |fr	entity.duplicate	Copie
      |de	entity.duplicate	Kopie
      |en	entity.new	New
      |it	entity.new	Nuovo
      |fr	entity.new	Nuoveau
      |de	entity.new	Neu
      |en	entity.search	Search
      |it	entity.search	Cerca
      |fr	entity.search	Cercher
      |de	entity.search	Suchen
      |en	entity.select	Select
      |it	entity.select	Seleziona
      |fr	entity.select	Sélectionner
      |de	entity.select	Auswählen
      |en	entity.table	Table
      |it	entity.table	Tabella
      |fr	entity.table	Tableau
      |de	entity.table	Tabelle
      |en	entity.title	Tables/Views
      |it	entity.title	Tabelle/Views
      |fr	entity.title	Tableaux/Views
      |de	entity.title	Tabellen/Views
      |en	error.notfound	URL not found!
      |it	error.notfound	URL non trovato!
      |fr	error.notfound	URL pas trouvé!
      |de	error.notfound	URL nicht gefunden!
      |en	exports.search	Search statistics
      |it	exports.search	Cerca statistica
      |fr	exports.search	Chercher statistique
      |de	exports.search	Statistik suchen
      |en	exports.select	Select a statistic
      |it	exports.select	Scegliere una statistica
      |fr	exports.select	Choisir une statistique
      |de	exports.select	Statistik auswählen
      |en	exports.title	Statistics
      |it	exports.title	Statistiche
      |fr	exports.title	Statistiques
      |de	exports.title	Statistiken
      |en	form.changed	Data changed
      |it	form.changed	Dati modificati
      |fr	form.changed	Données modifiées
      |de	form.changed	Daten abgeändert
      |en	form.required	required
      |it	form.required	obbligatorio
      |fr	form.required	REQUIRED
      |de	form.required	erforderlich
      |en	form.save	Save
      |it	form.save	Salva
      |fr	form.save	Sauvegarder
      |de	form.save	Speichern
      |en	form.save_add	Save and insert next
      |it	form.save_add	Salva e aggiungi
      |fr	form.save_add	Sauvegarder et ajouter
      |de	form.save_add	Speichern und einfügen
      |en	form.save_table	Save and back to table
      |it	form.save_table	Salva e ritorna alla tabella
      |fr	form.save_table	Sauvegarder et retourner au tableau
      |de	form.save_table	Speichern und zurück zur Tabelle
      |en	header.entities	Entities
      |it	header.entities	Entità
      |fr	header.entities	Entitées
      |de	header.entities	Entität
      |en	header.exports	Exports
      |it	header.exports	Statistiche
      |fr	header.exports	Statistiques
      |de	header.exports	Statistiken
      |en	header.forms	Forms
      |it	header.forms	Maschere
      |fr	header.forms	Masques
      |de	header.forms	Masken
      |en	header.functions	Functions
      |it	header.functions	Funzioni
      |fr	header.functions	Fonctions
      |de	header.functions	Funktionen
      |en	header.home	Home
      |it	header.home	Home
      |fr	header.home	Home
      |de	header.home	Home
      |en	header.lang	Language
      |it	header.lang	Lingua
      |fr	header.lang	Langue
      |de	header.lang	Sprache
      |en	header.tables	Tables
      |it	header.tables	Tabelle
      |fr	header.tables	Tableaux
      |de	header.tables	Tabellen
      |en	header.views	Views
      |it	header.views	Views
      |fr	header.views	Views
      |de	header.views	Views
      |en	login.button	Login
      |it	login.button	Login
      |fr	login.button	Login
      |de	login.button	Login
      |en	login.failed	Login failed
      |it	login.failed	Login fallito
      |fr	login.failed	Login pas réussi
      |de	login.failed	Fehlgeschlagene Anmeldung
      |en	login.password	Password
      |it	login.password	Password
      |fr	login.password	Password
      |de	login.password	Password
      |en	login.title	Sign In
      |it	login.title	Sign in
      |fr	login.title	Sign in
      |de	login.title	Sign in
      |en	login.username	Username
      |it	login.username	Nome utente
      |fr	login.username	Nom utilisateur
      |de	login.username	Benutzername
      |en	message.confirm	Are you sure?
      |it	message.confirm	Sei sicuro?
      |fr	message.confirm	Vous êtes sûrs?
      |de	message.confirm	Sind Sie sicher?
      |en	messages.confirm	Do you really want to delete?
      |it	messages.confirm	Vuoi veramente cancellare?
      |fr	messages.confirm	Voulez-vous vraiment effacer?
      |de	messages.confirm	Möchten Sie wirklich löschen?
      |en	navigation.first	◅
      |it	navigation.first	◅
      |fr	navigation.first	◅
      |de	navigation.first	◅
      |en	navigation.goAway	Leave page without saving changes?
      |it	navigation.goAway	Vuoi lasciare la pagina senza salvare i cambiamenti?
      |fr	navigation.goAway	Abandonner la page sans sauver les changements?
      |de	navigation.goAway	Seite verlassen ohne zu speichern?
      |en	navigation.last	▻
      |it	navigation.last	▻
      |fr	navigation.last	▻
      |de	navigation.last	▻
      |en	navigation.loading	Loading
      |it	navigation.loading	Caricamento
      |fr	navigation.loading	Chargement
      |de	navigation.loading	Aktualisierung
      |en	navigation.next	►
      |it	navigation.next	►
      |fr	navigation.next	►
      |de	navigation.next	►
      |en	navigation.of	of
      |it	navigation.of	di
      |fr	navigation.of	de
      |de	navigation.of	von
      |en	navigation.page	Page
      |it	navigation.page	Pagina
      |fr	navigation.page	Page
      |de	navigation.page	Seite
      |en	navigation.previous	◄
      |it	navigation.previous	◄
      |fr	navigation.previous	◄
      |de	navigation.previous	◄
      |en	navigation.record	Record
      |it	navigation.record	Evento
      |fr	navigation.record	Feu
      |de	navigation.record	Waldbrand
      |en	navigation.recordFound	Found records
      |it	navigation.recordFound	Righe trovate
      |fr	navigation.recordFound	LIgnes trouvés
      |de	navigation.recordFound	Gefundene Zeilen
      |en	sort.asc	▼
      |it	sort.asc	▼
      |fr	sort.asc	▼
      |de	sort.asc	▼
      |en	sort.desc	▲
      |it	sort.desc	▲
      |fr	sort.desc	▲
      |de	sort.desc	▲
      |en	sort.ignore	‎
      |it	sort.ignore	‎
      |fr	sort.ignore	‎
      |de	sort.ignore	‎
      |en	subform.add	Add
      |it	subform.add	Aggiungi
      |fr	subform.add	Ajouter
      |de	subform.add	Einfügen
      |en	subform.remove	Remove
      |it	subform.remove	Rimuovi
      |fr	subform.remove	Effacer
      |de	subform.remove	Löschen
      |en	table.actions	Actions
      |it	table.actions	Azioni
      |fr	table.actions	Actions
      |de	table.actions	Aktionen
      |en	table.csv	Download CSV
      |it	table.csv	Scarica CSV
      |fr	table.csv	Télécharger CSV
      |de	table.csv	CSV herunterladen
      |en	table.confirmDelete	Do you really want to delete the record?
      |it	table.confirmDelete	Vuole veramente cancellare i dati?
      |fr	table.confirmDelete	Voulez-vous vraiment effacer las données?
      |de	table.confirmDelete	Möchten Sie den Datensatz wirklich löschen?
      |en	table.confirmRevert	Do you really want to discard the changes?
      |it	table.confirmRevert	Vuole veramente annullare i cambiamenti?
      |fr	table.confirmRevert	Voulez-vous vraiment annuler les changements?
      |de	table.confirmRevert	Möchten Sie die Änderungen wirklich verwerfen?
      |en	table.delete	Delete
      |it	table.delete	Elimina
      |fr	table.delete	Supprimer
      |de	table.delete	Löschen
      |en	table.edit	Edit
      |it	table.edit	Modifica
      |fr	table.edit	Modifier
      |de	table.edit	Editieren
      |en	table.no_action	No action
      |it	table.no_action	Nessuna azione
      |fr	table.no_action	Pas d’actions
      |de	table.no_action	Keine Aktion
      |en	table.show	Show
      |it	table.show	Mostrare
      |fr	table.show	Montrer
      |de	table.show	Anzeigen
      |en	ui.index.title	<h1>BOX database</h1> Welcome to BOX.
      |it	ui.index.title	<h1>BOX database</h1> Benvenuti a BOX
      |fr	ui.index.title	<h1>BOX database</h1> Bienvenue a BOX.
      |de	ui.index.title	<h1>BOX database</h1> Wilkommen zur BOX.      |
    """.stripMargin


  val labels:Seq[BoxLabels.BoxLabels_row] = rawLabels.lines.map(_.split("\\t")).filterNot(_.length < 3).map{ line =>
    BoxLabels.BoxLabels_row(lang = line(0), key = line(1), label = Some(line.drop(2).mkString(" ")))
  }.toSeq
}
