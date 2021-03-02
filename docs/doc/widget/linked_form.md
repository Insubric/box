---
title: Linked form
parent: Widgets
grand_parent: Documentation
nav_order: 9
---

# Linked form

Renders the link to the form/page, if it's a form it links to the table

#### Supported types
- **Child**

#### Interface builder fields

| Entity             | Field name        | Description       | Required       | Default       |
|:-------------------|:------------------|:------------------|:------------------|:------------------|
| field              | Child form        | Select the target form   | `true`           | `none`           |
| field              | Child query       | Filters the child with the selected query (in JSONQuery format), useful in combination with param: `"open": "first"`      | `false`           | `none`          |
| field translations | Static content    | Label form the link      | `false`           | Form name           |


#### Params

| Key          | Value                   | Default           |
|:-------------|:------------------------|:------------------|
| `style`      | `box`: for boxed style  | `none`            |
| `color`      | hex formatted color (i.e. `#123456`)  | Main link color in config            |
| `background` | hex formatted color  | Main color in config            |
| `open`       | `first`: to open the first found record in form  | Opens the form table or page            |

