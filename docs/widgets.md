---
title: Widgets
nav_order: 4
has_children: true
---



## Dynamic select fields
Lookup tables can be queried dynamically by adding a JSONQuery to the field
and setting some parameters with `#fieldname`, the system will substitute the `#fieldname` parameter
with the current value and update the lookup values.

Example:
```
{
    "filter": [
        {
            "column": "enddate",
            "operator": "<=",
            "value": #date
        },
        {
            "column": "startdate",
            "operator": ">=",
            "value": #date
        }
    ],
    "sort": [],
    "paging": {
        "pageLength": 500,
        "currentPage": 1
    }
}
```