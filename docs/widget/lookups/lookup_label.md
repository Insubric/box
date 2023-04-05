---
title: Lookup label
parent: Widgets
grand_parent: Documentation
nav_order: 10
---

# Lookup label

This widget do a lookup in a foreign table and render it in the current form. It may be useful for example to show images that are stored in a lookup table.

The value of the lookup is then injected in the data object with `$` so it can be used i.e. for conditional checking

#### Supported types
- **String**
- **Number**

#### Interface builder fields

| Entity             | Field name        | Description                                                    | Required          | Default           |
|:-------------------|:------------------|:---------------------------------------------------------------|:------------------|:------------------|
| field              | lookupEntity      | Select the target entity                                       | `true`            | `none`            |
| field              | lookupValueField  | Column on the foreign entity                                   | `true`            | `none`            |
| field translations | lookupTextField   | Field on the foreign entity that should be used as label       | `false`           | `none`            |

#### Params

| Key          | Value                                                 | Default                   |
|:-------------|:------------------------------------------------------|:--------------------------|
| widget       | The widget name to use for rendering the lookup label | Selected using field type |
