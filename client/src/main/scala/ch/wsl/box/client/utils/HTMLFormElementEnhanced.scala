package ch.wsl.box.client.utils

import org.scalajs.dom.raw.HTMLFormElement

import scala.scalajs.js



object HTMLFormElementExtension {
  implicit class HTMLFormElementExt(form:HTMLFormElement) {
    def reportValidity(): Boolean = form.asInstanceOf[js.Dynamic].reportValidity().asInstanceOf[Boolean]
  }
}


