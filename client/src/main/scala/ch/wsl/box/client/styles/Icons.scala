package ch.wsl.box.client.styles

import ch.wsl.box.shared.utils.SVG
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


  //https://icons.getbootstrap.com/icons/list/
  val list:Icon = raw(
    s"""
       |<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-list" viewBox="0 0 16 16">
       |  <path fill-rule="evenodd" d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5z"/>
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
       |<svg
       |   width="16"
       |   height="16"
       |   fill="currentColor"
       |   class="bi bi-pentagon"
       |   viewBox="0 0 16 16"
       |   version="1.1"
       |   id="svg1"
       |   xmlns="http://www.w3.org/2000/svg"
       |   xmlns:svg="http://www.w3.org/2000/svg">
       |  <defs
       |     id="defs1" />
       |  <path
       |     d="m 7.685,1.545 a 0.5,0.5 0 0 1 0.63,0 l 6.263,5.088 a 0.5,0.5 0 0 1 0.161,0.539 l -2.362,7.479 A 0.5,0.5 0 0 1 11.901,15 H 4.099 A 0.5,0.5 0 0 1 3.623,14.65 L 1.26,7.173 A 0.5,0.5 0 0 1 1.421,6.633 L 7.684,1.546 Z m 8.213,5.28 A 0.5,0.5 0 0 0 15.736,6.285 L 8.316,0.257 a 0.5,0.5 0 0 0 -0.631,0 L 0.264,6.286 A 0.5,0.5 0 0 0 0.102,6.824 L 2.89,15.651 A 0.5,0.5 0 0 0 3.366,16 h 9.268 a 0.5,0.5 0 0 0 0.476,-0.35 l 2.788,-8.826 z"
       |     id="path1" />
       |  <path
       |
       |     d="m 5.7795212,11.06922 2.95638,-4.0840056 2.4027368,5.4866226 z"
       |     id="path2-5" />
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
  val handIndex:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-hand-index" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M6.75 1a.75.75 0 0 0-.75.75V9a.5.5 0 0 1-1 0v-.89l-1.003.2a.5.5 0 0 0-.399.546l.345 3.105a1.5 1.5 0 0 0 .243.666l1.433 2.15a.5.5 0 0 0 .416.223h6.385a.5.5 0 0 0 .434-.252l1.395-2.442a2.5 2.5 0 0 0 .317-.991l.272-2.715a1 1 0 0 0-.995-1.1H13.5v1a.5.5 0 0 1-1 0V7.154a4.208 4.208 0 0 0-.2-.26c-.187-.222-.368-.383-.486-.43-.124-.05-.392-.063-.708-.039a4.844 4.844 0 0 0-.106.01V8a.5.5 0 0 1-1 0V5.986c0-.167-.073-.272-.15-.314a1.657 1.657 0 0 0-.448-.182c-.179-.035-.5-.04-.816-.027l-.086.004V8a.5.5 0 0 1-1 0V1.75A.75.75 0 0 0 6.75 1zM8.5 4.466V1.75a1.75 1.75 0 0 0-3.5 0v5.34l-1.199.24a1.5 1.5 0 0 0-1.197 1.636l.345 3.106a2.5 2.5 0 0 0 .405 1.11l1.433 2.15A1.5 1.5 0 0 0 6.035 16h6.385a1.5 1.5 0 0 0 1.302-.756l1.395-2.441a3.5 3.5 0 0 0 .444-1.389l.272-2.715a2 2 0 0 0-1.99-2.199h-.582a5.184 5.184 0 0 0-.195-.248c-.191-.229-.51-.568-.88-.716-.364-.146-.846-.132-1.158-.108l-.132.012a1.26 1.26 0 0 0-.56-.642 2.634 2.634 0 0 0-.738-.288c-.31-.062-.739-.058-1.05-.046l-.048.002zm2.094 2.025z"/>
       |</svg>
       |""".stripMargin)

  //https://seekicon.com/free-icon/hand_4
  val hand: Icon = raw(
    s"""
       |<svg  width="1em" height="1em" fill="currentColor" viewBox="0 0 512 512" version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
       |	  style="enable-background:new 0 0 512 512;" xml:space="preserve">
       |<g>
       |	<path d="M435.1,102.4c-9.3,0-18.1,2.5-25.6,6.9V76.8c0-28.2-23-51.2-51.2-51.2c-10.6,0-20.3,3.2-28.5,8.7
       |		c-7-19.9-26-34.3-48.3-34.3c-22.3,0-41.3,14.3-48.3,34.3c-8.2-5.5-18-8.7-28.5-8.7c-28.2,0-51.2,23-51.2,51.2v188.8l-34.5-59.7
       |		c-6.7-12.2-17.6-20.8-30.7-24.4c-12.8-3.5-26.1-1.6-37.4,5.2c-23.2,13.9-32.1,45.4-19.7,70.2c0.8,1.6,17.1,35,68.1,137
       |		c24,48,50.4,82.3,78.3,102c22,15.4,37.1,16.2,39.9,16.2h128c21.8,0,42-7.1,60.2-21c17.1-13.1,31.9-32.1,44-56.4
       |		c23.9-47.8,36.6-114,36.6-191.3v-89.6C486.4,125.3,463.4,102.4,435.1,102.4L435.1,102.4z M460.7,243.2
       |		c0,73.4-11.7,135.6-33.8,179.9c-14.4,28.9-40.3,63.3-81.4,63.3H217.8c-1-0.1-11.8-1.3-28-13.4c-16.2-12.1-41.1-37.7-67.5-90.6
       |		c-51.9-103.8-67.9-136.5-68-136.8l0-0.1c-6.4-12.9-1.9-29.8,10-36.8c5.3-3.2,11.5-4,17.5-2.4c6.4,1.7,11.7,6,15,12.1l0.2,0.3
       |		l40,69.3c8.2,14.9,17.4,21.2,27.3,18.7c10-2.5,15-12.5,15-29.6v-200c0-14.1,11.5-25.6,25.6-25.6c14.1,0,25.6,11.5,25.6,25.6v166.4
       |		c0,7.1,5.7,12.8,12.8,12.8c7.1,0,12.8-5.7,12.8-12.8v-192c0-14.1,11.5-25.6,25.6-25.6c14.1,0,25.6,11.5,25.6,25.6v192
       |		c0,7.1,5.7,12.8,12.8,12.8c7.1,0,12.8-5.7,12.8-12.8V76.8c0-14.1,11.5-25.6,25.6-25.6c14.1,0,25.6,11.5,25.6,25.6v192
       |		c0,7.1,5.7,12.8,12.8,12.8c7.1,0,12.8-5.7,12.8-12.8V153.6c0-14.1,11.5-25.6,25.6-25.6c14.1,0,25.6,11.5,25.6,25.6L460.7,243.2
       |		L460.7,243.2z"/>
       |</g>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/check/
  val check:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-check" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M10.97 4.97a.75.75 0 0 1 1.071 1.05l-3.992 4.99a.75.75 0 0 1-1.08.02L4.324 8.384a.75.75 0 1 1 1.06-1.06l2.094 2.093 3.473-4.425a.236.236 0 0 1 .02-.022z"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/plus-circle/
  val plus:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-plus-circle" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |    <path fill-rule="evenodd" d="M8 15A7 7 0 1 0 8 1a7 7 0 0 0 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/>
       |    <path fill-rule="evenodd" d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
       |  </svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/plus-circle-fill/
  val plusFill:Icon = raw(
    s"""
       |<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-plus-circle-fill" viewBox="0 0 16 16">
       |  <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM8.5 4.5a.5.5 0 0 0-1 0v3h-3a.5.5 0 0 0 0 1h3v3a.5.5 0 0 0 1 0v-3h3a.5.5 0 0 0 0-1h-3v-3z"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/dash-circle-fill/
  val minusFill:Icon = raw(
    s"""
       |<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-dash-circle-fill" viewBox="0 0 16 16">
       |  <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM4.5 7.5a.5.5 0 0 0 0 1h7a.5.5 0 0 0 0-1h-7z"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/files/
  val duplicate:Icon = raw(
    s"""
       |<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-files" viewBox="0 0 16 16">
       |  <path d="M13 0H6a2 2 0 0 0-2 2 2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2 2 2 0 0 0 2-2V2a2 2 0 0 0-2-2zm0 13V4a2 2 0 0 0-2-2H5a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1zM3 4a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4z"/>
       |</svg>
       |""".stripMargin)


  //https://icons.getbootstrap.com/icons/check/
  val location:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-cursor" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path fill-rule="evenodd" d="M14.082 2.182a.5.5 0 0 1 .103.557L8.528 15.467a.5.5 0 0 1-.917-.007L5.57 10.694.803 8.652a.5.5 0 0 1-.006-.916l12.728-5.657a.5.5 0 0 1 .556.103zM2.25 8.184l3.897 1.67a.5.5 0 0 1 .262.263l1.67 3.897L12.743 3.52 2.25 8.184z"/>
       |</svg>
       |""".stripMargin)

  //http://simpleicon.com/wp-content/uploads/target1.svg
  val target:Icon = raw(
    s"""
       |<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1em" height="1em" viewBox="0 0 512 512">
       |	<path d="M384.645 256c0 70.942-57.702 128.655-128.655 128.655-70.942 0-128.655-57.712-128.655-128.655 0-70.912 57.723-128.635 128.655-128.635 70.963 0 128.655 57.713 128.655 128.635zM255.99 98.969c-86.558 0-157.020 70.42-157.020 157.031s70.462 157.041 157.020 157.041c86.579 0 157.061-70.441 157.061-157.041 0-86.61-70.482-157.030-157.061-157.030z" fill="currentColor" />
       |	<path d="M270.192 216.75v-206.428h-28.395v206.418c4.485-1.607 9.196-2.631 14.203-2.631s9.707 1.024 14.192 2.642z"  fill="currentColor" />
       |	<path d="M241.787 295.26v206.418h28.395v-206.418c-4.485 1.608-9.195 2.642-14.203 2.642s-9.708-1.034-14.192-2.642z" fill="currentColor" />
       |	<path d="M216.73 241.808h-212.121v28.375h212.121c-1.618-4.454-2.642-9.185-2.642-14.213 0-4.976 1.024-9.718 2.642-14.162z"  fill="currentColor"/>
       |	<path d="M507.392 241.808h-212.142c1.618 4.444 2.642 9.175 2.642 14.172 0 5.018-1.024 9.758-2.642 14.213h212.142v-28.386z" fill="currentColor" />
       |</svg>
       |""".stripMargin
  )

  //https://icons.getbootstrap.com/icons/map/
  val map:Icon = raw(
    s"""
       |<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-map" viewBox="0 0 16 16">
       |  <path fill-rule="evenodd" d="M15.817.113A.5.5 0 0 1 16 .5v14a.5.5 0 0 1-.402.49l-5 1a.502.502 0 0 1-.196 0L5.5 15.01l-4.902.98A.5.5 0 0 1 0 15.5v-14a.5.5 0 0 1 .402-.49l5-1a.5.5 0 0 1 .196 0L10.5.99l4.902-.98a.5.5 0 0 1 .415.103zM10 1.91l-4-.8v12.98l4 .8V1.91zm1 12.98l4-.8V1.11l-4 .8v12.98zm-6-.8V1.11l-4 .8v12.98l4-.8z"/>
       |</svg>
       |""".stripMargin
  )



  //https://icons.getbootstrap.com/icons/caret-down-fill/
  val caretDown:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-caret-down-fill" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path d="M7.247 11.14L2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z"/>
       |</svg>
       |""".stripMargin)

  //https://icons.getbootstrap.com/icons/caret-right-fill/
  val caretRight:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-caret-right-fill" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path d="M12.14 8.753l-5.482 4.796c-.646.566-1.658.106-1.658-.753V3.204a1 1 0 0 1 1.659-.753l5.48 4.796a1 1 0 0 1 0 1.506z"/>
       |</svg>
       |""".stripMargin)

  val asc:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-caret-right-fill rotate-right" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path d="M1 11a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1v-3zm5-4a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V7zm5-5a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V2z"/>
       |</svg>
       |""".stripMargin
  )

  val desc:Icon = raw(
    s"""
       |<svg width="1em" height="1em" viewBox="0 0 16 16" class="bi bi-caret-right-fill rotate-left" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
       |  <path d="M1 11a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1v-3zm5-4a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V7zm5-5a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V2z"/>
       |</svg>
       |""".stripMargin
  )

  //https://icons.getbootstrap.com/icons/cloud-arrow-down-fill/
  val download:Icon = raw(
    s"""
       |<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-cloud-arrow-down-fill" viewBox="0 0 16 16">
       |  <path d="M8 2a5.53 5.53 0 0 0-3.594 1.342c-.766.66-1.321 1.52-1.464 2.383C1.266 6.095 0 7.555 0 9.318 0 11.366 1.708 13 3.781 13h8.906C14.502 13 16 11.57 16 9.773c0-1.636-1.242-2.969-2.834-3.194C12.923 3.999 10.69 2 8 2zm2.354 6.854-2 2a.5.5 0 0 1-.708 0l-2-2a.5.5 0 1 1 .708-.708L7.5 9.293V5.5a.5.5 0 0 1 1 0v3.793l1.146-1.147a.5.5 0 0 1 .708.708z"/>
       |</svg>
       |""".stripMargin
  )

  //https://icons.getbootstrap.com/icons/cloud-arrow-up-fill/
  val upload:Icon = raw(
    s"""
       |<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-cloud-arrow-up-fill" viewBox="0 0 16 16">
       |  <path d="M8 2a5.53 5.53 0 0 0-3.594 1.342c-.766.66-1.321 1.52-1.464 2.383C1.266 6.095 0 7.555 0 9.318 0 11.366 1.708 13 3.781 13h8.906C14.502 13 16 11.57 16 9.773c0-1.636-1.242-2.969-2.834-3.194C12.923 3.999 10.69 2 8 2zm2.354 5.146a.5.5 0 0 1-.708.708L8.5 6.707V10.5a.5.5 0 0 1-1 0V6.707L6.354 7.854a.5.5 0 1 1-.708-.708l2-2a.5.5 0 0 1 .708 0l2 2z"/>
       |</svg>
       |""".stripMargin
  )

  //https://icons.getbootstrap.com/icons/file-earmark-check/
  def fileOk(size:Int):Icon = raw(
    SVG.iconOk(size)
  )


}
