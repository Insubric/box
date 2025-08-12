package ch.wsl.box.rest.utils


//https://github.com/Xavi-PL/GradientLogoMaker/blob/main/src/GradientLogoMaker.java

import javax.imageio._
import java.awt.image._
import java.awt._
import java.io._


object IconGenerator {
  def drawImage(size: Int, startColor: Color, endColor: Color, text: String, darkTheme: Boolean,fontSize:Int): BufferedImage = {
    val gp = new GradientPaint(0, 0, startColor, size, size, endColor)
    val bim = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val qualityHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    qualityHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    // Draw the gradient
    val g2 = bim.createGraphics
    g2.setPaint(gp)
    g2.setRenderingHints(qualityHints)
    g2.fillRoundRect(0, 0, size, size, 0, 0)
    // Draw the text
    g2.setColor(if (darkTheme) Color.WHITE
    else Color.BLACK)
    g2.setFont(new Font("Roboto", Font.PLAIN, fontSize))
    val metrics = g2.getFontMetrics
    val x = size / 2 - (metrics.stringWidth(text) / 2)
    val y = size / 2 + (metrics.getAscent / 3)
    g2.drawString(text, x, y)
    g2.dispose
    bim
  }

  def withName(name:String,mainColor:String,size:Int = 512,fontSize:Int = 200): Array[Byte] = {
      val gradientLogo = drawImage(size, Color.decode(mainColor), Color.decode(mainColor), name, true,fontSize)
      val byteArrayOutputStream = new ByteArrayOutputStream()
      ImageIO.write(gradientLogo, "PNG",byteArrayOutputStream)
      val result = byteArrayOutputStream.toByteArray
      byteArrayOutputStream.close()
      result
  }
}