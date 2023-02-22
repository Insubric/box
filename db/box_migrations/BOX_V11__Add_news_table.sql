
CREATE TABLE if not exists "news" (
	"news_id" serial NOT NULL,
	"datetime" Timestamp Without Time Zone DEFAULT now() NOT NULL,
	"author" Character Varying( 2000 ) COLLATE "pg_catalog"."default",
	PRIMARY KEY ( "news_id" ) );

CREATE TABLE if not exists "news_i18n" (
	"news_id" Integer NOT NULL,
	"lang" Character Varying( 2 ) COLLATE "pg_catalog"."default" NOT NULL,
	"text" Text COLLATE "pg_catalog"."default" NOT NULL,
	"title" Text COLLATE "pg_catalog"."default",
	PRIMARY KEY ( "news_id", "lang" ) );

ALTER TABLE "news_i18n"
      drop constraint if exists "fkey_news_i18n";

ALTER TABLE "news_i18n"
	ADD CONSTRAINT "fkey_news_i18n" FOREIGN KEY ( "news_id" )
	REFERENCES "news" ( "news_id" ) MATCH SIMPLE
	ON DELETE No Action
	ON UPDATE No Action;
