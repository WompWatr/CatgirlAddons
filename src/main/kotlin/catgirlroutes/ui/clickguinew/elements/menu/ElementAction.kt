package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil.capitalizeOnlyFirst
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.ui.misc.elements.impl.button
import java.awt.Color

class ElementAction(parent: ModuleButton, setting: ActionSetting) :
    Element<ActionSetting>(parent, setting, ElementType.ACTION) {

    private val actionButton = button {
        text = displayName.capitalizeOnlyFirst()
        size(this@ElementAction.width, this@ElementAction.height)
        colour = Color(ColorUtil.elementColor)
    } onClick { this.setting.doAction() }

    override fun renderElement(): Double {
        this.actionButton.render(mouseXRel, mouseYRel)
        return super.renderElement()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        return this.actionButton.mouseClicked(mouseXRel, mouseYRel, mouseButton)
    }
}