package ch.wsl.box.client.styles

import scalacss.ScalatagsCss._
import scalatags.JsDom
import scalatags.JsDom.all._


object Icons {

  type Icon = JsDom.RawFrag

  //https://icons.getbootstrap.com/icons/pencil/
  val pencil: Icon = raw(s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-pencil" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M11.293 1.293a1 1 0 0 1 1.414 0l2 2a1 1 0 0 1 0 1.414l-9 9a1 1 0 0 1-.39.242l-3 1a1 1 0 0 1-1.266-1.265l1-3a1 1 0 0 1 .242-.391l9-9zM12 2l2 2-9 9-3 1 1-3 9-9z"/>
       |  <path fill-rule="evenodd" d="M12.146 6.354l-2.5-2.5.708-.708 2.5 2.5-.707.708zM3 10v.5a.5.5 0 0 0 .5.5H4v.5a.5.5 0 0 0 .5.5H5v.5a.5.5 0 0 0 .5.5H6v-1.5a.5.5 0 0 0-.5-.5H5v-.5a.5.5 0 0 0-.5-.5H3z"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/dot/
  val point:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-dot" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M8 9.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/pentagon/
  val polygon:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-pentagon" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M8 1.288l-6.842 5.56L3.733 15h8.534l2.575-8.153L8 1.288zM16 6.5L8 0 0 6.5 3 16h10l3-9.5z"/>
       |</svg>
       |""".stripMargin
  )

  //https://icons.getbootstrap.com/icons/egg-fried/
  val hole:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-egg-fried" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M13.665 6.113a1 1 0 0 1-.667-.977L13 5a4 4 0 0 0-6.483-3.136 1 1 0 0 1-.8.2 4 4 0 0 0-3.693 6.61 1 1 0 0 1 .2 1 4 4 0 0 0 6.67 4.087 1 1 0 0 1 1.262-.152 2.5 2.5 0 0 0 3.715-2.905 1 1 0 0 1 .341-1.113 2.001 2.001 0 0 0-.547-3.478zM14 5c0 .057 0 .113-.003.17a3.001 3.001 0 0 1 .822 5.216 3.5 3.5 0 0 1-5.201 4.065 5 5 0 0 1-8.336-5.109A5 5 0 0 1 5.896 1.08 5 5 0 0 1 14 5z"/>
       |  <circle cx="8" cy="8" r="3"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/arrows-move/
  val move:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-arrows-move" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M6.5 8a.5.5 0 0 0-.5-.5H1.5a.5.5 0 0 0 0 1H6a.5.5 0 0 0 .5-.5z"/>
       |  <path fill-rule="evenodd" d="M3.854 5.646a.5.5 0 0 0-.708 0l-2 2a.5.5 0 0 0 0 .708l2 2a.5.5 0 0 0 .708-.708L2.207 8l1.647-1.646a.5.5 0 0 0 0-.708zM9.5 8a.5.5 0 0 1 .5-.5h4.5a.5.5 0 0 1 0 1H10a.5.5 0 0 1-.5-.5z"/>
       |  <path fill-rule="evenodd" d="M12.146 5.646a.5.5 0 0 1 .708 0l2 2a.5.5 0 0 1 0 .708l-2 2a.5.5 0 0 1-.708-.708L13.793 8l-1.647-1.646a.5.5 0 0 1 0-.708zM8 9.5a.5.5 0 0 0-.5.5v4.5a.5.5 0 0 0 1 0V10a.5.5 0 0 0-.5-.5z"/>
       |  <path fill-rule="evenodd" d="M5.646 12.146a.5.5 0 0 0 0 .708l2 2a.5.5 0 0 0 .708 0l2-2a.5.5 0 0 0-.708-.708L8 13.793l-1.646-1.647a.5.5 0 0 0-.708 0zM8 6.5a.5.5 0 0 1-.5-.5V1.5a.5.5 0 0 1 1 0V6a.5.5 0 0 1-.5.5z"/>
       |  <path fill-rule="evenodd" d="M5.646 3.854a.5.5 0 0 1 0-.708l2-2a.5.5 0 0 1 .708 0l2 2a.5.5 0 0 1-.708.708L8 2.207 6.354 3.854a.5.5 0 0 1-.708 0z"/>
       |</svg>
       |""".stripMargin
  )

  //https://icons.getbootstrap.com/icons/trash/
  val trash:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-trash" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
       |  <path fill-rule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4L4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/search/
  val search:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-search" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M10.442 10.442a1 1 0 0 1 1.415 0l3.85 3.85a1 1 0 0 1-1.414 1.415l-3.85-3.85a1 1 0 0 1 0-1.415z"/>
       |  <path fill-rule="evenodd" d="M6.5 12a5.5 5.5 0 1 0 0-11 5.5 5.5 0 0 0 0 11zM13 6.5a6.5 6.5 0 1 1-13 0 6.5 6.5 0 0 1 13 0z"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/slash/
  val line:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-slash" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M11.854 4.146a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708-.708l7-7a.5.5 0 0 1 .708 0z"/>
       |</svg>
       |""".stripMargin
  )

  //https://icons.getbootstrap.com/icons/hand-index/
  val hand:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-hand-index" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M6.75 1a.75.75 0 0 0-.75.75V9a.5.5 0 0 1-1 0v-.89l-1.003.2a.5.5 0 0 0-.399.546l.345 3.105a1.5 1.5 0 0 0 .243.666l1.433 2.15a.5.5 0 0 0 .416.223h6.385a.5.5 0 0 0 .434-.252l1.395-2.442a2.5 2.5 0 0 0 .317-.991l.272-2.715a1 1 0 0 0-.995-1.1H13.5v1a.5.5 0 0 1-1 0V7.154a4.208 4.208 0 0 0-.2-.26c-.187-.222-.368-.383-.486-.43-.124-.05-.392-.063-.708-.039a4.844 4.844 0 0 0-.106.01V8a.5.5 0 0 1-1 0V5.986c0-.167-.073-.272-.15-.314a1.657 1.657 0 0 0-.448-.182c-.179-.035-.5-.04-.816-.027l-.086.004V8a.5.5 0 0 1-1 0V1.75A.75.75 0 0 0 6.75 1zM8.5 4.466V1.75a1.75 1.75 0 0 0-3.5 0v5.34l-1.199.24a1.5 1.5 0 0 0-1.197 1.636l.345 3.106a2.5 2.5 0 0 0 .405 1.11l1.433 2.15A1.5 1.5 0 0 0 6.035 16h6.385a1.5 1.5 0 0 0 1.302-.756l1.395-2.441a3.5 3.5 0 0 0 .444-1.389l.272-2.715a2 2 0 0 0-1.99-2.199h-.582a5.184 5.184 0 0 0-.195-.248c-.191-.229-.51-.568-.88-.716-.364-.146-.846-.132-1.158-.108l-.132.012a1.26 1.26 0 0 0-.56-.642 2.634 2.634 0 0 0-.738-.288c-.31-.062-.739-.058-1.05-.046l-.048.002zm2.094 2.025z"/>
       |</svg>
       |""".stripMargin)


}
