package ch.wsl.box.client.geo

import ch.wsl.box.client.services.BrowserConsole
import typings.ol.extentMod.Extent
import typings.ol.geomMod.Point
import typings.ol.styleStyleMod.StyleFunction
import typings.ol.{styleCircleMod, styleFillMod, styleMod, styleStrokeMod, styleStyleMod}

import scala.scalajs.js

object MapStyle {

  def simpleStyle(color:String = "rgb(237, 28, 36)") = {

    val fillColor = color.replaceAll("rgb","rgba").replaceAll("\\)",",0.2)")

    new styleMod.Style(styleStyleMod.Options()
      .setFill(new styleMod.Fill(styleFillMod.Options().setColor(fillColor)))
      .setStroke(new styleMod.Stroke(styleStrokeMod.Options().setColor(color).setWidth(2)))
      .setImage(
        new styleMod.Circle(styleCircleMod.Options(3)
          .setFill(
            new styleMod.Fill(styleFillMod.Options().setColor(color))
          )
        ).asInstanceOf[typings.ol.imageMod.default]
      )
    )
  }

  def vectorStyle(color:String = "rgb(237, 28, 36)"): js.Array[typings.ol.styleStyleMod.Style] = js.Array(
    simpleStyle(color),
    new styleMod.Style(styleStyleMod.Options()
      .setImage(
        new styleMod.Circle(styleCircleMod.Options(8)
          .setStroke(
            new styleMod.Stroke(styleStrokeMod.Options().setColor(color).setWidth(2))
          )
        ).asInstanceOf[typings.ol.imageMod.default]
      )
    )
  )


  def growingStyle(color:String = "rgba(237, 28, 36)", fillColor:String = "rgb(237, 28, 36,0.2)"):StyleFunction = {
    val small = new styleMod.Style(styleStyleMod.Options()
      .setFill(new styleMod.Fill(styleFillMod.Options().setColor(fillColor)))
      .setStroke(new styleMod.Stroke(styleStrokeMod.Options().setColor(color).setWidth(2)))
      .setImage(
        new styleMod.Circle(styleCircleMod.Options(3)
          .setFill(
            new styleMod.Fill(styleFillMod.Options().setColor(color))
          )
        )
      )
    )

    val large = new styleMod.Style(styleStyleMod.Options()
      //.setFill(new styleMod.Fill(styleFillMod.Options().setColor(fillColor)))
      .setStroke(new styleMod.Stroke(styleStrokeMod.Options().setColor(color).setWidth(5.0)))
      .setGeometryFunction1((geom) => {
        val extent = geom.asInstanceOf[js.Dynamic].getGeometry().getExtent().asInstanceOf[Extent]
        new Point(typings.ol.extentMod.getCenter(extent))
      })
      .setImage(
        new styleMod.Circle(styleCircleMod.Options(5)
          .setFill(
            new styleMod.Fill(styleFillMod.Options().setColor(color))
          )
        )
      )
    )

    (feature, resolution) => {
      if (resolution < 30) {
        small
      } else {
        large
      }
    }
  }



      val highlightStyle: js.Array[typings.ol.styleStyleMod.Style] = js.Array(
        new styleMod.Style(styleStyleMod.Options()
          .setFill(new styleMod.Fill(styleFillMod.Options().setColor("rgba(237, 28, 36,0.2)")))
          .setStroke(new styleMod.Stroke(styleStrokeMod.Options().setColor("#ed1c24").setWidth(4)))
          .setImage(
            new styleMod.Circle(styleCircleMod.Options(3)
              .setFill(
                new styleMod.Fill(styleFillMod.Options().setColor("rgb(237, 28, 36)"))
              )
            ).asInstanceOf[typings.ol.imageMod.default]
          )
        ),
        new styleMod.Style(styleStyleMod.Options()
          .setImage(
            new styleMod.Circle(styleCircleMod.Options(8)
              .setStroke(
                new styleMod.Stroke(styleStrokeMod.Options().setColor("#ed1c24").setWidth(4))
              )
            ).asInstanceOf[typings.ol.imageMod.default]
          )
        )
      )
}
