package catgirlroutes.utils.render.font

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Font
import kotlin.math.max

class CFontRenderer(font: Font) : CFont(font, true, false) {

    private val boldChars = arrayOfNulls<CharData>(256)
    private val italicChars = arrayOfNulls<CharData>(256)
    private val boldItalicChars = arrayOfNulls<CharData>(256)
    private val colorCode = IntArray(32)
    private val colourCodes = "0123456789abcdefklmnor"
    private lateinit var texBold: DynamicTexture
    private lateinit var texItalic: DynamicTexture
    private lateinit var texItalicBold: DynamicTexture

    init {
        setupMinecraftColourCodes()
        setupBoldItalicIDs()
    }

    fun drawStringWithShadow(text: String, x: Double, y: Double, color: Int): Int {
        val shadowWidth = drawString(text, x + 0.5, y + 0.8999999761581421, Color(20, 20, 20).rgb, true, 8.3f)
        return max(shadowWidth, drawString(text, x, y, color, false, 8.3f)).toInt()
    }

    fun drawString(text: String, x: Double, y: Double, color: Int, shadow: Boolean) {
        if (shadow) drawStringWithShadow(text, x, y, color)
        else drawString(text, x, y, color, false, 8.3f)
    }

    fun drawString(text: String, x: Double, y: Double, color: Int, shadow: Boolean, kerning: Float): Float {
        var x1 = (x - 1) * 2
        val y1 = (y - 2) * 2
        if (text.isEmpty()) return 0.0f

        var colour = Color(color)
        if (colour.red == 255 && colour.green == 255 && colour.blue == 255 && colour.alpha == 32) colour = Color(255, 255, 255)
        if (colour.alpha < 4) colour = Color(colour.red, colour.blue, colour.green, 255)
        if (shadow) colour = Color(colour.red / 4, colour.green / 4, colour.blue / 4, colour.alpha)

        var currentData = charData
        var bold = false
        var italic = false
        var strikethrough = false
        var underline = false

        GL11.glPushMatrix()
        GlStateManager.scale(0.5, 0.5, 0.5)
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(770, 771)
        GlStateManager.color(colour.red / 255f, colour.green / 255f, colour.blue / 255f, colour.alpha / 255f)
        GlStateManager.enableTexture2D()
        GlStateManager.bindTexture(tex.glTextureId)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.glTextureId)

        var index = 0
        while (index < text.length) {
            val character = text[index]
            if (character == '\u00A7') {
                var colorIndex = 21
                try {
                    colorIndex = colourCodes.indexOf(text[index + 1])
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (colorIndex < 16) {
                    bold = false
                    italic = false
                    underline = false
                    strikethrough = false
                    GlStateManager.bindTexture(tex.glTextureId)
                    currentData = charData
                    val adjustedIndex = if (colorIndex < 0) 15 else colorIndex
                    val codeColor = colorCode[if (shadow) adjustedIndex + 16 else adjustedIndex]
                    GlStateManager.color(
                        (codeColor shr 16 and 0xFF) / 255.0f,
                        (codeColor shr 8 and 0xFF) / 255.0f,
                        (codeColor and 0xFF) / 255.0f,
                        colour.alpha.toFloat()
                    )
                } else if (colorIndex == 17) {
                    bold = true
                    currentData = if (italic) {
                        GlStateManager.bindTexture(texItalicBold.glTextureId)
                        boldItalicChars
                    } else {
                        GlStateManager.bindTexture(texBold.glTextureId)
                        boldChars
                    }
                } else if (colorIndex == 18) {
                    strikethrough = true
                } else if (colorIndex == 19) {
                    underline = true
                } else if (colorIndex == 20) {
                    italic = true
                    currentData = if (bold) {
                        GlStateManager.bindTexture(texItalicBold.glTextureId)
                        boldItalicChars
                    } else {
                        GlStateManager.bindTexture(texItalic.glTextureId)
                        italicChars
                    }
                } else {
                    bold = false
                    italic = false
                    underline = false
                    strikethrough = false
                    GlStateManager.color(colour.red / 255f, colour.green / 255f, colour.blue / 255f, colour.alpha / 255f)
                    GlStateManager.bindTexture(tex.glTextureId)
                    currentData = charData
                }
                index += 2
            } else {
                if (character.code < currentData.size) {
                    GL11.glBegin(GL11.GL_TRIANGLES)
                    drawChar(currentData, character, x1.toFloat(), y1.toFloat())
                    GL11.glEnd()

                    if (strikethrough) {
                        drawLine(
                            x1,
                            y1 + currentData[character.code]!!.height / 2.0,
                            x1 + currentData[character.code]!!.width - 8.0,
                            y1 + currentData[character.code]!!.height / 2.0,
                            1.0f
                        )
                    }
                    if (underline) {
                        drawLine(
                            x1,
                            y1 + currentData[character.code]!!.height - 2.0,
                            x1 + currentData[character.code]!!.width - 8.0,
                            y1 + currentData[character.code]!!.height - 2.0,
                            1.0f
                        )
                    }
                    x1 += currentData[character.code]!!.width - kerning + charOffset
                }
                index++
            }
        }

        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST)
        GL11.glPopMatrix()
        return x1.toFloat() / 2.0f
    }

    fun getStringWidth(text: String?): Double {
        if (text == null) {
            return 0.0
        }

        var width = 0.0f
        val currentData = charData

        var index = 0
        while (index < text.length) {
            val character = text[index]
            if (character == '\u00A7') {
                index++
            } else if (character.code < currentData.size) {
                width += currentData[character.code]!!.width - 8.3f + charOffset
            }
            index++
        }

        return (width / 2.0f).toDouble()
    }

    private fun setupBoldItalicIDs() {
        texBold = setupTexture(font.deriveFont(Font.BOLD), antiAlias, fractionalMetrics, boldChars)!!
        texItalic = setupTexture(font.deriveFont(Font.ITALIC), antiAlias, fractionalMetrics, italicChars)!!
        texItalicBold = setupTexture(font.deriveFont(Font.BOLD or Font.ITALIC), antiAlias, fractionalMetrics, boldItalicChars)!!
    }

    private fun drawLine(x2: Double, y2: Double, x1: Double, y1: Double, width: Float) {
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glLineWidth(width)
        GL11.glBegin(GL11.GL_LINES)
        GL11.glVertex2d(x2, y2)
        GL11.glVertex2d(x1, y1)
        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
    }

    private fun setupMinecraftColourCodes() {
        for (index in 0 until 32) {
            val noClue = (index shr 3 and 0x1) * 85
            var red = (index shr 2 and 0x1) * 170 + noClue
            var green = (index shr 1 and 0x1) * 170 + noClue
            var blue = (index and 0x1) * 170 + noClue

            if (index == 6) red += 85
            if (index >= 16) {
                red /= 4
                green /= 4
                blue /= 4
            }
            colorCode[index] = (red and 0xFF shl 16) or (green and 0xFF shl 8) or (blue and 0xFF)
        }
    }
}