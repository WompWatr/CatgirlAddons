package catgirlroutes.ui.clickguinew

import catgirlroutes.module.Category
import catgirlroutes.module.ModuleManager
import catgirlroutes.module.settings.SettingsCategory
import catgirlroutes.ui.animations.impl.LinearAnimation
import catgirlroutes.ui.clickguinew.elements.ModuleButton
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import kotlin.reflect.full.hasAnnotation

class Window(
    val category: Category,
    val clickGui: ClickGUI
) {
    val moduleButtons: ArrayList<ModuleButton> = ArrayList()

    var x = this.clickGui.x + this.clickGui.categoryWidth + 10.0
    var y = this.clickGui.y + 25.0 + 5.0

    val width = this.clickGui.guiWidth - this.clickGui.categoryWidth - 15.0
    val height = this.clickGui.guiHeight - 25.0

    private val selected: Boolean
        get() = this.clickGui.selectedWindow == this
    val inModule: Boolean get() = this.moduleButtons.any { it.extended }

    private var scrollTarget = 0.0
    private var scrollOffset = 0.0

    private val scrollAnimation = LinearAnimation<Double>(200)

    init {
        ModuleManager.modules
            .filter { (this.category == Category.SETTINGS && it::class.hasAnnotation<SettingsCategory>()) || it.category == this.category }
            .forEach { this.moduleButtons.add(ModuleButton(it, this)) }
    }


    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!this.selected) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0.0)

        this.scrollOffset = this.scrollAnimation.get(this.scrollOffset, this.scrollTarget)

        var drawY = this.scrollOffset
        this.moduleButtons.filtered().reversed().forEach {
            it.x = 0.0
            it.y = drawY
            drawY += it.drawScreen(mouseX, mouseY, partialTicks)
        }

        GlStateManager.popMatrix()
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (this.isHovered(mouseX, mouseY)) {
            this.moduleButtons.filtered().forEach {
                if (it.mouseClicked(mouseX, mouseY, mouseButton)) return true
            }
        }
        return false
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (this.selected) this.moduleButtons.filtered().forEach { it.mouseReleased(mouseX, mouseY, state) }
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (this.selected) {
            this.moduleButtons.filtered().forEach {
                if (it.keyTyped(typedChar, keyCode)) return true
            }
        }
        if (!this.inModule) when (keyCode) {
            Keyboard.KEY_UP -> this.scroll(1)
            Keyboard.KEY_DOWN -> this.scroll(-1)
        }
        return false
    }

    fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (this.selected) this.moduleButtons.filtered().forEach { it.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick) }
    }

    fun scroll(amount: Int, mouseX: Int = (this.x + 10.0).toInt(), mouseY: Int = (this.y + 10.0).toInt()): Boolean {
        if (inModule || !isHovered(mouseX, mouseY)) return false
        val h = moduleButtons.filtered().size * 25.0 + 5.0
        if (h < this.height) return false
        scrollTarget = (scrollTarget + amount * SCROLL_DISTANCE).coerceIn(-h + this.height, 0.0)
        scrollAnimation.start(true)
        return true
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        if (!this.selected) return false
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height
    }

    private fun List<ModuleButton>.filtered() = filter { it.module.name.contains(clickGui.searchBar.text, true) }.reversed()

    companion object {
        const val SCROLL_DISTANCE = 25
    }
}