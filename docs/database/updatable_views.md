---
title: Updatable views
parent: Database
nav_order: 3
layout: default
---


## Updatable view support
Box supports updatable views for forms, that could be useful if you need to build a form that doesn't have a direct table representation.

In order to do so an updatable view should be created, to create updatable views in postgres please refer to [https://www.postgresql.org/docs/current/sql-createview.html#SQL-CREATEVIEW-UPDATABLE-VIEWS](https://www.postgresql.org/docs/current/sql-createview.html#SQL-CREATEVIEW-UPDATABLE-VIEWS) 

Since view doesn't have primary keys to be able to update a record the key fields should be defined in the interface builder.

Example:

SQL
```

CREATE VIEW public.some_view AS
SELECT
       dna.id AS id,
       e.name AS person,
       cea.author_id AS author,
       cea.notes AS notes,
       dna.result AS result,
       dna.verified AS verified,
       dna.file AS file
FROM public.dna_analysis dna
LEFT JOIN public.case_analisis cea on cea.dna_id = dna.id
LEFT JOIN public.entity e on cea.author_id = e.id


CREATE OR REPLACE FUNCTION some_view_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE public.dna_analysis SET file = NEW.file, result = NEW.result, verified = NEW.verified WHERE id = NEW.id;
    RETURN NEW;
END $$;

CREATE TRIGGER some_view_trigger
    INSTEAD OF UPDATE ON public.some_view
    FOR EACH ROW EXECUTE FUNCTION some_view_update();
```