package ch.wsl.box.client.vendors


import typings.ol.eventsConditionMod.Condition
import typings.ol.interactionDrawMod.SketchCoordType
import typings.ol.geomGeometryMod.Type
import typings.ol.featureMod.FeatureLike
import typings.ol.interactionSelectMod.FilterFunction
import typings.ol.styleStyleMod.{Style, StyleLike}

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation._

@JSImport("ol-ext/interaction/DrawHole", JSImport.Default)
@js.native
class DrawHole extends typings.ol.interactionInteractionMod.default {
  def this(opt_options: DrawHoleOptions) = this()
}

@js.native
trait DrawHoleOptions extends js.Object {
  var addCondition: js.UndefOr[Condition] = js.native
  var condition: js.UndefOr[Condition] = js.native
  var features: js.UndefOr[
    typings.ol.collectionMod.default[typings.ol.featureMod.default[typings.ol.geomGeometryMod.default]]
  ] = js.native
  var filter: js.UndefOr[FilterFunction] = js.native
  var hitTolerance: js.UndefOr[Double] = js.native
  var layers: js.UndefOr[
    js.Array[typings.ol.layerLayerMod.default[typings.ol.sourceSourceMod.default,_]] | (js.Function1[
      /* p0 */ typings.ol.layerLayerMod.default[typings.ol.sourceSourceMod.default,_],
      Boolean
    ])
  ] = js.native
  var multi: js.UndefOr[Boolean] = js.native
  var removeCondition: js.UndefOr[Condition] = js.native
  var style: js.UndefOr[StyleLike] = js.native
  var toggleCondition: js.UndefOr[Condition] = js.native
}

object DrawHoleOptions{
    @scala.inline
    def apply(): DrawHoleOptions = {
      val __obj = js.Dynamic.literal()
      __obj.asInstanceOf[DrawHoleOptions]
    }
    @scala.inline
    implicit class OptionsOps[Self <: DrawHoleOptions] (val x: Self) extends AnyVal {
      @scala.inline
      def duplicate: Self = (js.Dynamic.global.Object.assign(js.Dynamic.literal(), x)).asInstanceOf[Self]
      @scala.inline
      def combineWith[Other <: js.Any](other: Other): Self with Other = (js.Dynamic.global.Object.assign(js.Dynamic.literal(), x, other.asInstanceOf[js.Any])).asInstanceOf[Self with Other]
      @scala.inline
      def set(key: String, value: js.Any): Self = {
        x.asInstanceOf[js.Dynamic].updateDynamic(key)(value)
        x
      }
//      @scala.inline
//      def setType(value: GeometryType): Self = this.set("type", value.asInstanceOf[js.Any])
      @scala.inline
      def setClickTolerance(value: Double): Self = this.set("clickTolerance", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteClickTolerance: Self = this.set("clickTolerance", js.undefined)
      @scala.inline
      def setCondition(value: Condition): Self = this.set("condition", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteCondition: Self = this.set("condition", js.undefined)
      @scala.inline
      def setDragVertexDelay(value: Double): Self = this.set("dragVertexDelay", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteDragVertexDelay: Self = this.set("dragVertexDelay", js.undefined)
      @scala.inline
      def setFeatures(
                       value: typings.ol.collectionMod.default[typings.ol.featureMod.default[typings.ol.geomGeometryMod.default]]
                     ): Self = this.set("features", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteFeatures: Self = this.set("features", js.undefined)
      @scala.inline
      def setFinishCondition(value: Condition): Self = this.set("finishCondition", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteFinishCondition: Self = this.set("finishCondition", js.undefined)
      @scala.inline
      def setFreehand(value: Boolean): Self = this.set("freehand", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteFreehand: Self = this.set("freehand", js.undefined)
      @scala.inline
      def setFreehandCondition(value: Condition): Self = this.set("freehandCondition", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteFreehandCondition: Self = this.set("freehandCondition", js.undefined)
      @scala.inline
      def setGeometryFunction(
                               value: (/* p0 */ SketchCoordType, /* p1 */ js.UndefOr[typings.ol.geomSimpleGeometryMod.default], /* p2 */ js.UndefOr[typings.ol.projProjectionMod.default]) => typings.ol.geomSimpleGeometryMod.default
                             ): Self = this.set("geometryFunction", js.Any.fromFunction3(value))
      @scala.inline
      def deleteGeometryFunction: Self = this.set("geometryFunction", js.undefined)
      @scala.inline
      def setGeometryName(value: String): Self = this.set("geometryName", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteGeometryName: Self = this.set("geometryName", js.undefined)
      @scala.inline
      def setMaxPoints(value: Double): Self = this.set("maxPoints", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteMaxPoints: Self = this.set("maxPoints", js.undefined)
      @scala.inline
      def setMinPoints(value: Double): Self = this.set("minPoints", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteMinPoints: Self = this.set("minPoints", js.undefined)
      @scala.inline
      def setSnapTolerance(value: Double): Self = this.set("snapTolerance", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteSnapTolerance: Self = this.set("snapTolerance", js.undefined)
      @scala.inline
      def setSource(value: typings.ol.sourceVectorMod.default[typings.ol.geomGeometryMod.default]): Self = this.set("source", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteSource: Self = this.set("source", js.undefined)
      @scala.inline
      def setStopClick(value: Boolean): Self = this.set("stopClick", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteStopClick: Self = this.set("stopClick", js.undefined)
      @scala.inline
      def setStyleFunction2(value: (/* p0 */ FeatureLike, /* p1 */ Double) => Style | js.Array[Style] | Unit): Self = this.set("style", js.Any.fromFunction2(value))
      @scala.inline
      def setStyleVarargs(value: Style*): Self = this.set("style", js.Array(value :_*))
      @scala.inline
      def setStyle(value: StyleLike): Self = this.set("style", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteStyle: Self = this.set("style", js.undefined)
      @scala.inline
      def setWrapX(value: Boolean): Self = this.set("wrapX", value.asInstanceOf[js.Any])
      @scala.inline
      def deleteWrapX: Self = this.set("wrapX", js.undefined)
    }



}

