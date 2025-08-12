---
title: Childs
parent: Widgets
nav_order: 6
has_children: true
---

# Childs

TODO explain how childs works.

Describe `#all` special field

#### Params

| Key                   | Value                                                                                                                                                                                | Default |
|:----------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------|
| disableAdd            | Disable add button                                                                                                                                                                   | `false` |
| disableRemove         | Disable remove button                                                                                                                                                                | `false` |
| disableDuplicate      | Disable duplicate button                                                                                                                                                             | `false` |
| duplicateIgnoreFields | Ignore fields when duplicating                                                                                                                                                       | `[]`    |
| enableDeleteOnlyNew   | Enable delete only for newly created rows                                                                                                                                            | `false` |
| sortable              | Enable sorting of rows                                                                                                                                                               | `false` |
| props                 | Static properties object for child                                                                                                                                                   | `null`  | 
| deleteWhenHidden      | If true childs that are not present in the JSON will be de deleted from DB,in order to avoid data losses by default hidden (usually by a condition) fields have no effect on the DB  | `false` | 

Different kind of childs may have more specific parameters