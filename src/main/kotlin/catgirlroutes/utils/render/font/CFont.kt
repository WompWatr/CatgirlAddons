package catgirlroutes.utils.render.font

import net.minecraft.client.renderer.texture.DynamicTexture
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

open class CFont(font: Font, antiAlias: Boolean, fractionalMetrics: Boolean) {
    private var imgSize = 4096.0f
    var charData: Array<CharData?> = arrayOfNulls(256)
    var font: Font = font
        set(value) {
            field = value
            tex = setupTexture(value, antiAlias, fractionalMetrics, charData)!!
        }
    var antiAlias: Boolean = antiAlias
        set(value) {
            if (field != value) {
                field = value
                tex = setupTexture(font, value, fractionalMetrics, charData)!!
            }
        }
    var fractionalMetrics: Boolean = fractionalMetrics
        set(value) {
            if (field != value) {
                field = value
                tex = setupTexture(font, antiAlias, value, charData)!!
            }
        }
    var fontHeight: Int = -1
    var charOffset: Int = 0
    var tex: DynamicTexture

    init {
        tex = setupTexture(font, antiAlias, fractionalMetrics, charData)!!
    }

    fun setupTexture(font: Font, antiAlias: Boolean, fractionalMetrics: Boolean, chars: Array<CharData?>): DynamicTexture? {
        val img = generateFontImage(font, antiAlias, fractionalMetrics, chars)
        return try {
            DynamicTexture(img)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateFontImage(font: Font, antiAlias: Boolean, fractionalMetrics: Boolean, chars: Array<CharData?>): BufferedImage {
        val imgSize = imgSize.toInt()
        val bufferedImage = BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB)
        val graphics = bufferedImage.graphics as Graphics2D
        graphics.font = font
        graphics.color = Color(255, 255, 255, 0)
        graphics.fillRect(0, 0, imgSize, imgSize)
        graphics.color = Color.WHITE
        graphics.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS,
            if (fractionalMetrics) RenderingHints.VALUE_FRACTIONALMETRICS_ON else RenderingHints.VALUE_FRACTIONALMETRICS_OFF
        )
        graphics.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            if (antiAlias) RenderingHints.VALUE_TEXT_ANTIALIAS_ON else RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
        )
        val fontMetrics = graphics.fontMetrics
        var charHeight = 0
        var positionX = 0
        var positionY = 1

        for (index in chars.indices) {
            val c = index.toChar()
            val charDatum = CharData()
            val dimensions = fontMetrics.getStringBounds(c.toString(), graphics)
            charDatum.width = dimensions.bounds.width + 8
            charDatum.height = dimensions.bounds.height

            if (positionX + charDatum.width >= imgSize) {
                positionX = 0
                positionY += charHeight
                charHeight = 0
            }

            if (charDatum.height > charHeight) {
                charHeight = charDatum.height
            }

            charDatum.storedX = positionX
            charDatum.storedY = positionY

            if (charDatum.height > fontHeight) {
                fontHeight = charDatum.height
            }

            chars[index] = charDatum
            graphics.drawString(c.toString(), positionX + 2, positionY + fontMetrics.ascent)
            positionX += charDatum.width
        }

        return bufferedImage
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    fun drawChar(chars: Array<CharData?>, c: Char, x: Float, y: Float) {
        val charData = chars[c.code] ?: return
        drawQuad(x, y, charData.width.toFloat(), charData.height.toFloat(),
            charData.storedX.toFloat(), charData.storedY.toFloat(),
            charData.width.toFloat(), charData.height.toFloat())
    }

    private fun drawQuad(x2: Float, y2: Float, width: Float, height: Float,
                         srcX: Float, srcY: Float, srcWidth: Float, srcHeight: Float) {
        val renderSRCX = srcX / imgSize
        val renderSRCY = srcY / imgSize
        val renderSRCWidth = srcWidth / imgSize
        val renderSRCHeight = srcHeight / imgSize

        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY)
        GL11.glVertex2d((x2 + width).toDouble(), y2.toDouble())
        GL11.glTexCoord2f(renderSRCX, renderSRCY)
        GL11.glVertex2d(x2.toDouble(), y2.toDouble())
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight)
        GL11.glVertex2d(x2.toDouble(), (y2 + height).toDouble())
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight)
        GL11.glVertex2d(x2.toDouble(), (y2 + height).toDouble())
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight)
        GL11.glVertex2d((x2 + width).toDouble(), (y2 + height).toDouble())
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY)
        GL11.glVertex2d((x2 + width).toDouble(), y2.toDouble())
    }

    class CharData {
        var width = 0
        var height = 0
        var storedX = 0
        var storedY = 0
    }
}