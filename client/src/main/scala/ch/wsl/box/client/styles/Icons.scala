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

  val target2:Icon = raw(
    s"""
       |<svg  width="1em" height="1em" version="1.1"  xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
       |	 viewBox="0 0 297 297"  xml:space="preserve">
       |<g>
       |	<path d="M148.5,129.953c-10.227,0-18.547,8.319-18.547,18.547s8.319,18.547,18.547,18.547s18.547-8.319,18.547-18.547
       |		S158.727,129.953,148.5,129.953z M148.5,149.72c-0.673,0-1.22-0.547-1.22-1.22s0.547-1.22,1.22-1.22s1.22,0.547,1.22,1.22
       |		S149.173,149.72,148.5,149.72z" fill="currentColor"/>
       |	<path d="M287.116,138.616h-25.204c-4.744-54.905-48.623-98.784-103.528-103.527V9.884c0-5.458-4.426-9.884-9.884-9.884
       |		s-9.884,4.426-9.884,9.884v25.205C83.711,39.832,39.832,83.711,35.088,138.616H9.884C4.426,138.616,0,143.042,0,148.5
       |		s4.426,9.884,9.884,9.884h25.204c4.744,54.905,48.623,98.784,103.528,103.527v25.205c0,5.458,4.426,9.884,9.884,9.884
       |		s9.884-4.426,9.884-9.884v-25.205c54.905-4.743,98.784-48.622,103.528-103.527h25.204c5.458,0,9.884-4.426,9.884-9.884
       |		S292.574,138.616,287.116,138.616z M242.061,138.616h-32.495c-4.227-26.209-24.973-46.954-51.182-51.182V54.939
       |		C202.382,59.554,237.445,94.618,242.061,138.616z M183.154,158.384h6.26c-3.692,15.285-15.745,27.338-31.03,31.03v-6.26
       |		c0-5.458-4.426-9.884-9.884-9.884s-9.884,4.426-9.884,9.884v6.26c-15.285-3.692-27.338-15.745-31.03-31.03h6.26
       |		c5.458,0,9.884-4.426,9.884-9.884s-4.426-9.884-9.884-9.884h-6.26c3.692-15.285,15.745-27.338,31.03-31.03v6.26
       |		c0,5.458,4.426,9.884,9.884,9.884s9.884-4.426,9.884-9.884v-6.26c15.285,3.692,27.338,15.745,31.03,31.03h-6.26
       |		c-5.458,0-9.884,4.426-9.884,9.884S177.696,158.384,183.154,158.384z M138.616,54.939v32.495
       |		c-26.209,4.228-46.954,24.973-51.182,51.182H54.939C59.555,94.618,94.618,59.554,138.616,54.939z M54.939,158.384h32.495
       |		c4.228,26.21,24.973,46.954,51.182,51.182v32.495C94.618,237.446,59.555,202.382,54.939,158.384z M158.384,242.061v-32.495
       |		c26.209-4.227,46.954-24.972,51.182-51.182h32.495C237.445,202.382,202.382,237.446,158.384,242.061z" fill="currentColor"/>

       |</svg>
       |
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


}
