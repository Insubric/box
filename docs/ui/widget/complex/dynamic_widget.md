---
title: Dynamic widget
parent: Complex
grand_parent: Widgets
nav_order: 2
---

# Dynamic widget

Render widget depending other field valus


#### Widget Selector object
| Property      | Value                         | 
|:--------------|:------------------------------|
| name | Widget name                   |
| params | Optional, widget parameters   | 

#### Params

| Key           | Value                                                                              | 
|:--------------|:-----------------------------------------------------------------------------------|
| selectorField | Field to watch to extract the widget                                               |
| widgetMapping | Map String,WidgetSelector, with key coresponding to the value of the selectorField | 
| default       | WidgetSelector object                                                              | 