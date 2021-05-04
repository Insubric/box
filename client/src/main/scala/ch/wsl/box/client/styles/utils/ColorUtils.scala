package ch.wsl.box.client.styles.utils

import scalacss.internal.{Color, ValueT}
import scalacss.internal.Dsl.autoDslNumD


object ColorUtils {

  trait Color {
    def color: ValueT[ValueT.Color]
    def lighten(amount:Double):Color
    def darken(amount:Double):Color
  }
  case class HSL(hue:Double,saturation:Double,lightness:Double) extends Color {
    def color = Color.hsl((hue*360).toInt,(saturation*100).%%,(lightness*100).%%)

    override def toString: String = s"HSL - $hue,$saturation,$lightness - ${(hue*360).toInt},${(saturation*100).toInt},${(lightness*100).toInt}"

    def lighten(amount:Double) = {
      this.copy(lightness = this.lightness + (1-this.lightness)*amount)
    }

    def darken(amount:Double) = {
      this.copy(lightness = this.lightness - this.lightness*amount)
    }

  }
  case class RGB(r:Double,g:Double,b:Double) extends Color {


    override def toString: String = s"RGB - $r,$g,$b - ${(r*255).toInt},${(g*255).toInt},${(b*255).toInt}"

    def color = Color.rgb((r * 255).toInt,(g * 255).toInt,(b * 255).toInt)


    lazy val hsl:HSL = rgb2hsl(this)

    def lighten(amount:Double) = hsl.lighten(amount)
    def darken(amount:Double) = hsl.darken(amount)
  }

  object RGB{
    def fromHex(hex:String):RGB = {
      val plain = hex.stripPrefix("#")
      plain.length match {
        case 6 => RGB(
          Integer.valueOf(plain.substring(0,2),16).doubleValue() / 255.0,
          Integer.valueOf(plain.substring(2,4),16).doubleValue() / 255.0,
          Integer.valueOf(plain.substring(4,6),16).doubleValue() / 255.0
        )
        case 3 => RGB(
          Integer.valueOf(plain.substring(0,1)*2,16).doubleValue() / 255.0,
          Integer.valueOf(plain.substring(1,2)*2,16).doubleValue() / 255.0,
          Integer.valueOf(plain.substring(2,3)*2,16).doubleValue() / 255.0
        )
        case _ => throw new Exception(s"Color hex format not parsable $hex")
      }


    }
  }


  def rgb2hsl(rgb:RGB):HSL = {

    val min = Seq(rgb.r,rgb.g,rgb.b).min
    val max = Seq(rgb.r,rgb.g,rgb.b).max

    val delta = max - min

    val mean = (min + max)/2


    max==min match {
      case true => HSL(0,0,mean)
      case false => {

        val saturation = if(mean > 0.5) {
          delta / (2.0 - max - min)
        } else {
          delta / (max+min)
        }

        val hue = max match {
          case rgb.r => (rgb.g - rgb.b) / delta + {if(rgb.g > rgb.b) 6 else 0 }
          case rgb.g => (rgb.b - rgb.r) / delta + 2
          case rgb.b => (rgb.r - rgb.g) / delta + 4
        }

        HSL(
          hue = hue / 6,
          saturation = saturation,
          lightness = mean
        )
      }
    }
  }


  def hsv2rgb(hsl:HSL):RGB = {

    def hue2rgb(p:Double, q:Double, _t:Double):Double = {
      val t:Double = _t match {
        case x if x < 0 => x+1
        case x if x > 1 => x-1
        case x => x
      }

      t match {
        case x if x < 1.0/6 => p + (q - p) * 6 * x
        case x if x < 1.0/2 => q
        case x if x < 2.0/3 => p + (q - p) * (2.0/3 - x) * 6
      }

    }

    hsl.saturation match {
      case 0 => RGB(hsl.lightness,hsl.lightness,hsl.lightness)
      case _ => {
        val q = if(hsl.lightness < 0.5) hsl.lightness * (1 + hsl.saturation) else hsl.lightness + hsl.saturation - hsl.lightness*hsl.saturation
        val p = 2 * hsl.lightness - q

        RGB(
          hue2rgb(p, q, hsl.hue + 1/3),
          hue2rgb(p, q, hsl.hue),
          hue2rgb(p, q, hsl.hue - 1/3)
        )

      }
    }
  }

}
