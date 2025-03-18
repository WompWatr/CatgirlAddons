package catgirlroutes.ui.notification

import catgirlroutes.CatgirlRoutes.Companion.RESOURCE_DOMAIN
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.wrapText
//import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.utils.Notifications
import catgirlroutes.utils.render.HUDRenderUtils
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color

class Notification( // todo recode
    val title: String,
    val description: String,
    private val duration: Double,
    val type: NotificationType,
    val icon: String? = null
) {
    val startTime: Long = System.currentTimeMillis()
    val endTime: Long = startTime + duration.toLong()

    fun draw(x: Double, y: Double, width: Double, height: Double) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        HUDRenderUtils.renderRect(x, y, width, height, Color(ColorUtil.bgColor)) // bg
        HUDRenderUtils.renderRectBorder(x, y, width, height, 1.0, this.type.getColour()) // border

        //easing stuff
        val elapsed = System.currentTimeMillis() - startTime
        val rawProgress = (elapsed / duration).coerceIn(0.0, 1.0)
        val easedProgress = Notifications.Easing.easeOutQuad(rawProgress) //change function from Easing for different animations
        HUDRenderUtils.renderRect(x, y, width * easedProgress, height, this.type.getColour().darker())

        val iconOffset = icon?.let {
            val texture = ResourceLocation(RESOURCE_DOMAIN, it)
            GlStateManager.enableBlend()
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
            mc.textureManager.bindTexture(texture)
            Gui.drawModalRectWithCustomSizedTexture(
                x.toInt() + 3, (y + (height - 40) / 2).toInt(),
                0f, 0f, 40, 40, 40f, 40f
            )
            GlStateManager.disableBlend()
            43 // offset
        } ?: 0

        val lines = wrapText(this.description, width - 12 - iconOffset)
        val lineSpacing = fontHeight + 2
        val totalTextHeight = (lines.size + 1) * lineSpacing
        val textX = x + iconOffset + 3
        val textY = y + (height - totalTextHeight) / 2

        // title
        FontUtil.drawStringWithShadow(this.title, textX, textY, Color.WHITE.rgb)

        // description
        lines.forEachIndexed { index, line ->
            FontUtil.drawStringWithShadow(line, textX + 3, textY + (lineSpacing * (index + 1)), Color.WHITE.rgb)
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.disableBlend()
        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }
}