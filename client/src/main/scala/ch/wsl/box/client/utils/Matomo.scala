package ch.wsl.box.client.utils


import ch.wsl.box.client.services.BrowserConsole
import org.scalajs.dom._

import scala.scalajs.js

object Matomo {

  /*

  <!-- Matomo -->
<script>
  var _paq = window._paq = window._paq || [];
  /* tracker methods like "setCustomDimension" should be called before "trackPageView" */
  _paq.push(['trackPageView']);
  _paq.push(['enableLinkTracking']);
  (function() {
    var u="//webapps.wsl.ch/";
    _paq.push(['setTrackerUrl', u+'matomo.php']);
    _paq.push(['setSiteId', '1']);
    var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
    g.async=true; g.src=u+'matomo.js'; s.parentNode.insertBefore(g,s);
  })();
</script>
<!-- End Matomo Code -->

   */
  def load(base:String,site_id:String) = {
    val _paq = js.Array(
      js.Array("trackPageView"),
      js.Array("enableLinkTracking"),
      js.Array("setTrackerUrl",s"${base}matomo.php"),
      js.Array("setSiteId",site_id)
    )
    BrowserConsole.log(_paq)
    window.asInstanceOf[js.Dynamic]._paq = _paq
    val s = document.createElement("script").asInstanceOf[HTMLScriptElement]
    s.async = true
    s.src = s"${base}matomo.js"
    val head = document.getElementsByTagName("head").item(0)
    head.appendChild(s)
  }
}
