---
title: Lookup form
parent: Widgets
grand_parent: Documentation
nav_order: 9
---

# Lookup form

Open the child on a new page in his form definition.

#### Supported types
- **Child**

#### Interface builder fields

| Entity             | Field name        | Description                                                    | Required          | Default           |
|:-------------------|:------------------|:---------------------------------------------------------------|:------------------|:------------------|
| field              | Child form        | Select the target form                                         | `true`            | `none`            |
| field              | Parent key fields | Field on the parent form that compose the ID of the child form | `true`            | `none`            |
| field translations | lookupTextField   | Field on the child form that should be used as label           | `false`           | `none`            |
| field translations | Static content    | Static label                                                   | `false`           | `"Open"`          |


#### Params

| Key          | Value                                                 | Default                   |
|:-------------|:------------------------------------------------------|:--------------------------|
| widget       | The widget name to use for rendering the lookup label | Selected using field type |

