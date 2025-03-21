package catgirlroutes.ui.clickguinew.elements

import catgirlroutes.CatgirlRoutes.Companion.RESOURCE_DOMAIN
import catgirlroutes.module.Module
import catgirlroutes.module.impl.render.ClickGui
import catgirlroutes.module.settings.impl.*
import catgirlroutes.ui.animations.impl.ColorAnimation
import catgirlroutes.ui.animations.impl.LinearAnimation
import catgirlroutes.ui.clickgui.elements.ModuleButton.Companion.haramIcon
import catgirlroutes.ui.clickgui.elements.ModuleButton.Companion.whipIcon
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.wrapText
import catgirlroutes.ui.clickguinew.Window
import catgirlroutes.ui.clickguinew.Window.Companion.SCROLL_DISTANCE
import catgirlroutes.ui.clickguinew.elements.menu.*
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawTexturedRect
import net.minecraft.client.gui.GuiScreen.isShiftKeyDown
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color

class ModuleButton(val module: Module, val window: Window) {
    val menuElements: ArrayList<Element<*>> = ArrayList()

    var x = 0.0
    var y = 0.0

    val width = this.window.width
    val height: Double = 20.0

    var extended = false

    val xAbsolute: Double
        get() = x + window.x
    val yAbsolute: Double
        get() = y + window.y

    private val description: List<String> get() = wrapText(this.module.description, this.width - 5.0)

    private val descHeight get() = fontHeight * this.description.size.toDouble()

    private val elementsHeight get() = this.menuElements.sumOf { it.height + 5.0 }

    private val colourAnimation = ColorAnimation(100)
    private val scrollAnimation = LinearAnimation<Double>(200)

    private var scrollTarget = 0.0
    private var scrollOffset = 0.0

    private var listeningKey = false
    private lateinit var keySetting: KeyBindSetting

    init {
        updateElements()
    }

