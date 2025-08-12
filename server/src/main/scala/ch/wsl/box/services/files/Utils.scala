package ch.wsl.box.services.files

import ch.wsl.box.shared.utils.SVG
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.batik.transcoder.{TranscoderInput, TranscoderOutput}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

object Utils {
  def placeholderOk:Array[Byte] = {

    val istream = new ByteArrayInputStream(SVG.iconOk(50).getBytes());
    val svg = new TranscoderInput(istream)
    val png = new ByteArrayOutputStream()
    val output_png_image = new TranscoderOutput(png)

    new PNGTranscoder().transcode(svg, output_png_image)
    val result = png.toByteArray
    png.flush()
    png.close()
    istream.close()
    result

  }
}
