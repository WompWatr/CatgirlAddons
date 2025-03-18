package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.module.settings.impl.StringSelectorSetting
import catgirlroutes.ui.animations.impl.EaseOutQuadAnimation
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.capitalizeOnlyFirst
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import catgirlroutes.utils.render.StencilUtils
import java.awt.Color

class ElementSelector(parent: ModuleButton, setting: StringSelectorSetting) : // todo use misc element
    Element<StringSelectorSetting>(parent, setting, ElementType.SELECTOR) {

    private val extendAnimation = EaseOutQuadAnimation(300)

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Double {
        height = this.extendAnimation.get(13.0, this.setting.options.size * 13.0 + 13.0, !extended)
        val displayValue = "$displayName: ${this.setting.selected}"

        drawRoundedBorderedRect(0.0, 0.0, width, height, 3.0, 1.0, Color(ColorUtil.bgColor).darker(),  ColorUtil.clickGUIColor)
        drawRoundedBorderedRect(0.0, 0.0, width, 13.0, 3.0, 1.0, Color(ColorUtil.buttonColor),  ColorUtil.clickGUIColor)
        FontUtil.drawTotalCenteredString(displayValue, width / 2.0, 13.0 / 2.0)

        if (!extended && !this.extendAnimation.isAnimating()) return height

        StencilUtils.write(false, 3)
        drawRoundedRect(0.0, 0.0, width, height, 3.0, Color.WHITE)
        StencilUtils.erase(true, 3)
        this.setting.options.forEachIndexed { i, option ->
            val yOff = (i + 1) * 13.0
            if (this.isHovered(mouseX, mouseY, yOff = yOff.toInt())) {
                drawRoundedBorderedRect(0.0, yOff, width, 13.0, 3.0, 1.0, Color(ColorUtil.outlineColor).darker(), Color(ColorUtil.outlineColor))
            }
            FontUtil.drawTotalCenteredString(option, width / 2.0, yOff + 13.0 / 2.0)
        }
        StencilUtils.dispose()

        return height
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            if (this.isHovered(mouseX, mouseY)) {
                this.setting.index += 1
                return true
            }

            if (!extended) return false

            this.setting.options.forEachIndexed { i, option ->
                val yOff = (i + 1) * 13
                if (this.isHovered(mouseX, mouseY, yOff = yOff)) {
                    this.setting.selected = option
                    return true
                }
            }
        } else if (mouseButton == 1 && this.isHovered(mouseX, mouseY)) {
            if (this.extendAnimation.start()) extended = !extended
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun isHovered(mouseX: Int, mouseY: Int, xOff: Int = 0, yOff: Int = 0): Boolean {
        return mouseX >= xAbsolute + xOff && mouseX <= xAbsolute + width + xOff &&
                mouseY >= yAbsolute + yOff && mouseY <= yAbsolute + 13.0 + yOff
    }
}