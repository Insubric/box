package ch.wsl.box.client.utils

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import scribe.Logging
import yamusca.context.Value
import yamusca.data.{Section, Template, Variable}
import yamusca.imports.{Context, mustache}

object MustacheUtils extends Logging {
  def context(tmpl:Template,data:Json):Context = {
    val variables = tmpl.els.flatMap {
      case Variable(key, _) => Some(key)
      case Section(key, _, _) => Some(key)
      case _ => None
    }

    val values = variables.map { v =>
      v -> data.js(v).toMustacheValue
    } ++ Seq(
      "BASE_URI" -> Value.of(Routes.baseUri),
      "FULL_URL" -> Value.of(Routes.fullUrl),
      "ORIGIN_URL" -> Value.of(Routes.originUrl)
    )
    Context(values: _*)

  }

  def render(template:String,data:Json) = {
    mustache.parse(template) match {
      case Left(err) => {
        logger.warn(err._2)
        template
      }
      case Right(tmpl) => mustache.render(tmpl)(context(tmpl, data))
    }
  }
}