    fun updateElements() {
        var position = -1
        for (setting in module.settings) {
            if ((setting.visibility.visibleInClickGui || setting.visibility.visibleInAdvanced) && setting.shouldBeVisible) run addElement@{
                position++
                if (this.menuElements.any { it.setting === setting }) return@addElement
                val newElement = when (setting) {
                    is BooleanSetting ->    ElementBoolean(this, setting)
                    is NumberSetting ->     ElementSlider(this, setting)
                    is StringSelectorSetting ->   ElementSelector(this, setting)
                    is StringSetting ->     ElementTextField(this, setting)
                    is ColorSetting ->      ElementColor(this, setting)
                    is ActionSetting ->     ElementAction(this, setting)
                    is KeyBindSetting ->    ElementKeyBind(this, setting)
                    is DropdownSetting ->   ElementDropdown(this, setting)
                    else -> return@addElement
                }
                try { // for now ig
                    this.menuElements.add(position, newElement)
                } catch (e: IndexOutOfBoundsException) {
//                    this.menuElements.add(newElement)
                }
            } else {
                this.menuElements.removeIf { it.setting === setting }
            }
        }
        this.keySetting = this.menuElements.removeAt(this.menuElements.lastIndex).setting as KeyBindSetting
    }

    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) : Double {
        GlStateManager.pushMatrix()

        if (!this.window.inModule) {
            GlStateManager.translate(x, y, 0.0)
            val colour = this.colourAnimation.get(Color(ColorUtil.outlineColor), Color(ColorUtil.bgColor), this.module.enabled)
            drawRoundedBorderedRect(0.0, 0.0, this.width, this.height, 3.0, 1.0, colour, Color(ColorUtil.outlineColor))
            FontUtil.drawString(module.name, 9.5, 2 + fontHeight / 2.0)

            if (this.menuElements.isNotEmpty()) {
                drawTexturedRect(dotsIcon, this.width - 18.5, 2.5, 16.0, 16.0)
            }

            val keyName = if (this.listeningKey) "..."
            else if (this.keySetting.value.key > 0) Keyboard.getKeyName(this.keySetting.value.key) ?: "Err"
            else if (this.keySetting.value.key < 0) Mouse.getButtonName(this.keySetting.value.key + 100)
            else "NONE"
            FontUtil.drawString("[${keyName}]", this.width - 25.5 - FontUtil.getStringWidth("[${keyName}]"), 2 + fontHeight / 2.0)

            when (this.module.tag) {
                Module.TagType.HARAM -> drawTexturedRect(haramIcon, FontUtil.getStringWidth(this.module.name) + 15.0, 1.5, 17.0, 17.0)
                Module.TagType.WHIP -> drawTexturedRect(whipIcon, FontUtil.getStringWidth(this.module.name) + 15.0, 1.5, 18.0, 18.0)
                Module.TagType.NONE -> {}
            }
            GlStateManager.popMatrix()
            return this.height + 5.0
        }

        if (this.extended) {
            this.scrollOffset = this.scrollAnimation.get(this.scrollOffset, this.scrollTarget)

            var drawY = this.scrollOffset
            this.menuElements.forEach {
                it.y = drawY
                it.update()
                drawY += it.drawScreen(mouseX, mouseY, partialTicks)
            }
        }
        GlStateManager.popMatrix()
        return 0.0
    }

    fun scroll(amount: Int, mouseX: Int = (this.window.x + 10.0).toInt(), mouseY: Int = (this.window.y + 10.0).toInt()): Boolean {
        if (!this.extended || !this.window.isHovered(mouseX, mouseY)) return false
        val h = this.elementsHeight + 15.0
        if (h < this.window.height) {
            this.scrollTarget = 0.0
            return false
        }
        this.scrollTarget = (this.scrollTarget + amount * SCROLL_DISTANCE).coerceIn(-h + this.window.height, 0.0)
        this.scrollAnimation.start(true)
        return true
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (this.listeningKey) {
            this.keySetting.value.key = -100 + mouseButton
            this.listeningKey = false
            return true
        }
        return when {
            isButtonHovered(mouseX, mouseY) && (!this.window.inModule) -> when (mouseButton) {
                0 -> {
                    when {
                        isShiftKeyDown() -> {
                            this.listeningKey = true
                            true
                        }
                        !this.listeningKey -> {
                            this.module.toggle()
                            this.colourAnimation.start()
                            true
                        }
                        else -> false
                    }
                }
                1 -> {
                    if (this.listeningKey) return false
                    this.menuElements.takeIf { it.isNotEmpty() }?.let { elements ->
                        this.extended = true
                        elements.forEach { it.listening = false }
                        true
                    } ?: false
                }
                2 -> {
                    this.listeningKey = true
                    true
                }
                else -> false
            }
            this.isMouseUnderButton(mouseX, mouseY) -> this.menuElements.reversed().any {
                it.mouseClicked(mouseX, mouseY, mouseButton).also { clicked ->
                    if (clicked) {
                        if (it.parent.module.name == "ClickGUI" && it.displayName == "ClickGui") {
                            ClickGui.onEnable()
                        }
                        updateElements()
                    }
                }
            }
            else -> false
        }
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (this.extended) this.menuElements.reversed().forEach { it.mouseReleased(mouseX, mouseY, state) }
    }

    fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (this.extended) this.menuElements.reversed().forEach { it.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick) }
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (this.extended) {
            this.menuElements.reversed().forEach {
                if (it.keyTyped(typedChar, keyCode)) return true
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.extended = false
                return true
            }
            if (!this.menuElements.any { it.listening }) when (keyCode) {
                Keyboard.KEY_UP -> this.scroll(1)
                Keyboard.KEY_DOWN -> this.scroll(-1)
            }
        }

        if (!this.listeningKey) return false
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                this.keySetting.value.key = Keyboard.KEY_NONE
                this.listeningKey = false
            }
            Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> {
                this.listeningKey = false
            }
            else -> {
                this.keySetting.value.key = keyCode
                this.listeningKey = false
            }
        }
        return true
    }

    private fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return (!this.extended || !this.window.inModule) && mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + this.height
    }

    private fun isMouseUnderButton(mouseX: Int, mouseY: Int): Boolean {
        return this.extended && mouseX >= this.window.x && mouseX <= this.window.x + this.width && mouseY > yAbsolute
    }

    companion object {
        val dotsIcon = ResourceLocation(RESOURCE_DOMAIN, "dots.png")
    }
}