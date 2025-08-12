---
title: Select
parent: Lookups
grand_parent: Widgets
nav_order: 15
---

# Select

This widget present a dropdown menu

#### Supported types
- **String**
- **Number**

#### Interface builder fields

| Entity             | Field name        | Description                                                        | Required          | Default           |
|:-------------------|:------------------|:-------------------------------------------------------------------|:------------------|:------------------|
| field           | lookupEntity      | Select the table/view to look at           | `true`           | `none`           |
| field           | lookupValueField  | Field on the external table to match with the field of `name`, usually `name` is the foreign key reffering to `lookupValueField`       | `true`           | `none`           |
| field           | lookupQuery  | JSONQuery to filter and/or sort the possible selectable results       | `false`           | `none`           |
| field translations | lookupTextField  | Field on the child form that should be used as label        | `false`           | `none`           |

TODO describe dynamic query format and special `##lang` keyword

### TODO Describe params 
- fullWidth