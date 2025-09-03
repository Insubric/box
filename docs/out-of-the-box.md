---
title: Out of the Box
permalink: /out-of-the-box/
nav_order: 3
layout: default
---

# Out of the Box Features

When you log in to Box for the first time, you will have the opportunity to explore your database tables without needing any further configuration.


## Tabular View
The Box Framework provides a robust tabular view for accessing and managing database tables. This view is designed to be intuitive and powerful, offering a range of features to enhance data management and visualization.

![Tabular view](/assets/images/table.png)

### Filtering

The tabular view includes a powerful filtering feature that allows users to filter data based on various criteria. This makes it easier to find specific records without having to manually search through the entire dataset. Key filtering capabilities include:

- **Column-based Filtering**: Filter data based on specific columns.
- **Multiple Criteria**: Apply multiple filtering criteria simultaneously.
- **Dynamic Updates**: Filters update the table view in real-time as criteria are adjusted.

### Export Options

The tabular view supports exporting data in multiple formats, making it easy to share and analyze data outside of the web interface. The available export options include:

- **CSV (Comma-Separated Values)**: Export data in a CSV format for easy data exchange and compatibility with spreadsheet software like Microsoft Excel and Google Sheets.
- **XLS (Excel Spreadsheet)**: Export data directly into an Excel spreadsheet format for detailed data analysis and reporting.
- **Shapefile**: Export geospatial data in the Shapefile format, which is widely used in Geographic Information Systems (GIS) for storing and analyzing geographic information.
- **Geopackage**: Export geospatial data in the Geopackage format, an open standard for geospatial data storage that supports both vector and raster data.

### Map Visualization

For geospatial data, the tabular view includes a map visualization feature. This allows users to visualize and interact with geographic information directly within the web interface. Key map visualization capabilities include:

- **Interactive Map**: Display data points on an interactive map, allowing users to zoom, pan, and click on data points for more information.
- **Layer Support**: Support for multiple layers, enabling users to overlay different datasets on the map.
- **Spatial Queries**: Perform spatial queries to filter data based on the extent of the map.


### Form View
Allows for detailed editing of individual records through a web interface.

![Form view](/assets/images/form.png)

The Box Framework leverages the PostgreSQL information schema to automatically generate forms. 

- **Data Type Detection**: It fetches the data types of each column, ensuring that the form fields match the expected data types (e.g., text, numbers, dates).
- **Foreign Key Handling**: The framework detects foreign key relationships and generates appropriate dropdowns or lookup fields, making it easy to maintain referential integrity.

## Next Step

[Define your own UI](/ui/)
