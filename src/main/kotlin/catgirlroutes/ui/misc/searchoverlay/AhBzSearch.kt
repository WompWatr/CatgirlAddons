package catgirlroutes.ui.misc.searchoverlay

import catgirlroutes.CatgirlRoutes.Companion.moduleConfig
import catgirlroutes.module.impl.misc.Inventory.ahHistory
import catgirlroutes.module.impl.misc.Inventory.bzHistory
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.misc.elements.impl.MiscElementBoolean
import catgirlroutes.ui.misc.elements.impl.MiscElementButton
import catgirlroutes.ui.misc.elements.impl.MiscElementSelector
import catgirlroutes.ui.misc.elements.impl.MiscElementText
import catgirlroutes.utils.ChatUtils.commandAny
import catgirlroutes.utils.NeuRepo
import catgirlroutes.utils.NeuRepo.toStack
import catgirlroutes.utils.Utils.noControlCodes
import catgirlroutes.utils.render.HUDRenderUtils
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.renderRect
import catgirlroutes.utils.render.StencilUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C12PacketUpdateSign
import net.minecraft.tileentity.TileEntitySign
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.client.config.GuiUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import java.io.IOException
import java.util.*

class AhBzSearch( // todo: recode some day
    val type: OverlayType,
    private val sign: TileEntitySign? = null, // if null then run command instead
) : GuiScreen() {

    private var x = 0.0
    private var y = 0.0

    private val overlayWidth = 305.0
    private val overlayHeight = 255.0

    private var visibleRange = 0.0 .. 0.0

    private lateinit var searchBar: MiscElementText

    private var lastSelected = "0"
    private var starSelector = MiscElementSelector(
        "", "0",
        arrayListOf("§6➊", "§6➋", "§6➌", "§6➍", "§6➎", "§c➊", "§c➋", "§c➌", "§c➍", "§c➎"),
        width = 15.0, height = 15.0, thickness = 1.0, radius = 3.0,
        vertical = false, optionsPerRow = 5
    )
    private val forcePetLvl = MiscElementBoolean(text = "Lvl 100 Pets", radius = 3.0, thickness = 1.0)
    private val petLvl: String
        get() = if (forcePetLvl.enabled) "[Lvl 100] " else ""

    private var resHistory
        get() = if (type == OverlayType.AUCTION) ahHistory.value else bzHistory.value
        set(value) {
            if (type == OverlayType.AUCTION) ahHistory.value = value
            else bzHistory.value = value
        }

    private var scrollOffset = 0
    private var scrollHeight = 0.0
    private var scrollY = 0.0
    private var isDraggingScroll = false

    override fun initGui() {
        val sr = ScaledResolution(mc)
        y = sr.scaledHeight / 2.0 - overlayHeight / 2.0
        x = sr.scaledWidth / 2.0 - overlayWidth / 2.0

        visibleRange = (y + 25.0)..(y + 25.0 * 10)

        searchBar = MiscElementText(x, y, overlayWidth, 20.0, radius = 3.0, bgColour = Color(ColorUtil.bgColor))
        searchBar.focus = true

        sign?.let {
            sign.setEditable(false)
            sign.signText[0] = ChatComponentText(searchBar.text)
        }
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {

        GlStateManager.pushMatrix()

        val ahX = x + overlayWidth + 10.0
        val ahY = y + 25.0
        val ahW = 110.0
        val ahH = 85.0

        if (type == OverlayType.AUCTION) {
            drawRoundedBorderedRect(ahX - 10.0, ahY - 5.0, ahW + 10.0, ahH + 10.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
            drawRoundedBorderedRect(ahX, ahY, ahW - 5.0, 55.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
            drawRoundedBorderedRect(ahX, ahY + 60.0, ahW - 5.0, 25.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)

            FontUtil.drawString("Stars:", ahX + 5.0, ahY + 5.0)

            starSelector.apply {
                x = ahX + 5.0
                y = ahY + 15.0
                render(mouseX, mouseY)
            }

            forcePetLvl.apply {
                x = ahX + 5.0
                y = ahY + 65.0
                render(mouseX, mouseY)
            }
        }

        // main box
        drawRoundedBorderedRect(x - 5.0, y - 5.0, overlayWidth + 10, overlayHeight + 10, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)

        if (type == OverlayType.AUCTION) {
            renderRect(ahX - 10.0 + 4.0, ahY - 5.0 + 0.5, 2.0, ahH + 10.0 - 1.0, Color(ColorUtil.bgColor))
            doAhCorners(ahX -10.0 + 4.0, ahY - 5.0, ahH + 10.0)
        }

        // inner box
        drawRoundedBorderedRect(x, y + 25.0, overlayWidth, overlayHeight - 25.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)

        searchBar.prependText = if (forcePetLvl.enabled) "§7[Lvl 100]§r " else ""
        searchBar.render(mouseX, mouseY)

        searchResults.clear()
        val tooltips = mutableListOf<Pair<List<String>, Pair<Int, Int>>>()

        var offset = 30.0
        if (searchBar.text.length > 2) {
            val filteredItems = NeuRepo.repoItems.filter {
                val nameNoJunk = it.name.replace(Regex("['.]"), "") // remove ' and . (necron's -> necrons, l.a.s.r - lasr, )
                val toCheck = if (this.type == OverlayType.AUCTION) it.auction else it.bazaar

                toCheck &&
                (it.name.contains(searchBar.text, true) || nameNoJunk.contains(searchBar.text, true) ||
                (this.type == OverlayType.BAZAAR && it.lore.any { it.contains(searchBar.text, true) })) && // only search lore if bazaar
                (!forcePetLvl.enabled || it.name.contains("Lvl"))
            }

            filteredItems.forEach {
                val posY = y + offset - scrollOffset
                val width = overlayWidth - if (filteredItems.count() > 9) 20.0 else 10.0
                val resultButton = MiscElementButton("", x + 5.0, posY, width, 20.0, 1.0, 3.0) {
                    var finalRes = "$petLvl${it.name}${getStars()}"
                    finalRes = finalRes.clean

                    resHistory.add(0, "REPOITEM:${it.name.clean}")

                    if (sign != null) {
                        sign.signText[0] = ChatComponentText(finalRes)
                        sign.markDirty()
                        mc.displayGuiScreen(null)
                    } else {
                        mc.displayGuiScreen(null)
                        commandAny("${if (type == OverlayType.AUCTION) "ahs" else "bz"} $finalRes")
                    }
                }
                searchResults.add(resultButton)

                StencilUtils.write(false)
                drawRoundedBorderedRect(x, y + 26.0, overlayWidth, overlayHeight - 27.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
                StencilUtils.erase(true)

                val itemStack = it.toStack()
                resultButton.render(mouseX, mouseY)
                FontUtil.drawString(it.name, x + 25.0, posY + 7.0)
                HUDRenderUtils.drawItemStackWithText(itemStack, x + 7.0, posY + 3.0)
                if (resultButton.isHovered(mouseX, mouseY) && mouseY.toDouble() in visibleRange) {
                    tooltips.add(Pair(itemStack.getTooltip(mc.thePlayer, false), Pair(mouseX, mouseY)))
                }
                StencilUtils.dispose()
                offset += 25.0
            }
        } else {
            scrollOffset = 0

            resHistory.filter { it.trim().isNotEmpty() }.forEach { // Idc about optimisation (dupe code etc) rn. if it works it works
                val posY = y + offset
                val name = it.replace("REPOITEM:", "")
                val resultButton = MiscElementButton("", x + 5.0, posY, overlayWidth - 10.0, 20.0, 1.0, 3.0) {
                    var finalRes = "$petLvl$name${getStars()}"
                    finalRes = finalRes.clean

                    resHistory.add(0, it.clean)

                    if (sign != null) {
                        sign.signText[0] = ChatComponentText(finalRes)
                        sign.markDirty()
                        mc.displayGuiScreen(null)
                    } else {
                        mc.displayGuiScreen(null)
                        commandAny("${if (type == OverlayType.AUCTION) "ahs" else "bz"} $finalRes")
                    }
                }
                searchResults.add(resultButton)

                if (it.startsWith("REPOITEM:")) {
                    val item = NeuRepo.getItemFromName(name)
                    val itemStack = item!!.toStack()
                    resultButton.render(mouseX, mouseY)
                    FontUtil.drawString(item.name, x + 25.0, posY + 7.0)
                    HUDRenderUtils.drawItemStackWithText(itemStack, x + 7.0, posY + 3.0)
                    if (resultButton.isHovered(mouseX, mouseY)) {
                        tooltips.add(Pair(itemStack.getTooltip(mc.thePlayer, false), Pair(mouseX, mouseY)))
                    }
                } else {
                    resultButton.render(mouseX, mouseY)
                    FontUtil.drawString(name, x + 25.0, posY + 7.0)
                }
                offset += 25.0
            }
        }

        if (searchResults.size > 9) { // scroll bar
            scrollHeight = (overlayHeight - 25.0) * (9.0 / searchResults.size)
            scrollY = y + 30.0 + (scrollOffset / ((searchResults.size - 9) * 25.0)).coerceIn(0.0, 1.0) * (overlayHeight - 35.0 - scrollHeight)
            drawRoundedBorderedRect(
                x + overlayWidth - 10.0, scrollY, 5.0, scrollHeight,
                3.0, 1.0, Color(ColorUtil.bgColor),
                if (isHoveringScroll(mouseX, mouseY) || isDraggingScroll) ColorUtil.clickGUIColor else Color(ColorUtil.outlineColor)
            )
        }

        tooltips.forEach { (tooltip, position) ->
            GuiUtils.drawHoveringText(
                tooltip, position.first, position.second, width, height, -1, mc.fontRendererObj
            )
        }

        GlStateManager.popMatrix()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        searchBar.mouseClicked(mouseX, mouseY, mouseButton)
        searchBar.focus = true

        if (isHoveringScroll(mouseX, mouseY) && mouseButton == 0) {
            isDraggingScroll = true
        }
        if (mouseY.toDouble() !in visibleRange) return
        searchResults.forEach { it.mouseClicked(mouseX, mouseY, mouseButton) }

        if (type != OverlayType.AUCTION) return
        forcePetLvl.mouseClicked(mouseX, mouseY, mouseButton)
        if (starSelector.mouseClicked(mouseX, mouseY, mouseButton)) {
            if (starSelector.selected == this.lastSelected) {
                // starSelector.selected = blahblhablah doesn't work for some reason, so I just reset it this way...
                starSelector = MiscElementSelector(
                    "", "0",
                    arrayListOf("§6➊", "§6➋", "§6➌", "§6➍", "§6➎", "§c➊", "§c➋", "§c➌", "§c➍", "§c➎"),
                    width = 15.0, height = 15.0, thickness = 1.0, radius = 3.0,
                    vertical = false, optionsPerRow = 5
                )
                this.lastSelected = "00"
            } else {
                this.lastSelected = starSelector.selected
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        searchBar.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
        if (isDraggingScroll) {
            val scrollPercentage = ((mouseY - (y + 30.0)) / (overlayHeight - 35.0)).coerceIn(0.0, 1.0)
            val newScrollOffset = (scrollPercentage * ((searchResults.size - 9) * 25.0)).toInt()
            scrollOffset = (newScrollOffset / 25) * 25
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        isDraggingScroll = false
        super.mouseReleased(mouseX, mouseY, state)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        searchBar.keyTyped(typedChar, keyCode)

        when (keyCode) {
            Keyboard.KEY_RETURN -> {
                resHistory.add(0, searchBar.text)

                if (sign != null) {
                    sign.markDirty()
                    mc.displayGuiScreen(null)
                } else {
                    mc.displayGuiScreen(null)
                    commandAny("${if (type == OverlayType.AUCTION) "ahs" else "bz"} $petLvl${searchBar.text}")
                }
            }

            Keyboard.KEY_ESCAPE -> {
                mc.displayGuiScreen(null)
            }
        }

        sign?.let {
            sign.signText[0] = ChatComponentText("$petLvl${searchBar.text}")
        }

        super.keyTyped(typedChar, keyCode)
    }

    @Throws(IOException::class)
    override fun handleMouseInput() {
        super.handleMouseInput()

        var i = Mouse.getEventDWheel()
        if (i != 0) {
            if (i > 1) {
                i = 1
            }
            if (i < -1) {
                i = -1
            }

            if (this.scroll(i)) return
        }
    }

    override fun onGuiClosed() {
        sign?.let {
            mc.netHandler?.addToSendQueue(C12PacketUpdateSign(sign.pos, sign.signText))
            sign.setEditable(true)
        }
        resHistory = resHistory.distinct().take(9).toMutableList()
        moduleConfig.saveConfig()
    }

    override fun doesGuiPauseGame(): Boolean {
        return false
    }

    private fun scroll(amount: Int): Boolean {
        if (searchResults.size <= 9) return false
        val diff = -amount * 25

        val realDiff = (scrollOffset + diff).coerceAtLeast(0) - scrollOffset
        if ((scrollOffset + realDiff) / 25 > searchResults.size - 9) return false
        scrollOffset += realDiff
        return true
    }

    private fun isHoveringScroll(mouseX: Int, mouseY: Int): Boolean {
        val scrollX = x + overlayWidth - 10.0
        return mouseX >= scrollX && mouseX <= scrollX + 5.0 && mouseY >= scrollY && mouseY <= scrollY + scrollHeight
    }

    private fun doAhCorners(x: Double, y: Double, h: Double) {
        renderRect(x + 0.5, y - 0.5, 1.0, 1.0, Color(ColorUtil.bgColor))
        renderRect(x + 1.5, y - 1.0, 0.5, 0.5, ColorUtil.clickGUIColor.darker())
        renderRect(x + 0.5, y - 1.0, 0.5, 0.5, ColorUtil.clickGUIColor.darker())
        renderRect(x + 1.5, y, 0.5, 0.5, ColorUtil.clickGUIColor.darker())

        renderRect(x + 0.5, y + h - 0.5, 1.0, 1.0, Color(ColorUtil.bgColor))
        renderRect(x + 1.5, y + h + 0.5, 0.5, 0.5, ColorUtil.clickGUIColor.darker())
        renderRect(x + 0.5, y + h + 0.5, 0.5, 0.5, ColorUtil.clickGUIColor.darker())
        renderRect(x + 1.5, y + h - 0.5, 0.5, 0.5, ColorUtil.clickGUIColor.darker())
    }

    private fun getStars(): String {
        val res = StringBuilder(" ")
        when {
            starSelector.index in 1..5 -> res.append("✪".repeat(starSelector.index))
            starSelector.index > 5 -> res.append("✪".repeat(5)).append(starSelector.selected.last())
        }
        return res.toString()
    }

    private val String.clean: String
        get() = this.noControlCodes.replace("[Lvl {LVL}] ", "")

    companion object {
        val searchResults: ArrayList<MiscElementButton> = arrayListOf()
    }
}

enum class OverlayType {
    BAZAAR, AUCTION, NONE
}