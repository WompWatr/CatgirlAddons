package catgirlroutes.ui.misc.elements.impl

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.getStringWidth
import catgirlroutes.ui.clickgui.util.FontUtil.getStringWidthDouble
import catgirlroutes.ui.misc.elements.MiscElement
import catgirlroutes.utils.Utils.noControlCodes
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ChatAllowedCharacters
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.abs

/**
 * A [MiscElement] that displays and optionally allows input for a string value with customizable options and style.
 *
 * @param x The x-coordinate position (default is 0.0).
 * @param y The y-coordinate position (default is 0.0).
 * @param width The width of the element (default is 100.0).
 * @param height The height of the element (default is 20.0).
 * @param value The initial text value (default is an empty string).
 * @param options An integer representing additional options (NUM_ONLY, NO_SPACE, FORCE_CAPS).
 * @param prependText Text that will be displayed before the text (default is none).
 * @param thickness The border thickness (default is 2.0).
 * @param radius The radius of the corners (default is 5.0).
 * @param bgColour The background color of the element (default is `ColorUtil.buttonColor`).
 */
class MiscElementText( // todo: CLEAN UP/RECODE (mc code is ass); redo/undo
    x: Double = 0.0,
    y: Double = 0.0,
    width: Double = 100.0,
    height: Double = 20.0,
    val value: String = "",
    var size: Int = 9999,
    var placeholder: String = "",
    var options: Int = 0,
    var prependText: String = "",
    var thickness: Double = 2.0,
    var radius: Double = 5.0,
    var bgColour: Color = Color(ColorUtil.buttonColor),
    var outlineColour: Color = Color(ColorUtil.outlineColor),
    var outlineFocusColour: Color = ColorUtil.clickGUIColor
) : MiscElement(x, y, width, height) {

    var focus: Boolean = false
    private var scrollOffset = 0.0

    private var selectionEnd = 0
    private var cursorPosition = 0
        set(i) {
            field = i
            field = MathHelper.clamp_int(this.cursorPosition, 0, this.text.length)
            this.selectionPos = this.cursorPosition
        }
    private val selectedText get() = this.text.substring(minOf(cursorPosition, selectionEnd), maxOf(cursorPosition, selectionEnd))
    private var selectionPos: Int
        get() = selectionEnd
        set(i) {
            selectionEnd = i.coerceIn(0, text.length)
            scrollOffset = scrollOffset.coerceAtMost(text.length.toDouble())

            val l = mc.fontRendererObj.trimStringToWidth(this.text.substring(scrollOffset.toInt()), -8).length + scrollOffset.toInt()
            if (i.toDouble() == scrollOffset) scrollOffset -= mc.fontRendererObj.trimStringToWidth(text, -8, true).length
            scrollOffset += (if (i > l) i - l else if (i <= scrollOffset) i - scrollOffset else 0).toDouble()
            scrollOffset = MathHelper.clamp_int(scrollOffset.toInt(), 0, text.length).toDouble()
        }


    var text: String = value
        set(string) {
            field = if (string.length > this.size) {
                string.substring(0, this.size)
            } else {
                string
            }
            if (this.cursorPosition > field.length) {
                this.cursorPosition = MathHelper.clamp_int(this.cursorPosition, 0, this.text.length)
            }
        }

    private val renderText get() = this.prependText + text
    private val sizeText get() = if (this.size != 9999) "${renderText.length}/${this.size}" else ""
    private val fieldWidth get() = this.width - 10 - getStringWidthDouble(sizeText)

    private fun updateScrollOffset() {
        val cursorX = getStringWidthDouble(renderText.substring(0, cursorPosition + prependText.length))
        if (cursorX - scrollOffset > fieldWidth) {
            scrollOffset = cursorX - fieldWidth
        } else if (cursorX < scrollOffset) {
            scrollOffset = cursorX.toDouble()
        }
        scrollOffset = scrollOffset.coerceAtLeast(0.0)
    }


    private fun getCursorPos(mouseX: Int): Int {
        val xComp = mouseX - x - getStringWidthDouble(prependText) - 5
        val adjustedX = (xComp + scrollOffset).coerceAtLeast(0.0)

        val trimmed = mc.fontRendererObj.trimStringToWidth(this.text, adjustedX.toInt())

        return trimmed.length.coerceAtMost(renderText.length)
    }

    private var lastClickTime: Long = 0
    private var doubleClickStart: Int = -1
    private var doubleClickEnd: Int = -1

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) {
            focus = false
            this.selectionPos = cursorPosition
            return false
        }

        // in new click gui text fields don't get unfocused if you click on another element for whatever (this.mouseClicked is processed if I click within the element or smt) reason this is an easy fix (kinda)
        // if it's still going to bother I'll actually fix it I guess
        currentlyFocused?.focus = false
        currentlyFocused?.cursorPosition?.let { currentlyFocused?.selectionPos = it }
        currentlyFocused = this

        val currentTime = System.currentTimeMillis()
        val cursorPos = getCursorPos(mouseX)

        when(mouseButton) {
            0 -> {
                if (currentTime - lastClickTime < 250) {
                    val wordBounds = findWordBounds(renderText, cursorPos)
                    doubleClickStart = wordBounds.first
                    doubleClickEnd = wordBounds.second

                    cursorPosition = doubleClickStart
                    this.selectionPos = doubleClickEnd
                } else cursorPosition = cursorPos
            }
            1 -> text = ""
            else -> cursorPosition = cursorPos
        }
        focus = true
        lastClickTime = currentTime

        updateScrollOffset()
        return true
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (!focus || !this.isHovered(mouseX, mouseY)) return
        this.selectionPos = getCursorPos(mouseX)
        return super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!focus) return false
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (this.selectedText.isNotEmpty()) {
                this.cursorPosition = this.text.length
                return true
            }
            this.focus = false
            return true
        }

        var typedChar2 = typedChar

        val old = this.text
        if ((options and FORCE_CAPS) != 0) typedChar2 = typedChar2.uppercaseChar()
        if ((options and NO_SPACE) != 0 && typedChar2 == ' ') return false

        textboxKeyTyped(typedChar2, keyCode)
        if ((options and NUM_ONLY) != 0 && this.text.any { it !in "0-9." }) this.text = old

        updateScrollOffset()
        return true
    }

    override fun render(mouseX: Int, mouseY: Int) {
        val yPos = y + (this.height - 8) / 2
        val maxScrollOffset = (getStringWidthDouble(this.renderText) - this.fieldWidth).coerceAtLeast(0.0)
        this.scrollOffset = this.scrollOffset.coerceIn(0.0, maxScrollOffset)

        GlStateManager.pushMatrix()
        GlStateManager.color(1.0f, 1.0f, 1.0f)

        drawRoundedBorderedRect(x, y, this.width, this.height, this.radius, this.thickness, this.bgColour, if (this.focus) this.outlineFocusColour else this.outlineColour)

        val sizeColour = if (this.renderText.length == this.size) Color.RED else Color.LIGHT_GRAY
        FontUtil.drawString(this.sizeText, x + this.fieldWidth + 8.0, yPos, sizeColour.rgb)

        val visibleTextStartIndex = mc.fontRendererObj.trimStringToWidth(this.renderText, this.scrollOffset.toInt()).length
        val visibleText = mc.fontRendererObj.trimStringToWidth(
            this.renderText.substring(visibleTextStartIndex),
            this.fieldWidth.toInt() + getStringWidth(" ")
        )

        FontUtil.drawString(visibleText, x + 5, yPos)

        if (this.text.isEmpty()) FontUtil.drawString(this.placeholder, x + 5 + getStringWidthDouble(this.prependText), yPos, Color.LIGHT_GRAY.rgb)

        if (this.focus && System.currentTimeMillis() % 1000 > 500) {
            val cursorText = this.renderText.substring(0, this.cursorPosition + this.prependText.length)

            val cursorWidth = (getStringWidthDouble(cursorText) - this.scrollOffset).coerceIn(0.0, this.fieldWidth)
            val cursorX = x + 5 + cursorWidth

            drawRoundedRect(cursorX, yPos - 1, 1.0, 10.0, 1.0, Color.WHITE)
        }

        if (this.selectedText.isNotEmpty()) {
            val (left, right) = listOf(this.cursorPosition, this.selectionEnd)
                .map { it + this.prependText.noControlCodes.length }
                .let { it.minOrNull() to it.maxOrNull() }

            var texX = 0.0
            this.renderText.noControlCodes.forEachIndexed { i, c ->
                val len = getStringWidthDouble(c.toString())
                if (i in left!! until right!!) {
                    val currentTexX = texX - this.scrollOffset

                    if (currentTexX + len > 0 && currentTexX < this.fieldWidth) {
                        drawRoundedRect(x + 5 + currentTexX, yPos - 1, len.toDouble(), 10.0, 0.1, Color.WHITE.withAlpha(150))
                    }
                }
                texX += len
            }
        }

        GlStateManager.popMatrix()
    }

    private fun findWordBounds(text: String, cursorPos: Int): Pair<Int, Int> {
        if (text.isEmpty()) return Pair(0, 0)

        var start = cursorPos
        var end = cursorPos

        // find start
        while (start > 0 && !text[start - 1].isWhitespace()) {
            start--
        }

        // find end
        while (end < text.length && !text[end].isWhitespace()) {
            end++
        }

        return Pair(start, end)
    }

    private fun textboxKeyTyped(c: Char, i: Int): Boolean {
        if (!this.focus) return false
        when {
            GuiScreen.isKeyComboCtrlA(i) -> {
                this.cursorPosition = this.text.length
                this.selectionPos = 0
                return true
            }
            GuiScreen.isKeyComboCtrlC(i) -> {
                GuiScreen.setClipboardString(this.selectedText)
                return true
            }
            GuiScreen.isKeyComboCtrlV(i) -> {
                this.writeText(GuiScreen.getClipboardString())
                return true
            }
            GuiScreen.isKeyComboCtrlX(i) -> {
                GuiScreen.setClipboardString(this.selectedText)
                this.writeText("")
                return true
            }
            else -> {
                when (i) {
                    14 -> { // backspace
                        if (GuiScreen.isCtrlKeyDown()) this.deleteWords(-1) else this.deleteFromCursor(-1)
                        return true
                    }

                    199 -> { // home
                        if (GuiScreen.isShiftKeyDown()) this.selectionPos = 0 else this.cursorPosition = 0
                        return true
                    }

                    203 -> { // left
                        when {
                            GuiScreen.isShiftKeyDown() -> this.selectionPos = if (GuiScreen.isCtrlKeyDown()) this.getNthWordFromPos(-1, this.selectionEnd) else this.selectionEnd - 1
                            GuiScreen.isCtrlKeyDown() -> this.cursorPosition = this.getNthWordFromCursor(-1)
                            else -> this.cursorPosition = this.selectionEnd - 1
                        }
                        return true
                    }

                    205 -> { // right
                        when {
                            GuiScreen.isShiftKeyDown() ->
                                if (GuiScreen.isCtrlKeyDown()) this.selectionPos = this.getNthWordFromPos(1, this.selectionEnd)
                                else this.cursorPosition = this.selectionEnd + 1
                            GuiScreen.isCtrlKeyDown() -> this.cursorPosition = this.getNthWordFromCursor(1)
                            else -> this.cursorPosition = this.selectionEnd + 1
                        }
                        return true
                    }

                    207 -> { // end
                        if (GuiScreen.isShiftKeyDown()) this.selectionPos = this.text.length else this.cursorPosition = this.text.length
                        return true
                    }

                    211 -> { // another backspace?
                        if (GuiScreen.isCtrlKeyDown()) this.deleteWords(1) else this.deleteFromCursor(1)
                        return true
                    }

                    else -> if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                        this.writeText(c.toString())
                        return true
                    } else {
                        return false
                    }
                }
            }
        }
    }

    private fun deleteFromCursor(i: Int) {
        if (this.text.isNotEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                writeText("")
            } else {
                val (start, end) = if (i < 0) this.cursorPosition + i to this.cursorPosition else this.cursorPosition to this.cursorPosition + i
                val newText = this.text.takeIf { start >= 0 }?.take(start).orEmpty() + this.text.drop(end)

                val oldSelection = this.selectionEnd
                this.text = newText
                if (i < 0) this.cursorPosition = oldSelection + i
            }
        }
    }

    private fun writeText(string: String?) {
        val allowed = ChatAllowedCharacters.filterAllowedCharacters(string)
        val (start, end) = listOf(this.cursorPosition, this.selectionEnd).sorted()
        val maxInsert = this.size - this.text.length - (start - end)
        val newText = this.text.take(start) + allowed.take(maxInsert) + this.text.drop(end)
        this.text = newText
        this.cursorPosition = this.selectionEnd + (start - this.selectionEnd + allowed.take(maxInsert).length)
    }

    private fun deleteWords(i: Int) {
        if (this.text.isNotEmpty()) {
            if (this.selectionEnd != this.cursorPosition) this.writeText("")
            else deleteFromCursor(getNthWordFromCursor(i) - this.cursorPosition)
        }
    }

    private fun getNthWordFromCursor(i: Int): Int {
        return this.getNthWordFromPos(i, this.cursorPosition)
    }

    private fun getNthWordFromPos(i: Int, j: Int): Int {
        return this.func_146197_a(i, j, true)
    }

    private fun func_146197_a(i: Int, j: Int, bl: Boolean): Int { // I think it's this.findWordBounds but a lil different or smt IDK
        var k = j
        val bl2 = i < 0
        val l = abs(i.toDouble()).toInt()

        for (m in 0 until l) {
            if (!bl2) {
                val n = text.length
                k = text.indexOf(32.toChar(), k)
                if (k == -1) {
                    k = n
                } else {
                    while (bl && k < n && text[k] == ' ') {
                        ++k
                    }
                }
            } else {
                while (bl && k > 0 && text[k - 1] == ' ') {
                    --k
                }

                while (k > 0 && text[k - 1] != ' ') {
                    --k
                }
            }
        }

        return k
    }

    companion object { // todo: change
        const val NUM_ONLY = 0b10000
        const val NO_SPACE = 0b01000
        const val FORCE_CAPS = 0b00100
    }
}
