package catgirlroutes.ui.misc.inventorybuttons

import catgirlroutes.config.InventoryButtonsConfig
import catgirlroutes.config.InventoryButtonsConfig.allButtons
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.misc.elements.impl.MiscElementText
import catgirlroutes.utils.render.HUDRenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class InventoryButtonEditor : GuiScreen() { // todo: recode prob

    private val invWidth: Int = 176
    private val invHeight: Int = 166

    private var invX: Int = 0
    private var invY: Int = 0

    private var editingButton: InventoryButton? = null

    private val editorWidth = 150.0
    private val editorHeight = 78.0
    private var editorX = 0
    private var editorY = 0

    private val commandTextField: MiscElementText = MiscElementText(width = editorWidth - 14, height = 16.0)
    private val iconTextField: MiscElementText = MiscElementText(width = editorWidth - 14, height =  16.0)

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        this.drawDefaultBackground()

        invX = width / 2 - invWidth / 2
        invY = height / 2 - invHeight / 2

        GlStateManager.pushMatrix()
        GlStateManager.enableDepth()
        GlStateManager.color(1f, 1f, 1f, 1f)

        mc.textureManager.bindTexture(INVENTORY)
        HUDRenderUtils.drawTexturedRect(invX.toFloat(), invY.toFloat(), invWidth.toFloat(), invHeight.toFloat(), 0f, invWidth / 256f, 0f, invHeight / 256f, GL11.GL_NEAREST)

        allButtons.forEach { button ->
            val x = invX + button.x
            val y = invY + button.y
            val (colour, borderColour) = if (editingButton == button) {
                InventoryButton.colour.brighter() to InventoryButton.borderColour.brighter()
            } else {
                InventoryButton.colour to InventoryButton.borderColour
            }
            button.render(invX.toDouble(), invY.toDouble(), colour, borderColour)
            if (!button.isActive && !button.isEquipment) FontUtil.drawTotalCenteredString("+", (x + 8).toDouble(), (y + 8).toDouble())
        }

        editingButton?.let {
            val x = invX + it.x
            val y = invY + it.y
            editorX = x + 8 - editorWidth.toInt() / 2
            editorY = y + 20

            GlStateManager.pushMatrix()
            GlStateManager.translate(0.0, 0.0, 50.0)
            HUDRenderUtils.drawRoundedBorderedRect(
                editorX.toDouble(), editorY.toDouble(),
                editorWidth, editorHeight,
                5.0, 1.0,
                Color(ColorUtil.bgColor), Color(ColorUtil.outlineColor)
            )

            FontUtil.drawString("Command", editorX + 7, editorY + 7, 0xffa0a0a0.toInt())
            commandTextField.apply {
                prependText = if (text.startsWith("/")) "" else "§7/§r"
                this.x = editorX + 7.0
                this.y = editorY + 19.0
                render(mouseX, mouseY)
            }

            FontUtil.drawString("Icon", editorX + 7, editorY + 43, 0xffa0a0a0.toInt())
            iconTextField.apply {
                this.x = editorX + 7.0
                this.y = editorY + 55.0
                render(mouseX, mouseY)
            }
            GlStateManager.popMatrix()
        }

        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return

        editingButton?.takeIf { isHoveringEditor(mouseX, mouseY) }?.let {
            if (commandTextField.mouseClicked(mouseX, mouseY, mouseButton)) it.command = commandTextField.text
            if (iconTextField.mouseClicked(mouseX, mouseY, mouseButton)) it.icon = iconTextField.text
        }

        if (isHoveringEditor(mouseX, mouseY)) return

        allButtons.filter { !it.isEquipment && it.isHovered(mouseX - invX, mouseY - invY) }
            .forEach { button ->
                if (editingButton == button) {
                    editingButton = null
                } else {
                    editingButton = button

                    commandTextField.focus = true
                    commandTextField.text = editingButton!!.command

                    iconTextField.text = editingButton!!.icon
                    iconTextField.focus = false

                    InventoryButtonsConfig.save()
                }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        editingButton?.let {
            if (commandTextField.keyTyped(typedChar, keyCode)) it.command = commandTextField.text
            if (iconTextField.keyTyped(typedChar, keyCode)) it.icon = iconTextField.text
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.editingButton = null
                return
            }
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        InventoryButtonsConfig.save()
        super.onGuiClosed()
    }

    override fun doesGuiPauseGame(): Boolean {
        return false
    }

    private fun isHoveringEditor(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= editorX && mouseX <= editorX + editorWidth
                && (mouseY >= editorY) and (mouseY <= editorY + editorHeight)
    }

    companion object {
        val INVENTORY: ResourceLocation = ResourceLocation("minecraft:textures/gui/container/inventory.png")
    }
}