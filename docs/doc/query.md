---
title: JSON Query
parent: Documentation
nav_order: 2
---

# JSON Query

In Box framework to fetch a set of data from an entity we defied a generic structure to specify filters, sorting and pagination.

Example: 
```json
{
  "filter" : [
    {
      "column" : "name",
      "operator" : "like",
      "value" : "Minetti"
    }
  ],
  "sort" : [
    {
      "column" : "id",
      "order" : "desc"
    }
  ],
  "paging" : {
    "pageLength" : 30,
    "currentPage" : 1
  }
}
```

TODO explain details