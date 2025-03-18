package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.module.settings.impl.KeyBindSetting
import catgirlroutes.ui.animations.impl.ColorAnimation
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color

class ElementKeyBind(parent: ModuleButton, setting: KeyBindSetting) :
    Element<KeyBindSetting>(parent, setting, ElementType.KEY_BIND) {

    private val colourAnimation = ColorAnimation(100)
    private val stringWidth = FontUtil.getStringWidth(displayName)
    private var keyWidth = 0.0

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Double {
        val keyName = if (this.setting.value.key > 0) Keyboard.getKeyName(this.setting.value.key) ?: "Err"
        else if (this.setting.value.key < 0) Mouse.getButtonName(this.setting.value.key + 100)
        else "None"
        this.keyWidth = FontUtil.getStringWidth(keyName).toDouble()

        FontUtil.drawString(displayName, 0.0, 1.0)

        val colour = this.colourAnimation.get(ColorUtil.clickGUIColor, Color(ColorUtil.outlineColor), listening)
        val colour2 = this.colourAnimation.get(Color(ColorUtil.outlineColor).darker().darker(), Color(ColorUtil.buttonColor), listening)
        drawRoundedBorderedRect(this.stringWidth + 5.0, 0.0, this.keyWidth + 5.0, height, 3.0, 1.0, colour2, colour)

        FontUtil.drawString(keyName, this.stringWidth + 5.0 + 3.0, 2.0)

        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && this.isHovered(mouseX, mouseY) && !listening) {
            if (this.colourAnimation.start()) listening = true
            return true
        } else if (listening) {
            this.setting.value.key = -100 + mouseButton
            if (this.colourAnimation.start()) listening = false
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!listening) return super.keyTyped(typedChar, keyCode)

        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                this.setting.value.key = Keyboard.KEY_NONE
                if (this.colourAnimation.start()) listening = false
            }
            Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> {
                if (this.colourAnimation.start()) listening = false
            }
            else -> {
                this.setting.value.key = keyCode
                if (this.colourAnimation.start()) listening = false
            }
        }
        return true
    }

    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= xAbsolute + this.stringWidth + 5.0 && mouseX <= xAbsolute + (this.stringWidth + 5.0) + (this.keyWidth + 5.0) &&
                mouseY >= yAbsolute && mouseY <= yAbsolute + height
    }
}