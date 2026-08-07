package ch.wsl.box.rest.io.geotools

import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.model.shared.geo.Box2d
import org.geotools.api.data.SimpleFeatureSource
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.style.{NamedLayer, Style, StyledLayerDescriptor}
import org.geotools.data.collection.{CollectionFeatureSource, ListFeatureCollection}
import org.geotools.factory.CommonFactoryFinder
import org.geotools.feature.DefaultFeatureCollection
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.geometry.jts.ReferencedEnvelope
import org.geotools.map.{FeatureLayer, MapContent, MessageDirectLayer}
import org.geotools.ows.wms.WebMapServer
import org.geotools.ows.wms.map.WMSLayer
import org.geotools.ows.wmts.WebMapTileServer
import org.geotools.ows.wmts.map.WMTSMapLayer
import org.geotools.ows.wmts.model.WMTSLayer
import org.geotools.renderer.lite.StreamingRenderer
import org.geotools.sld.v1_1.{SLD, SLDConfiguration}
import org.geotools.styling.BasicPolygonStyle
import org.geotools.xml.styling.SLDParser
import org.geotools.xsd.DOMParser
import org.w3c.dom.Document

import java.awt.{Color, Rectangle, RenderingHints}
import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, StringReader}
import collection.JavaConverters._
import java.net.URL
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import scala.concurrent.{ExecutionContext, Future}

object Map2Image {

    private def renderMapToPNG(mapContent: MapContent, width: Int, height: Int,bbox:Box2d,crs:CoordinateReferenceSystem, copyright:String): Array[Byte] = {
        val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics

        val java2dHints =
            new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            )


        val rendererHints =
            new java.util.HashMap[AnyRef, Any]()


        rendererHints.put(
            StreamingRenderer.LINE_WIDTH_OPTIMIZATION_KEY,
            java.lang.Boolean.FALSE
        )

        rendererHints.put(
            StreamingRenderer.VECTOR_RENDERING_KEY,
            java.lang.Boolean.TRUE
        )

        // Set the background color (optional)
        graphics.setBackground(Color.WHITE)
        graphics.clearRect(0, 0, width, height)
        // Create a renderer
        val renderer = new StreamingRenderer
        renderer.setJava2DHints(java2dHints)
        renderer.setRendererHints(rendererHints)
        renderer.setMapContent(mapContent)
        // Set up the rendering context
        val paintArea = new Rectangle(0, 0, width, height)

        val envelope = new ReferencedEnvelope(bbox.xMin,bbox.xMax,bbox.yMin,bbox.yMax,crs)

        renderer.paint(graphics, paintArea, envelope)

        graphics.setColor(Color.white)
        graphics.fillRect(0,height - 30,width,30)
        graphics.setColor(Color.DARK_GRAY)
        graphics.drawString(copyright, 0, height - 15)



        val byteArrayOutputStream = new ByteArrayOutputStream

        // Save the image to file
        ImageIO.write(image, "png", byteArrayOutputStream)
        // Clean up
        graphics.dispose()
        mapContent.dispose()

        byteArrayOutputStream.toByteArray()
    }

    private def loadStyle(sld:String):Seq[Style] = {
        val styleFactory  = CommonFactoryFinder.getStyleFactory()

        // Create a DocumentBuilder to parse the XML String
        val factory = DocumentBuilderFactory.newInstance
        factory.setNamespaceAware(true)
        val builder = factory.newDocumentBuilder

        val source = new org.xml.sax.InputSource(new StringReader(sld))

        val document = builder.parse(source)

        val sldParser = new SLDParser(styleFactory)

        sldParser.readDOM(document)

        val parser = new DOMParser(
            new SLDConfiguration(),
            document
        )

        val sldParsed = parser.parse().asInstanceOf[StyledLayerDescriptor]
        sldParsed.getStyledLayers().head.asInstanceOf[NamedLayer].getStyles



    }

    def renderPng(wmtsServer:String,layerName:String,width:Int,height:Int, bbox:Box2d,geoms:Seq[Geometry],sld:String)(implicit ex:ExecutionContext):Future[Array[Byte]] = Future{

        val url = new URL(wmtsServer);

        val mapcontent = new MapContent

        val wmts = new WebMapTileServer(url)
        val capabilities = wmts.getCapabilities()

        // gets all the layers in a flat list, in the order they appear in
        // the capabilities document (so the rootLayer is at index 0)
        val layer = capabilities.getLayer(layerName)

        val copyright = capabilities.getService.getContactInformation.getOrganisationName.toString()

        val crs = capabilities.getMatrixSets.asScala.head.getCoordinateReferenceSystem


        mapcontent.setTitle(wmts.getCapabilities.getService.getTitle)
        mapcontent.addLayer(new WMTSMapLayer(wmts, layer))


        // Create vector schema
        val builder: SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder
        builder.setName("geom")
        GeotoolUtils.geomSchema(builder,"geom",geoms.map(x => Some(x)),geoms.headOption.map(_.crs.srid).getOrElse(0))
        val schema = builder.buildFeatureType()
        val collection = new DefaultFeatureCollection("geoms", schema)
        val featureBuilder = new SimpleFeatureBuilder(schema)

        // Add geoms
        geoms.flatMap(GeoJsonConverter.toJTS).foreach{ g =>
            featureBuilder.add(g)
            collection.add(featureBuilder.buildFeature(null))
        }
        val source = new CollectionFeatureSource(collection)


        // Load SLD style
        val styles = loadStyle(sld)

        // Add vector layer
        mapcontent.addLayer(new FeatureLayer(source,styles.head))

        renderMapToPNG(mapcontent,width,height,bbox,crs,copyright)

    }
}
