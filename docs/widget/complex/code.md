---
title: Code
parent: Complex
grand_parent: Widgets
nav_order: 2
---

# Code

Widget for editing code with syntax highlight, is the default widget to edit json as well.
The widget is implemented with [Monaco Editor](https://microsoft.github.io/monaco-editor/), the core of VS Code.


#### Supported types
- **String**
- **JSON**

#### Params

| Key          | Value                                                                                                                        | Default                               |
|:-------------|:-----------------------------------------------------------------------------------------------------------------------------|:--------------------------------------|
| language     | check the possible values on the MonacoEditor documentation, there is a high chance that your favorite language is supported | `html` for string and `json` for json |
| height     | editor height                                                                                                                | `200`                                 |
| fullWidth | show the picker at 100% width | `true`                                |