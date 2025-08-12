package ch.wsl.box.rest.logic.functions
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter

import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

import java.util

object UtilsImpl extends RuntimeUtils {
  override def qrCode(url: String): String = {

    val barcodeWriter = new QRCodeWriter

    val hintMap = new util.HashMap[EncodeHintType,Any]()
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q)
    hintMap.put(EncodeHintType.MARGIN, -1)
    val bitMatrix = barcodeWriter.encode(url, BarcodeFormat.QR_CODE, 500, 500,hintMap)
    val os = new ByteArrayOutputStream()

    MatrixToImageWriter.writeToStream(bitMatrix,"jpg",os)
    val result = Base64.getEncoder.encodeToString(os.toByteArray)
    os.close()
    result
  }
}
