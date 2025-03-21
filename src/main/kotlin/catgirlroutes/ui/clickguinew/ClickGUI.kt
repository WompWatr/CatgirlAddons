package catgirlroutes.ui.clickguinew

import catgirlroutes.CatgirlRoutes.Companion.moduleConfig
import catgirlroutes.module.Category
import catgirlroutes.module.impl.render.ClickGui
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.capitalizeOnlyFirst
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.wrapText
import catgirlroutes.ui.misc.elements.impl.MiscElementButton
import catgirlroutes.ui.misc.elements.impl.MiscElementText
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.resetScissor
import catgirlroutes.utils.render.HUDRenderUtils.scissor
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.IOException

class ClickGUI : GuiScreen() { // todo: module description, fix element description

    var x: Double = 0.0
    var y: Double = 0.0

    val guiWidth = 305.0
    val guiHeight = 230.0

    val categoryWidth = 85.0

    var selectedWindow: Window

    val searchBar = MiscElementText(
        width = guiWidth - categoryWidth - 5.0,
        height = 20.0,
        placeholder = "Search...",
        radius = 3.0,
        bgColour = Color(ColorUtil.bgColor)
    )
    private val categoryButtons: ArrayList<MiscElementButton> = ArrayList()

    val description: MutableMap<String, Description> = mutableMapOf()

    private lateinit var sr: ScaledResolution

    init {
        windows = ArrayList()
        for (category in Category.entries) {
            windows.add(Window(category, this))
        }

        this.selectedWindow = windows[1] // render
    }

