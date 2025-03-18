package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.CatgirlRoutes.Companion.RESOURCE_DOMAIN
import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.impl.DropdownSetting
import catgirlroutes.ui.animations.impl.EaseOutQuadAnimation
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawTexturedRect
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import java.awt.Color

class ElementDropdown(parent: ModuleButton, setting: DropdownSetting) :
    Element<DropdownSetting>(parent, setting, ElementType.DROPDOWN) {

    private var arrowAnimation = EaseOutQuadAnimation(300)

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Double {
        val extraHeight = this.getElementsForSettings(this.setting.dependentModules.filter { it.shouldBeVisible }).sumOf { it.height + 5.0 } + 2.0
        drawRoundedBorderedRect(-2.0, 0.0, width + 4.0, height + extraHeight, 3.0, 1.0, Color(ColorUtil.outlineColor).darker(), ColorUtil.clickGUIColor)
        FontUtil.drawString(displayName, 0.0, 3.0)

        val rotation = this.arrowAnimation.get(0.0, -90.0, !this.setting.enabled)
        GlStateManager.translate(width - 15.0 + 6.5, 0.0 + 6.5, 0.0)
        GlStateManager.rotate(rotation.toFloat(), 0.0f, 0.0f, 1.0f)
        GlStateManager.translate(-(width - 15.0 + 6.5), -(0.0 + 6.5), 0.0)
        drawTexturedRect(arrowIcon, width - 15.0, 0.0, 13.0, 13.0)
        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered(mouseX, mouseY) && this.arrowAnimation.start()) {
            this.setting.enabled = !this.setting.enabled
            this.parent.updateElements()
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + height
    }

    private fun getElementsForSettings(settings: List<Setting<*>>): List<Element<*>> {
        return this.parent.menuElements.filter { it.setting in settings }
    }

    companion object {
        val arrowIcon = ResourceLocation(RESOURCE_DOMAIN, "arrow.png")
    }
}