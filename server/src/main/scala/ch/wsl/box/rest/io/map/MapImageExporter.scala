package ch.wsl.box.rest.io.map

import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.data.store.EmptyFeatureCollection
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.geometry.jts.{JTSFactoryFinder, ReferencedEnvelope}

import javax.imageio.ImageIO
import java.awt.{Color, Graphics2D, Rectangle}
import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, IOException}
import org.geotools.map.{FeatureLayer, Layer, MapContent, MapViewport}
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.geotools.renderer.lite.StreamingRenderer
import org.geotools.styling.SLD
import org.geotools.tile.impl.WebMercatorTileService
import org.geotools.tile.impl.osm.OSMService
import org.geotools.tile.util.TileLayer
import org.locationtech.jts.geom.{Coordinate, Point}
import org.opengis.geometry.BoundingBox

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import scala.concurrent.{ExecutionContext, Future}

object MapImageExporter {

  def render()(implicit ec:ExecutionContext) = {

    val builder = new SimpleFeatureTypeBuilder();
    val geometryFactory = JTSFactoryFinder.getGeometryFactory()
    builder.setName("Location")
    builder.setCRS(DefaultGeographicCRS.WGS84) // <- Coordinate reference system

    // add attributes in order
    builder.add("the_geom", classOf[Point])
    builder.length(15).add("Name", classOf[String]) // <- 15 chars width for name field
    builder.add("number", classOf[Integer])

    // build the type
    val LOCATION = builder.buildFeatureType()

    val featureBuilder = new SimpleFeatureBuilder(LOCATION)



    val point = geometryFactory.createPoint(new Coordinate(11.116667,46.066667))

    featureBuilder.add(point)
    featureBuilder.add("TEST")
    featureBuilder.add(1)
    val feature = featureBuilder.buildFeature(null)

    val collection = new EmptyFeatureCollection(LOCATION)

    collection.add(feature)


    val baseURL = "http://tile.openstreetmap.org/"
    val service = new OSMService("OSM", baseURL)
    service.getScaleList
    val osm = new TileLayer(service)
    osm.setVisible(true)

    val map: MapContent = new MapContent
    map.setTitle("Quickstart")
    val mapBound = new ReferencedEnvelope(osm.getBounds)
    map.setViewport(new MapViewport(mapBound))
    map.getViewport.setScreenArea(new Rectangle(0,0,500,500))
//    map.getMaxBounds.setBounds(mapBound)
//    map.getViewport.setCoordinateReferenceSystem(mapBound.getCoordinateReferenceSystem)

    map.addPropertyChangeListener(new PropertyChangeListener {
      override def propertyChange(evt: PropertyChangeEvent): Unit = {
        println(evt)
      }
    })


    val style = SLD.createSimpleStyle(LOCATION)
    val layer: Layer = new FeatureLayer(collection, style)

    map.addLayer(osm)
    map.addLayer(layer)

    for{
      r <- Future.successful(saveImage(map))
    } yield r


  }





  def saveImage(map: MapContent): Array[Byte] = {

    val renderer = new StreamingRenderer()
    renderer.setMapContent(map)
    val mapBounds = map.getMaxBounds
    val imageBounds = map.getViewport.getScreenArea


    val image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB)
    val gr = image.createGraphics
    gr.setPaint(Color.WHITE)
    gr.fill(imageBounds)

    renderer.paint(gr, imageBounds, mapBounds)

    val baos = new ByteArrayOutputStream()
    ImageIO.write(image, "jpeg", baos)
    baos.flush()
    val result = baos.toByteArray
    baos.close()
    result

  }



}