    override fun initGui() {
        sr = ScaledResolution(mc)
        scale = CLICK_GUI_SCALE / sr.scaleFactor
        x = ((sr.scaledWidth / 2.0) - (guiWidth / 2.0) * scale) / scale
        y = ((sr.scaledHeight / 2.0) - (guiHeight / 2.0) * scale) / scale

        searchBar.x = this.x + categoryWidth + 5.0
        searchBar.y = this.y

        windows.forEach {
            it.x = x + categoryWidth + 10.0
            it.y = y + 25.0 + 5.0
        }

        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        GlStateManager.pushMatrix()
        val prevScale = mc.gameSettings.guiScale
        scale = CLICK_GUI_SCALE / sr.scaleFactor
        mc.gameSettings.guiScale = 2
        GL11.glScaled(scale, scale, scale)

        var categoryOffset = 25.0

        if (ClickGui.showUsageInfo.enabled) renderUsage()

        drawRoundedBorderedRect(x - 5.0, y - 5.0, guiWidth + 10.0, guiHeight + 10.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
        drawRoundedBorderedRect(x, y, categoryWidth, guiHeight, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)

        drawRoundedBorderedRect(x + categoryWidth + 5.0, y + 25.0, guiWidth - categoryWidth - 5.0, guiHeight - 25.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)

        val titleWidth = FontUtil.getStringWidth(ClickGui.clientName.text)
        FontUtil.drawStringWithShadow(ClickGui.clientName.text, x + categoryWidth / 2.0 - titleWidth / 2.0, y + 6.0)

        this.searchBar.apply {
            outlineColour = Color(ColorUtil.outlineColor)
            outlineFocusColour = ColorUtil.clickGUIColor
            bgColour = Color(ColorUtil.bgColor)
            render(scaledMouseX, scaledMouseY)
        }
        categoryButtons.clear()

        val scissor = scissor(x, y + 26.0, guiWidth, guiHeight - 27.0)
        for (window in windows) {

            val offset: Double = if (window.category == Category.SETTINGS) guiHeight - 20.0 else categoryOffset

            val categoryButton = MiscElementButton(
                "",
                x + 5.0,
                y + offset + 2.0,
                categoryWidth - 9.0,
                14.0,
                1.0,
                3.0,
                outlineColour = Color.WHITE.withAlpha(0),
                outlineHoverColour = Color.WHITE.withAlpha(0)
            ) { this.selectedWindow = window }

            if (this.searchBar.text.isNotEmpty()) {
                val containsSearch = window.moduleButtons.any { it.module.name.contains(this.searchBar.text, true) }
                if (!containsSearch) {
                    continue
                }
            }

            categoryButton.render(scaledMouseX, scaledMouseY)
            categoryButtons.add(categoryButton)

            window.drawScreen(scaledMouseX, scaledMouseY, partialTicks)

            if (this.selectedWindow == window) {
                drawRoundedBorderedRect(x + 5.0, y + offset + 2.0, categoryWidth - 9.0, 14.0, 3.0, 1.0, Color(ColorUtil.outlineColor), Color(ColorUtil.outlineColor))
            }
            FontUtil.drawStringWithShadow(window.category.name.capitalizeOnlyFirst(), x + 12.0, y + offset + 5.0)

            if (window.category != Category.SETTINGS) categoryOffset += 14.0
        }
        resetScissor(scissor)

        this.description.forEach { (_, desc) -> if (this.selectedWindow.inModule) desc.draw() } // todo: fix this schizo shit

        GlStateManager.popMatrix()
        mc.gameSettings.guiScale = prevScale
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    override fun handleMouseInput() {
        super.handleMouseInput()

        var i = Mouse.getEventDWheel()
        if (i != 0) {
            i = i.coerceIn(-1, 1)
            if (isShiftKeyDown()) i *= 7
            debugMessage(scaledMouseX)
            debugMessage(scaledMouseY)

            for (window in windows.reversed()) {
                if (window.scroll(i, scaledMouseX, scaledMouseY)) return
            }
            this.selectedWindow.moduleButtons.forEach { if (it.scroll(i, scaledMouseX, scaledMouseY)) return }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        this.searchBar.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)
        categoryButtons.firstOrNull { it.mouseClicked(scaledMouseX, scaledMouseY, mouseButton) }?.let {
            selectedWindow.moduleButtons.forEach { moduleButton -> moduleButton.extended = false }
        }
        windows.reversed().forEach { if (it.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)) return }
        super.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        windows.reversed().forEach { it.mouseReleased(mouseX, mouseY, state) }
        super.mouseReleased(scaledMouseX, scaledMouseY, state)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (this.searchBar.keyTyped(typedChar, keyCode)) return
        windows.reversed().forEach { if (it.keyTyped(typedChar, keyCode)) return }
        when (keyCode) {
            Keyboard.KEY_F -> if (isCtrlKeyDown()) this.searchBar.focus = true
            Keyboard.KEY_ESCAPE -> {
                if (this.searchBar.text.isNotEmpty()) {
                    this.searchBar.text = ""
                    return
                }
            }
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        windows.reversed().forEach { it.mouseClickMove(scaledMouseX, scaledMouseY, clickedMouseButton, timeSinceLastClick) }
        super.mouseClickMove(scaledMouseX, scaledMouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun doesGuiPauseGame(): Boolean {
        return false
    }

    override fun onGuiClosed() {
        this.searchBar.focus = false
        moduleConfig.saveConfig()
    }

    val scaledMouseX get() = MathHelper.ceiling_double_int(Mouse.getX() / CLICK_GUI_SCALE)
    val scaledMouseY get() = MathHelper.ceiling_double_int( (mc.displayHeight - Mouse.getY()) / CLICK_GUI_SCALE)

    private fun renderUsage() {
        val lines = listOf("GUI Usage:",
            "Left click Module Buttons to toggle the Module.",
            "Right click Module Buttons to open settings.",
            "Middle click or Shift click Module Buttons to change the Module key bind.",
            "Disable this Overlay in the Click Gui Module in the Render Category.",
            "You can change Click GUI style in settings"
        )

        lines.forEachIndexed { i, text ->
            FontUtil.drawString(
                text,
                10.0,
                10.0 + FontUtil.getScaledFontHeight(1.3).toDouble() * i,
                ColorUtil.clickGUIColor.rgb,
                scale = 1.3
            )
        }
    }

    data class Description(var text: String = "", var x: Double, var y: Double) {
        fun draw() {
            if (this.text.isEmpty()) return
            val description = wrapText(text, 150.0)
            y -= description.size * fontHeight + 6.0
            x += 6.0
            drawRoundedBorderedRect(
                x - 3.0,
                y - 3.0,
                description.maxOf { FontUtil.getStringWidth(it).toDouble() } + 6.0,
                description.size * fontHeight + 6.0,
                3.0,
                1.0,
                Color(ColorUtil.bgColor).darker(),
                ColorUtil.clickGUIColor
            )
            description.forEachIndexed { i, it ->
                FontUtil.drawString(it, x, y + fontHeight * i)
            }
        }
    }

    companion object {
        const val CLICK_GUI_SCALE = 2.0
        var windows: ArrayList<Window> = arrayListOf()
        var scale = 2.0
    }
}