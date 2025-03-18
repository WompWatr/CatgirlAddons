package catgirlroutes.ui.clickguinew.elements

import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.StringSelectorSetting
import catgirlroutes.ui.clickguinew.ClickGUI
import net.minecraft.client.renderer.GlStateManager

abstract class Element<S: Setting<*>>(
    val parent: ModuleButton,
    val setting: S,
    val type: ElementType
) {
    private val clickGui: ClickGUI = parent.window.clickGui

    var x = 0.0
    var y = 0.0

    val width = this.parent.width
    var height: Double

    var displayName: String = this.setting.name
    var extended = false
    var listening = false

    val xAbsolute: Double
        get() = this.x + this.parent.window.x

    val yAbsolute: Double
        get() = this.y + this.parent.window.y

    private var hoverStartTime: Long? = null
    private var description = ClickGUI.Description("", 0.0, 0.0)

    init {
        this.height = when (this.type) {
            ElementType.TEXT_FIELD -> 25.0
            ElementType.SLIDER -> 18.0
            ElementType.BOOLEAN -> 11.0
            ElementType.KEY_BIND -> 11.0
            else -> DEFAULT_HEIGHT
        }
    }

    fun update() {
        displayName = setting.name
        when (type) {
            ElementType.SELECTOR -> {
                height = if (extended)
                    ((setting as StringSelectorSetting).options.size * DEFAULT_HEIGHT + DEFAULT_HEIGHT)
                else
                    DEFAULT_HEIGHT
            }
            ElementType.COLOR -> {
                height =
                    if ((setting as ColorSetting).collapsible && !extended) {
                        DEFAULT_HEIGHT
                    } else {
                        DEFAULT_HEIGHT * 9 + 5.0
                    }
            }
            else -> {}
        }
    }

    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) : Double {
        GlStateManager.pushMatrix()
        GlStateManager.translate(this.x, this.y, 0.0)

        val elementLength = renderElement(mouseX, mouseY, partialTicks)

        if (isHoveredTemp(mouseX, mouseY) && this.setting.description != null && this.parent.extended) {
            if (hoverStartTime == null) hoverStartTime = System.currentTimeMillis()

            if (System.currentTimeMillis() - hoverStartTime!! >= 1000) {
                clickGui.description[this.setting.name] = ClickGUI.Description(this.setting.description!!, mouseX.toDouble(), mouseY.toDouble())
            }
        } else {
            clickGui.description[this.setting.name] = ClickGUI.Description("", 0.0, 0.0)
            hoverStartTime = null
        }

        GlStateManager.popMatrix()
        return elementLength + 5.0
    }

    protected open fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float) : Double { return this.height }

    open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        return this.isHovered(mouseX, mouseY)
    }

    open fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {}

    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean { return false }

    open fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {  }

    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= this.xAbsolute && mouseX <= this.xAbsolute + this.width && mouseY >= this.yAbsolute && mouseY <= this.yAbsolute + this.height
    }

    private fun isHoveredTemp(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= this.xAbsolute && mouseX <= this.xAbsolute + this.width && mouseY >= this.yAbsolute && mouseY <= this.yAbsolute + DEFAULT_HEIGHT
    }

    companion object {
        const val DEFAULT_HEIGHT = 13.0
    }
}

