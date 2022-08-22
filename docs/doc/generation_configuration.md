---
title: Code Generator Configuration
parent: Documentation
nav_order: 1
---

# Code Generator Configuration

Example conf:
```
db {
  url="jdbc:postgresql://127.0.0.1:5432/test"
  user="postgres"
  password="password"
  schema="public"
  adminPoolSize=20

  generator{
    tables=["*"],
    views=["*"],
    excludes=["flyway_schema_history","geography_columns","geometry_columns","raster_columns","raster_overviews","spatial_ref_sys"],    //excludes tables and views from generation,  works with regexp
    excludeFields=[], //excludes fields from generation
    keys {    //specify if key on inserts are managed by dbms (db) or application (app)
      default.strategy="app"
      db=[]
      app=[]
    },
    hasTriggerDefault=[
      "case.user_create", "case.date_create",
    ]
    files=[
    ]
  }
}


```

## Default value in database
If you want to use the default values specified in the database add the column in the `hasTriggerDefault` property 