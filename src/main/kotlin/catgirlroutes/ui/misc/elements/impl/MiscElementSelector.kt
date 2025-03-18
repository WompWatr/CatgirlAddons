package catgirlroutes.ui.misc.elements.impl

import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.capitalizeOnlyFirst
import catgirlroutes.ui.misc.elements.MiscElement
import catgirlroutes.utils.render.HUDRenderUtils
import java.awt.Color
import java.util.*

/**
 * A [MiscElement] for selecting an option from a list. Supports vertical or horizontal layout and customizable
 * dimensions and style.
 *
 * @param name The name of the selector (doesn't really do anything)
 * @param defaultSelected The default selected option.
 * @param options A list of available options.
 * @param x The x-coordinate position (default is 0.0).
 * @param y The y-coordinate position (default is 0.0).
 * @param width The width of the selector (default is 80.0).
 * @param height The height of the selector (default is 20.0).
 * @param thickness The border thickness (default is 2.0).
 * @param radius The radius of the corners (default is 5.0).
 * @param vertical Whether the options are displayed vertically or horizontally (default is true).
 * @param optionsPerRow The number of options per row. Only works with horizontal selector.
 * @param gap The gap between options (default is 5).
 */
class MiscElementSelector(
    var name: String,
    var defaultSelected: String,
    var options: ArrayList<String>,
    x: Double = 0.0,
    y: Double = 0.0,
    width: Double = 80.0,
    height: Double = 20.0,
    var thickness: Double = 2.0,
    var radius: Double = 5.0,
    private val vertical: Boolean = true,
    val optionsPerRow: Int = 999,
    val gap: Int = 5
) : MiscElement(x, y, width, height) {

    private var extended = false

    var index: Int = optionIndex(defaultSelected)
        set(newVal) {
            field = if (newVal > options.size - 1)  0 else if ( newVal < 0) options.size - 1 else newVal
        }

    var selected: String
        get() {
            return if (index == -1) this.defaultSelected
            else options[index]
        }
        set(value) {
            val newIndex = optionIndex(value)
            if (newIndex != -1) {
                index = newIndex
            }
        }

    private fun optionIndex(string: String): Int {
        return this.options.map { it.lowercase() }.indexOf(string.lowercase())
    }

    fun isSelected(string: String): Boolean {
        return this.selected.equals(string, ignoreCase = true)
    }


    override fun render(mouseX: Int, mouseY: Int) { // todo add some indicator it's a selector
        if (vertical) {
            HUDRenderUtils.drawRoundedBorderedRect(
                this.x, this.y, this.width, if (extended) this.height * (this.options.size + 1) else this.height, this.radius, this.thickness,
                Color(ColorUtil.elementColor), if (extended || isHovered(mouseX, mouseY)) ColorUtil.clickGUIColor else Color(ColorUtil.outlineColor)
            )
            FontUtil.drawTotalCenteredString(this.selected, x + this.width / 2.0, y + this.height / 2.0)

            if (!extended) return
            this.options.forEachIndexed { i, option ->

                if (this.isSelected(option) || this.isHovered(mouseX, mouseY, yOff = this.height.toInt() * (i + 1))) {
                    HUDRenderUtils.drawRoundedRect(this.x, this.y + (this.height * (i + 1)), this.width, this.height, this.radius, ColorUtil.clickGUIColor)
                }

                val optionName = option.substring(0, 1).uppercase(Locale.getDefault()) + option.substring(1, option.length)
                FontUtil.drawTotalCenteredString(optionName, this.x + this.width / 2.0, this.y + height / 2.0 + (this.width / 4.0 * (i + 1)))
            }
            return
        }

        this.options.forEachIndexed { i, option ->
            val row = i / 5
            val column = i % 5

            val xPos = this.x + column * (this.width + this.gap)
            val yPos = this.y + row * (this.height + this.gap)

            HUDRenderUtils.drawRoundedBorderedRect(
                xPos, yPos, this.width, this.height, 5.0, this.thickness,
                Color(ColorUtil.elementColor), if (this.isSelected(option)) ColorUtil.clickGUIColor else Color(ColorUtil.outlineColor)
            )

            if (this.isSelected(option) || this.isHovered(mouseX, mouseY, xOff = (xPos - this.x).toInt(), yOff = (yPos - this.y).toInt())) {
                HUDRenderUtils.drawRoundedOutline(xPos, yPos, this.width, this.height, this.radius, this.thickness, ColorUtil.clickGUIColor)
            }

            FontUtil.drawTotalCenteredString(option.capitalizeOnlyFirst(), xPos + this.width / 2.0, yPos + this.height / 2.0)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            if (isHovered(mouseX, mouseY) && this.vertical) {
                this.index += 1
                return true
            }

            if (!extended && this.vertical) return false

            this.options.forEachIndexed { i, option ->
                if (this.vertical) {
                    val yOff = i * (this.height.toInt() + this.gap)
                    if (this.isHovered(mouseX, mouseY, yOff = yOff)) {
                        this.selected = option
                        return true
                    }
                } else {
                    val row = i / optionsPerRow
                    val column = i % optionsPerRow

                    val xOff = column * (this.width.toInt() + this.gap)
                    val yOff = row * (this.height.toInt() + this.gap)

                    if (this.isHovered(mouseX, mouseY, xOff = xOff, yOff = yOff)) {
                        this.selected = option
                        return true
                    }
                }
            }
        } else if (mouseButton == 1 && isHovered(mouseX, mouseY) && this.vertical) {
            this.extended = !extended
            return true
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }
}