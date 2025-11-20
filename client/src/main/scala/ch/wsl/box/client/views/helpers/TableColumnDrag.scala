package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.services.ClientConf
import org.scalajs.dom
import org.scalajs.dom.{DataTransferDropEffectKind, DataTransferEffectAllowedKind, DragEvent, Element, Event, HTMLTableElement, document, html}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.JSConverters._

class TableColumnDrag(table:Element,_extractor: dom.HTMLElement => String,_onDrop: DragEvent => Unit) {



  // -----------------------------------------------------------------
  // Helper to create the “ghost” table used as drag image
  // -----------------------------------------------------------------
  private def createDragGhost(col: html.TableCell): html.Element = {
    // container table
    val dragGhost = document.createElement("table").asInstanceOf[html.Table]
    table.classList.foreach(dragGhost.classList.add)
    dragGhost.classList.add("tableGhost")

    // keep original column width
    val srcStyle = dom.window.getComputedStyle(col)
    dragGhost.style.width = srcStyle.getPropertyValue("width")

    // ----- thead (clone of the header cell) -----
    val theadGhost = document.createElement("thead").asInstanceOf[html.TableSection]
    val headerClone = col.cloneNode(deep = true).asInstanceOf[html.TableCell]
    headerClone.style.backgroundColor = "gray"
    theadGhost.appendChild(headerClone)
    dragGhost.appendChild(theadGhost)

    // ----- tbody (clone of the column cells) -----
    val srcIndex  = table.querySelectorAll("th").indexWhere(th => th.innerHTML == col.innerHTML) + 2
    val tbodyGhost = document.createElement("tbody").asInstanceOf[html.TableSection]

    table.querySelectorAll("tr td:nth-child(" + srcIndex + ")").foreach {  td =>
      val tr  = document.createElement("tr").asInstanceOf[html.TableRow]
      val tdC = document.createElement("td").asInstanceOf[html.TableCell]
      tdC.innerHTML = td.innerHTML
      td.classList.foreach(tdC.classList.add)
      tr.appendChild(tdC)
      tbodyGhost.appendChild(tr)
    }

    dragGhost.appendChild(tbodyGhost)

    // hide off‑screen (required for drag image)
    dragGhost.style.position = "absolute"
    dragGhost.style.top      = "-1500px"
    document.body.appendChild(dragGhost)

    dragGhost
  }

  // -----------------------------------------------------------------
  // Event listeners (mirroring the original JavaScript)
  // -----------------------------------------------------------------
  private def onDragStart(e: DragEvent): Unit = {
    val target = e.target.asInstanceOf[html.TableCell]
    e.dataTransfer.effectAllowed = DataTransferEffectAllowedKind.move
    e.dataTransfer.setData("text", _extractor(target))


    val ghost = createDragGhost(target)
    e.dataTransfer.setDragImage(ghost, 0, 0)

  }

  private def onDragOver(e: DragEvent): Unit = {
    e.preventDefault()
    e.dataTransfer.dropEffect = DataTransferDropEffectKind.move
  }

  private def onDragEnter(e: Event): Unit = {
    table.querySelectorAll("th").foreach { col =>
      col.classList.remove(ClientConf.style.thOver.className.value)
    }
    e.currentTarget.asInstanceOf[dom.HTMLElement].closest("th").classList.add(ClientConf.style.thOver.className.value)
    e.stopPropagation()
  }

//  private def onDragLeave(e: Event): Unit = {
//    e.currentTarget.asInstanceOf[dom.HTMLElement].closest("th").classList.remove(ClientConf.style.thOver.className.value)
//    e.stopPropagation()
//  }

  private def onDrop(e: DragEvent): Unit = {
    e.preventDefault()
    e.stopPropagation()

    _onDrop(e)

  }

  private def onDragEnd(e: Event): Unit = {
    table.querySelectorAll("th").foreach { col =>
      col.classList.remove(ClientConf.style.thOver.className.value)
      col.asInstanceOf[dom.HTMLElement].style.opacity = "1"
    }
  }




  table.querySelectorAll("th").foreach  {  col =>
      val el = col.asInstanceOf[html.Element]
      el.addEventListener("dragstart",  onDragStart _ , false)
      el.addEventListener("dragenter",  onDragEnter _ , false)
      el.addEventListener("dragover",   onDragOver _  , false)
//      el.addEventListener("dragleave", onDragLeave _ , false)
      el.addEventListener("drop",       onDrop _      , false)
      el.addEventListener("dragend",    onDragEnd _   , false)
    }


}