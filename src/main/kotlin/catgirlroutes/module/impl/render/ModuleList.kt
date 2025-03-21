package catgirlroutes.module.impl.render

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.ModuleManager.modules
import catgirlroutes.module.settings.NoShowInList
import catgirlroutes.module.settings.RegisterHudElement
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.hud.HudElement
import catgirlroutes.utils.render.HUDRenderUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

//TODO: Maybe box idk. I would personally not. Maybe add a switch to modules for showing in list.

@NoShowInList
object ModuleList : Module(
    name = "Array List",
    category = Category.RENDER,
    description = "ArrayList"
) {
    val textColour = ColorSetting("Text colour", Color.PINK, false, collapsible = false)

    init {
        this.addSettings(textColour)
    }

    val activeModuleList: List<String>
        get() = modules.mapNotNull {
            if (it.enabled && it.name != this.name) it.name else null
        }

    @RegisterHudElement
    object ArrayHud : HudElement(this, 0, 0) {
        override fun renderHud() {
            val modules = activeModuleList
            if (modules.isEmpty()) return

            val sr = ScaledResolution(mc)
            val isLeft = x < sr.scaledWidth / 2
            val isTop = y < sr.scaledHeight / 2

            val maxWidth = modules.maxOf { FontUtil.getStringWidth(it) }.toDouble()
            val sortedModules = modules.sortedBy { if (isTop) -FontUtil.getStringWidth(it) else FontUtil.getStringWidth(it) }

            var yOffset = 0.0
            for (module in sortedModules) {
                val width = FontUtil.getStringWidth(module)
                val xOffset = if (isLeft) 0.0 else maxWidth - width

                // some schizophrenic shit tbh
                val bgX = xOffset + if (isLeft) 0.0 else -1.0
                val textX = xOffset + if (isLeft) 3.0 else 0.0
                val stickX = if (isLeft) -2.0 else maxWidth + 2.0

                GlStateManager.pushMatrix()

                HUDRenderUtils.renderRect(bgX, yOffset - 2.0, width + 4.0, 11.0, Color(0, 0, 0, 128)) // bg
                FontUtil.drawStringWithShadow(module, textX, yOffset, textColour.value.rgb) // module name
                HUDRenderUtils.renderRect(stickX, yOffset - 2.0, 2.0, 11.0, textColour.value) // stick

                GlStateManager.popMatrix()

                yOffset += 11.0
            }
        }

        override fun setDimensions() {
            val modules = activeModuleList
            this.width =  (modules.maxOfOrNull { FontUtil.getStringWidth(it) } ?: 0) + 3
            this.height = modules.size * 11
        }
    }
}
