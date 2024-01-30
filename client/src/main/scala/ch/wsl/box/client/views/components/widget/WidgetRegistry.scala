package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.views.components.widget.admin.LayoutWidget
import ch.wsl.box.client.views.components.widget.array.{ChoicesWidget, MultiWidget}
import ch.wsl.box.client.views.components.widget.boolean.SelectBooleanWidget
import ch.wsl.box.client.views.components.widget.child.{EditableTable, LookupFormWidget, SimpleChildFactory, Spreadsheet, TableChildFactory, TrasparentChild}
import ch.wsl.box.client.views.components.widget.geo.{MapPointWidget, OlMapListWidget, OlMapWidget}
import ch.wsl.box.client.views.components.widget.labels.{HtmlWidget, LinkedFormWidget, LookupLabelWidget, StaticTextWidget, TitleWidget}
import ch.wsl.box.client.views.components.widget.lookup.{MultipleLookupWidget, PopupSelectWidget, SelectWidgetFactory}
import ch.wsl.box.client.views.components.widget.utility.{DropdownLangWidget, LangWidget}
import ch.wsl.box.model.shared.WidgetsNames
import scribe.Logging

object WidgetRegistry extends Logging {
  val widgets:Seq[ComponentWidgetFactory] = Seq(

    TitleWidget(1),
    TitleWidget(2),
    TitleWidget(3),
    TitleWidget(4),
    TitleWidget(5),
    StaticTextWidget,
    HtmlWidget,

    HiddenWidget,
    LangWidget,
    DropdownLangWidget,

    PopupSelectWidget,
    SelectWidgetFactory,

    LookupLabelWidget,

    InputWidgetFactory.Input,
    InputWidgetFactory.IntegerDecimal2,
    InputWidgetFactory.InputDisabled,
    InputWidgetFactory.TwoLines,
    InputWidgetFactory.TextArea,

    CheckboxWidget,
    TristateWidget,
    SelectBooleanWidget,

    DateTimeWidget.Time,
    DateTimeWidget.Date,
    DateTimeWidget.DateTime,

    TableChildFactory,
    SimpleChildFactory,
    TrasparentChild,
    EditableTable,
    LookupFormWidget,
    LinkedFormWidget,
    Spreadsheet,

    FileSimpleWidgetFactory,

    OlMapWidget,
    OlMapListWidget,
    MapPointWidget,

    MonacoWidget,
    RichTextEditorWidgetFactory(RichTextEditorWidget.Minimal),
    RichTextEditorWidgetFactory(RichTextEditorWidget.Full),
    RichTextPopup,
    RedactorFactory,

    DynamicWidget,

    ExecuteFunctionWidget,

    SliderWidget,

    ChoicesWidget,
    MultipleLookupWidget,

    MultiWidget,

    PopupWidget,

    LayoutWidget

  )

  def forName(widgetName:String): ComponentWidgetFactory = widgets.find(_.name == widgetName) match {
    case Some(w) => w
    case None => {
      logger.warn(s"Widget: $widgetName not registred, using default")
      InputWidgetFactory.Input
    }
  }

  @deprecated
  def forType(typ:String):ComponentWidgetFactory = {
    logger.warn("Selecting widget for type is deprecated, specify widget name instead")
    val widgetName = WidgetsNames.defaults.getOrElse(typ,s"no default widget for $typ")
    forName(widgetName)
  }

}
