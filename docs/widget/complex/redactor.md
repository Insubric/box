---
title: Redactor
parent: Complex
grand_parent: Widgets
nav_order: 13
---

# Redactor

Implements the [redactor](https://imperavi.com/redactor/) WYSIWYG editor. Since redactor is not open source to enable this widget the files of redactor needs to be updated in Admin -> Config.
Using this widget the resulting string is in HTML, it may be useful if you plan to use Box as CMS

#### Supported types
- **String**


#### Params

| Key          | Value             |
|:-------------|:------------------|
| editorOptions  | You may use the redactor object |


##### Example
```json
{
  "editorOptions" : {
    "buttons" : [
      "format",
      "alignment",
      "|",
      "bold",
      "italic",
      "|",
      "lists",
      "horizontalrule",
      "|",
      "link",
      "|",
      "html"
    ],
    "plugins" : [
      "source",
      "alignment"
    ],
    "maxHeight" : "350px",
    "minHeight" : "350px",
    "formatting" : [
      "p",
      "h2",
      "h3",
      "h4"
    ],
    "removeEmpty" : [
      "strong",
      "em",
      "span",
      "p"
    ]
  }
}
```