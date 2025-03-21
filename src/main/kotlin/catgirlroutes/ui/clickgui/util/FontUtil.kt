package catgirlroutes.ui.clickgui.util

import catgirlroutes.CatgirlRoutes.Companion.RESOURCE_DOMAIN
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.impl.render.ClickGui
import catgirlroutes.utils.Utils.noControlCodes
import catgirlroutes.utils.render.font.CFontRenderer
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.*


/**
 * Provides methods for rending text.
 *
 * @author Aton
 */
object FontUtil {
    private lateinit var fontRenderer: FontRenderer
    private lateinit var customFontRenderer: CFontRenderer

    val fontHeight: Int get() = fontRenderer.FONT_HEIGHT

    private val font: Boolean get() = ClickGui.customFont.enabled

    fun setupFontUtils() {
        val stream = mc.resourceManager.getResource(ResourceLocation(RESOURCE_DOMAIN, "Roboto-Regular.ttf")).inputStream
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val font = Font.createFont(0, stream)
        ge.registerFont(font)

        customFontRenderer = CFontRenderer(font.deriveFont(0, 19f))
        fontRenderer = mc.fontRendererObj
    }

    private fun drawText(
        text: String,
        x: Double,
        y: Double,
        color: Int,
        customFont: Boolean,
        scale: Double,
        shadow: Boolean = false
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0.0)
        GlStateManager.scale(scale, scale, 1.0)

        if (customFont) customFontRenderer.drawString(text, 0.0, 0.0, color, shadow)
        else fontRenderer.drawString(text, 0f, 0f, color, shadow)

        GlStateManager.popMatrix()
    }

    fun getStringWidth(text: String, customFont: Boolean = font, scale: Double = 1.0): Int {
        val width = if (customFont) customFontRenderer.getStringWidth(text.noControlCodes) else fontRenderer.getStringWidth(text.noControlCodes)
        return (width.toInt() * scale).toInt()
    }

    fun getStringWidthDouble(text: String, customFont: Boolean = font, scale: Double = 1.0): Double {
        val width = if (customFont) customFontRenderer.getStringWidth(text.noControlCodes) else fontRenderer.getStringWidth(text.noControlCodes)
        return width.toDouble() * scale
    }

    fun getSplitHeight(text: String, wrapWidth: Int): Int {
        var dy = 0
        for (s in mc.fontRendererObj.listFormattedStringToWidth(text, wrapWidth)) {
            dy += mc.fontRendererObj.FONT_HEIGHT
        }
        return dy
    }

    fun getScaledFontHeight(scale: Double = 1.0): Int = (fontHeight * scale).toInt()

    fun drawString(text: String, x: Double, y: Double, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        drawText(text, x, y, color, customFont, scale)

    fun drawString(text: String, x: Int, y: Int, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        drawString(text, x.toDouble(), y.toDouble(), color, customFont, scale)

    fun drawStringWithShadow(text: String, x: Double, y: Double, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        drawText(text, x, y, color, customFont, scale, true)

    fun drawCenteredString(text: String, x: Double, y: Double, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        drawText(text, x - getStringWidth(text, customFont) * scale / 2, y, color, customFont, scale)

    fun drawCenteredStringWithShadow(text: String, x: Double, y: Double, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        drawText(text, x - getStringWidth(text, customFont) * scale / 2, y, color, customFont, scale, true)

    fun drawTotalCenteredString(text: String, x: Double, y: Double, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        drawText(text, x - getStringWidth(text, customFont) * scale / 2, y - getScaledFontHeight(scale) / 2, color, customFont, scale)

    fun drawTotalCenteredStringWithShadow(text: String, x: Double, y: Double, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        drawText(text, x - getStringWidth(text, customFont) * scale / 2, y - getScaledFontHeight(scale) / 2, color, customFont, scale, true)

    fun drawWrappedText(text: String, x: Double, y: Double, width: Double, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        wrapText(text, width, customFont, scale).forEachIndexed { i, line -> drawText(line, x, y + getScaledFontHeight(scale) * i, color, customFont, scale) }

    fun drawWrappedText(text: String, x: Int, y: Int, width: Int, color: Int = ColorUtil.textcolor, customFont: Boolean = font, scale: Double = 1.0) =
        drawWrappedText(text, x.toDouble(), y.toDouble(), width.toDouble(), color, customFont, scale)

    fun wrapText(text: String, maxWidth: Double, customFont: Boolean = font, scale: Double = 1.0): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""

        text.split(" ").forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (this.getStringWidthDouble(testLine, customFont, scale) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    /**
     * Returns a copy of the String where the first letter is capitalized.
     */
    fun String.forceCapitalize(): String = this.substring(0, 1).uppercase(Locale.getDefault()) + this.substring(1, this.length)

    /**
     * Returns a copy of the String where the only first letter is capitalized.
     */
    fun String.capitalizeOnlyFirst(): String = this.substring(0, 1).uppercase(Locale.getDefault()) + this.substring(1, this.length).lowercase()
}