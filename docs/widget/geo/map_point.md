---
title: Map Point
parent: Widgets
grand_parent: Documentation
nav_order: 11
---

# Map Point

Draw a map using [openlayers](https://openlayers.org/)
TODO describe difference with standard map


#### Supported types
- **Geometry**

[PostGIS](https://postgis.net/) enabled database is required


## Map Widget customization
Map widget can be very complex and it's hard to cover all use cases with a generic configuration,
Box provides a generic configuration that is mainly tought as a Demo, for real implementation the widget can be customized at two level:
1. System-wise, adding a config entry `map.options` with the JSON configuration
1. Params field in Interface builder fields, adding the specific JSON configuration of the field definition of the form

The two configuration work in a hierarchical fashion, we take advantage of the JSON mergability so the form-filed specific
configuration overrides the config level configuration

### Configuration JSON
Here an example of the configuration Json with Swiss LV05 coordinates system and supporting points or polygons
```json
{
    "features": {
        "point": true,
        "multiPoint": true,
        "line": false,
        "multiLine": false,
        "polygon": true,
        "multiPolygon": true,
        "geometryCollection": false
    },
    "projections": [
        {
            "name": "EPSG:21781",
            "proj": "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=600000 +y_0=200000 +ellps=bessel +towgs84=674.4,15.1,405.3,0,0,0,0 +units=m +no_defs",
            "unit": "m",
            "extent": [
                485071.54,
                75346.36,
                828515.78,
                299941.84
            ]
        },
        {
            "name": "EPSG:2056",
            "proj": "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=2600000 +y_0=1200000 +ellps=bessel +towgs84=674.374,15.056,405.346,0,0,0,0 +units=m +no_defs ",
            "unit": "m",
            "extent": [
                2485071.58,
                1075346.31,
                2828515.82,
                1299941.79
            ]
        }
    ],
    "defaultProjection": "EPSG:21781",
    "baseLayers": [
        {
            "name": "Swisstopo",
            "capabilitiesUrl": "https://wmts.geo.admin.ch/EPSG/21781/1.0.0/WMTSCapabilities.xml",
            "layerId": "ch.swisstopo.pixelkarte-farbe"
        },
        {
            "name": "SwissImage",
            "capabilitiesUrl": "https://wmts.geo.admin.ch/EPSG/21781/1.0.0/WMTSCapabilities.xml",
            "layerId": "ch.swisstopo.swissimage"
         }
     ]
}
```

Fields:
- `features.point`: enables POINT features
- `features.multiPoint`: enables MULTIPOINT features
- `features.line`: enables LINESTRING features
- `features.multiLine`: enables MULTILINESTRING features
- `features.polygon`: enables POLYGON features
- `features.multiPolygon`: enables MULTIPOLYGON features
- `features.geometryCollection`: enables GEOMETRYCOLLECTION features
- `defaultProjection`: the default projection of the map
- `projections.0.name`: name of the projection (usually in the format `EPSG:<number>`)
- `projections.0.proj`: proj4 definition string (can be found in [https://epsg.io/](https://epsg.io/) )
- `projections.0.unit`: unit of measure
- `projections.0.extent`: vertex of the map
- Optional `baseLayers`: array of object, if not defined Openstreetmap is used as baselayer
    - `name`: name of the layer (it will appear on the UI)
    - `capabilitiesUrl`: WMTS capabilities URL
    - `layerId`: name of the layer
- Optional `precision`: precision of points
- Optional `formatters`: custom feature formatters, in the I18n.Label format i.e. `[{"lang": "id", "label": "Punto %x %y"}]`
    - Optional `point`
    - Optional `multiPoint`
    - Optional `line`
    - Optional `multiLine`
    - Optional `polygon`
    - Optional `multiPolygon`
- Optional `enableSwisstopo` enable swisstopo integration (i.e. location search)

| Key           | Value                      | Default |
|:--------------|:---------------------------|:--------|
| nolabel | hide the label             | `false` |  
| useXY | use X Y instead of Lat Lng | `false` |  
| coordinateLabel | TODO                       | `` |  

