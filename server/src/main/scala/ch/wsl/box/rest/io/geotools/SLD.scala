package ch.wsl.box.rest.io.geotools

object SLD {
  def defaultPoint = """<?xml version="1.0" encoding="UTF-8"?>
                       |<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" version="1.1.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:se="http://www.opengis.net/se" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.1.0/StyledLayerDescriptor.xsd">
                       |  <NamedLayer>
                       |    <se:Name>New scratch layer</se:Name>
                       |    <UserStyle>
                       |      <se:Name>New scratch layer</se:Name>
                       |      <se:FeatureTypeStyle>
                       |        <se:Rule>
                       |          <se:Name>Single symbol</se:Name>
                       |          <se:PointSymbolizer>
                       |            <se:Graphic>
                       |              <se:Mark>
                       |                <se:WellKnownName>circle</se:WellKnownName>
                       |                <se:Fill>
                       |                  <se:SvgParameter name="fill">#db1e2a</se:SvgParameter>
                       |                </se:Fill>
                       |                <se:Stroke>
                       |                  <se:SvgParameter name="stroke">#801119</se:SvgParameter>
                       |                  <se:SvgParameter name="stroke-width">1</se:SvgParameter>
                       |                </se:Stroke>
                       |              </se:Mark>
                       |              <se:Size>14</se:Size>
                       |            </se:Graphic>
                       |          </se:PointSymbolizer>
                       |        </se:Rule>
                       |      </se:FeatureTypeStyle>
                       |    </UserStyle>
                       |  </NamedLayer>
                       |</StyledLayerDescriptor>
                       |""".stripMargin

  def defaultPolygon = s"""<?xml version="1.0" encoding="UTF-8"?>
                          |<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" version="1.1.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:se="http://www.opengis.net/se" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.1.0/StyledLayerDescriptor.xsd">
                          |  <NamedLayer>
                          |    <se:Name>New scratch layer</se:Name>
                          |    <UserStyle>
                          |      <se:Name>New scratch layer</se:Name>
                          |      <se:FeatureTypeStyle>
                          |        <se:Rule>
                          |          <se:Name>Single symbol</se:Name>
                          |          <se:PolygonSymbolizer>
                          |            <se:Fill>
                          |              <se:SvgParameter name="fill">#e41a1c</se:SvgParameter>
                          |              <se:SvgParameter name="fill-opacity">0.47</se:SvgParameter>
                          |            </se:Fill>
                          |            <se:Stroke>
                          |              <se:SvgParameter name="stroke">#800e10</se:SvgParameter>
                          |              <se:SvgParameter name="stroke-width">1</se:SvgParameter>
                          |              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
                          |            </se:Stroke>
                          |          </se:PolygonSymbolizer>
                          |        </se:Rule>
                          |      </se:FeatureTypeStyle>
                          |    </UserStyle>
                          |  </NamedLayer>
                          |</StyledLayerDescriptor>""".stripMargin
}
