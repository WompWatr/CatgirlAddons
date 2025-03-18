package catgirlroutes.ui.clickguinew.elements.menu

import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.ui.animations.impl.EaseOutQuadAnimation
import catgirlroutes.ui.clickgui.util.ColorUtil.hex
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickguinew.elements.Element
import catgirlroutes.ui.clickguinew.elements.ElementType
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import catgirlroutes.ui.misc.elements.impl.MiscElementText
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.Utils.equalsOneOf
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedOutline
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedHueBox
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawSBBox
import catgirlroutes.utils.render.StencilUtils
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard
import java.awt.Color

class ElementColor(parent: ModuleButton, setting: ColorSetting) : // todo: shadows, rounded boxes, favourite colours
    Element<ColorSetting>(parent, setting, ElementType.COLOR) {

    var dragging: Int? = null

    private inline val colorValue: Color
        get() = this.setting.value

    private val hexTextField = MiscElementText(
        FontUtil.getStringWidth("Hex") + 5.0,
        DEFAULT_HEIGHT * 8 + 5.0,
        width / 2.0 - (FontUtil.getStringWidth("Hex") + 5.0) + 27.0,
        DEFAULT_HEIGHT,
        colorValue.hex,
        prependText = "§7#§r",
        thickness = 1.0,
        radius = 5.0,
        outlineColour = colorValue.hsbMax(setting).withAlpha(255).darker(),
        outlineFocusColour = colorValue.hsbMax(setting).withAlpha(255).darker()
    )
    private var hexPrev = this.hexTextField.text
    private val extendAnimation = EaseOutQuadAnimation(300)

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Double {
        height = this.extendAnimation.get(if (this.setting.collapsible) DEFAULT_HEIGHT else DEFAULT_HEIGHT * 9 + 5.0, DEFAULT_HEIGHT * 9 + 5.0, !extended)
        FontUtil.drawString(displayName, 0.0, 0.0)
        drawRoundedBorderedRect(
            FontUtil.getStringWidth(displayName) + 5.0,
            0.0,
            20.0,
            9.0,
            3.0,
            1.0,
            this.colorValue,
            this.colorValue.darker()
        )

        if (!(extended || !this.setting.collapsible) && !this.extendAnimation.isAnimating()) return height

        if (this.setting.collapsible) {
            StencilUtils.write(false)
            drawRoundedRect(-3.0, 0.0, width, height, 3.0, Color.WHITE)
            StencilUtils.erase(true)
        }

        /**
         * SB BOX
         */
        drawSBBox(1.0, DEFAULT_HEIGHT + 1.0, width / 2.0 - 2.0, DEFAULT_HEIGHT * 7 - 2.0, this.colorValue.hsbMax(this.setting).rgb)
        drawRoundedOutline(0.0, DEFAULT_HEIGHT, width / 2.0, DEFAULT_HEIGHT * 7, 3.0, 1.0, this.colorValue.hsbMax(this.setting))
        drawRoundedBorderedRect(
            this.setting.saturation * width / 2.0 - 3.0,
            (1 - this.setting.brightness) * DEFAULT_HEIGHT * 7 + DEFAULT_HEIGHT - 3.0,
            6.0, 6.0, 6.0, 2.0,
            this.colorValue.withAlpha(255), Color.WHITE
        )

        /**
         * HUE
         */
        drawRoundedHueBox(width / 2.0 + 3.5 + 1.0, DEFAULT_HEIGHT + 1.0, 8.0, DEFAULT_HEIGHT * 7 - 2.0, 3.0, true)
        drawRoundedOutline(width / 2.0 + 3.5, DEFAULT_HEIGHT, 10.0, DEFAULT_HEIGHT * 7, 3.0, 1.0, this.colorValue.hsbMax(this.setting))
        drawRoundedBorderedRect(
            width / 2.0 + 3.5 + 2.0,
            this.setting.hue * DEFAULT_HEIGHT * 7 + DEFAULT_HEIGHT - 3.0,
            6.0, 6.0, 6.0, 2.0,
            this.colorValue.hsbMax(this.setting).withAlpha(255).darker(),
            Color.WHITE
        )

        /**
         * ALPHA
         */
        if (this.setting.allowAlpha) {
//            drawRoundedSBBox(
//                width / 2.0 + DEFAULT_HEIGHT + 3.5, DEFAULT_HEIGHT, 8.0, DEFAULT_HEIGHT * 6, 3.0,
//                this.colorValue.withAlpha(255).rgb, Color.black.rgb, this.colorValue.withAlpha(255).rgb, Color.black.rgb
//            )
            drawSBBox(
                width / 2.0 + DEFAULT_HEIGHT + 3.5 + 1.0, DEFAULT_HEIGHT + 1.0, 8.0, DEFAULT_HEIGHT * 7 - 2.0,
                this.colorValue.withAlpha(255).rgb, this.colorValue.withAlpha(255).rgb, Color.black.rgb, Color.black.rgb
            )
            drawRoundedOutline(width / 2.0 + DEFAULT_HEIGHT + 3.5, DEFAULT_HEIGHT, 10.0, DEFAULT_HEIGHT * 7, 3.0, 1.0, this.colorValue.hsbMax(this.setting))
            drawRoundedBorderedRect(
                width / 2.0 + DEFAULT_HEIGHT + 3.5 + 2.0,
                (1.0 - this.setting.alpha) * (DEFAULT_HEIGHT * 7) + DEFAULT_HEIGHT - 3.0,
                6.0, 6.0, 6.0, 2.0,
                Color.WHITE.withAlpha(this.setting.alpha), Color.WHITE
            )
        }

        /**
         * DRAGGING
         */
        when (this.dragging) {
            0 -> {
                this.setting.saturation = MathHelper.clamp_float(((mouseX - xAbsolute) / (width / 2.0)).toFloat(), 0.0f, 1.0f)
                this.setting.brightness = MathHelper.clamp_float((-(mouseY - yAbsolute - DEFAULT_HEIGHT * 8) / (DEFAULT_HEIGHT * 7)).toFloat(), 0.0f, 1.0f)
            }
            1 -> this.setting.hue = MathHelper.clamp_float((((mouseY - yAbsolute - DEFAULT_HEIGHT - 2.0) / (DEFAULT_HEIGHT * 7.0 - 2.0)).toFloat()), 0.0f, 1.0f)
            2 -> this.setting.alpha = MathHelper.clamp_float(1f - ((mouseY - yAbsolute - DEFAULT_HEIGHT - 2.0) / (DEFAULT_HEIGHT * 7.0 - 2.0)).toFloat(), 0.0f, 1.0f)
        }

        /**
         * HEX STRING
         */

        if (dragging != null) {
            this.hexTextField.apply {
                text = colorValue.hex
                outlineColour = colorValue.hsbMax(setting).withAlpha(255).darker()
                outlineFocusColour = colorValue.hsbMax(setting).withAlpha(255).darker()
            }
            hexPrev = this.hexTextField.text

        }

        FontUtil.drawString("Hex", 0.0, DEFAULT_HEIGHT * 8 + 7.0)
        this.hexTextField.render(mouseX - xAbsolute.toInt(), mouseY - yAbsolute.toInt())

        /**
         * FAVOURITE
         */
        val favX = if (this.setting.allowAlpha) width / 2.0 + DEFAULT_HEIGHT * 2 + 3.5 else width / 2.0 + DEFAULT_HEIGHT + 3.5
        drawRoundedOutline(favX, DEFAULT_HEIGHT, 15.0, 15.0, 3.0, 1.0, this.colorValue)
        drawRoundedBorderedRect(favX + 1.0, DEFAULT_HEIGHT + 1.0, 13.0, 13.0, 3.0, 1.0, this.colorValue, this.colorValue)

        for (i in 0..2) { // temp
            drawRoundedOutline(favX, DEFAULT_HEIGHT * 2 + 5.0 + (i * (15.0 + 3.0)), 15.0, 15.0, 3.0, 1.0, Color.WHITE.darker())
        }

        if (this.setting.collapsible) StencilUtils.dispose()

        return height
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0) {

            if (isHovered(mouseX, mouseY, FontUtil.getStringWidth(displayName) + 5.0, 0.0, 20.0, 9.0) && this.setting.collapsible) {
                if (this.extendAnimation.start()) extended = !extended
                return true
            }

            if (!extended && this.setting.collapsible) return false

            dragging = when {
                isHovered(mouseX, mouseY, 0.0, DEFAULT_HEIGHT, width / 2, DEFAULT_HEIGHT * 6) -> 0 // SB box
                isHovered(mouseX, mouseY, width / 2.0 + 4.5, DEFAULT_HEIGHT, 8.0, DEFAULT_HEIGHT * 6 - 2.0) -> 1 // hue
                isHovered(mouseX, mouseY, width / 2.0 + DEFAULT_HEIGHT + 4.5, DEFAULT_HEIGHT, 8.0, DEFAULT_HEIGHT * 6) && this.setting.allowAlpha -> 2 // alpha
                else -> null
            }
        } else if (mouseButton == 1) {
            if (isHovered(mouseX, mouseY, FontUtil.getStringWidth(displayName) + 5.0, 0.0, 20.0, 9.0) && this.setting.collapsible) {
                if (this.extendAnimation.start()) extended = !extended
                return true
            }
        }
        return this.hexTextField.mouseClicked(mouseX - xAbsolute.toInt(), mouseY - yAbsolute.toInt(), mouseButton)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        this.hexTextField.mouseClickMove(mouseX - xAbsolute.toInt(), mouseY - yAbsolute.toInt(), clickedMouseButton, timeSinceLastClick)
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        this.dragging = null
        super.mouseReleased(mouseX, mouseY, state)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (this.hexTextField.keyTyped(typedChar, keyCode)) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RETURN) {
                this.hexTextField.text = this.hexTextField.text.completeHexString().removePrefix("#")
                this.hexTextField.focus = false
            }
            return true
        }
        return super.keyTyped(typedChar, keyCode)
    }

    private fun String.completeHexString(): String {
        if (this.isEmpty()) return this
        val stringWithoutHash = this.removePrefix("#")
        if (stringWithoutHash.length.equalsOneOf(6, 8)) {
            try {
                val alpha = if (stringWithoutHash.length == 8) stringWithoutHash.substring(6, 8).toInt(16) / 255f else 1f
                val red = stringWithoutHash.substring(0, 2).toInt(16) / 255f
                val green = stringWithoutHash.substring(2, 4).toInt(16) / 255f
                val blue = stringWithoutHash.substring(4, 6).toInt(16) / 255f
                setting.value = Color(red, green, blue, alpha)
                val hsb = Color.RGBtoHSB(setting.value.red, setting.value.green, setting.value.blue, null)

                // tbh should it probably should be in ColorSetting .value but I cba todo: refactor some day
                setting.hue = hsb[0]
                setting.saturation = hsb[1]
                setting.brightness = hsb[2]

                hexPrev = this
                return this
            } catch (e: Exception) {
                debugMessage(e.toString())
                 return hexPrev
            }
        } else {
            return hexPrev
        }
    }

    private fun isHovered(mouseX: Int, mouseY: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        return mouseX >= xAbsolute + x && mouseX <= xAbsolute + x + width && mouseY >= yAbsolute + y && mouseY <= yAbsolute + y + height
    }

    private fun Color.hsbMax(setting: ColorSetting): Color { // the dumbest fix ever
        val hsb = Color.RGBtoHSB(this.red, this.green, this.blue, null)
        if (hsb[1] == 0.0f || hsb[2] == 0.0f) hsb[0] = setting.hue // when saturation or brightness are 0 hue is 0 too for some weird reason... took me a few hours to realise
        return Color.getHSBColor(hsb[0], 1f, 1f)
    }

}