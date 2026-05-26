package ch.wsl.box.client.views.components.table

import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.utils.{BootstrapStyles, UdashBootstrapComponent}
import io.udash.properties.seq
import io.udash._
import org.scalajs.dom.Element
import scalatags.JsDom.all._


class BoxTable[ItemType, ElemType <: ReadableProperty[ItemType]] (
                                                                            items: seq.ReadableSeqProperty[ItemType, ElemType],
                                                                            nestedInterceptor: Binding.NestedInterceptor,
                                                                            mod:Modifier*
                                                                          )(
                                                                            headerFactory: Option[Binding.NestedInterceptor => Modifier],
                                                                            rowFactory: (ElemType, Binding.NestedInterceptor) => Element
                                                                          )  {

  import io.udash.css.CssView._

  val render: Element = {
    div(
      table(
        BootstrapStyles.Table.table,
        mod
      )(
        headerFactory.map(head => thead(head(nestedInterceptor)).render),
        tbody(
          nestedInterceptor(
            repeatWithNested(items) { case (item, nested) =>
              rowFactory(item, nested)
            }
          )
        )
      )
    ).render
  }
}