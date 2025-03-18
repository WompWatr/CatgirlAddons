package catgirlroutes.ui.misc.elements.impl

import catgirlroutes.ui.animations.impl.ColorAnimation
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.misc.elements.MiscElement
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedOutline
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import java.awt.Color

/**
 * A [MiscElement] representing a boolean toggle with customizable text and style.
 *
 * @param x The x-coordinate position (default is 0.0).
 * @param y The y-coordinate position (default is 0.0).
 * @param width The width of the element (default is 15.0).
 * @param height The height of the element (default is 15.0).
 * @param text The text displayed on the element (default is none).
 * @param enabled The initial state of the boolean toggle (default is false).
 * @param thickness The border thickness (default is 2.0).
 * @param radius The radius of the corners (default is 5.0).
 */
class MiscElementBoolean(
    x: Double = 0.0,
    y: Double = 0.0,
    width: Double = 15.0,
    height: Double = 15.0,
    var text: String = "",
    var enabled: Boolean = false,
    var thickness: Double = 2.0,
    var radius: Double = 5.0,
    var gap: Double = 1.0,
    var colour: Color = Color(ColorUtil.bgColor)
): MiscElement(x, y, width, height) {

    private val colourAnimation = ColorAnimation(250)

    override fun render(mouseX: Int, mouseY: Int) {
        val colour = colourAnimation.get(ColorUtil.clickGUIColor, this.colour, this.enabled)
        drawRoundedBorderedRect(x + this.gap, y + this.gap, width - this.gap * 2, height - this.gap * 2, this.radius, this.thickness, colour, colour)
        drawRoundedOutline(
            x, y, width, height, this.radius, this.thickness,
            if (this.isHovered(mouseX, mouseY)) ColorUtil.clickGUIColor else Color(ColorUtil.outlineColor)
        )

        FontUtil.drawString(this.text, x + width + 5.0, y + height / 2 - FontUtil.fontHeight / 2)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (this.isHovered(mouseX, mouseY) && mouseButton == 0 && colourAnimation.start()) {
            this.enabled = !this.enabled
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

}