CREATE TABLE if not exists "box"."image_cache" (
                                            "key" text NOT NULL,
                                            "data" bytea NOT NULL,
                                            PRIMARY KEY ( "key" ) );