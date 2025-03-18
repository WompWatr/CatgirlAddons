package catgirlroutes.ui.hud

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Visibility
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.impl.misc.Test.TestHud
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.utils.render.HUDRenderUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.floor

/**
 * Provides functionality for game overlay elements.
 * @author Aton
 */
abstract class HudElement{

    private var parentModule: Module
    val enabled: Boolean
        get() = parentModule.enabled

    private val xSett: NumberSetting
    private val ySett: NumberSetting
    val scale: NumberSetting

    var width: Int
    var height: Int

    var partialTicks: Float = 1.0f

    private var preview: () -> Unit

    private val zoomIncrement = 0.05

    /**
     * Use these instead of a direct reference to the NumberSetting
     */
    var x: Int
     get() = xSett.value.toInt()
     set(value) {
         xSett.value = value.toDouble()
     }

    var y: Int
        get() = ySett.value.toInt()
        set(value) {
            ySett.value = value.toDouble()
        }

    /**
     * Sets up a hud Element.
     * This constructor takes care of creating the [NumberSetting]s required to save the position and scale of the hud
     * element to the config.
     */
    constructor(
        module: Module,
        xDefault: Int = 0,
        yDefault: Int = 0,
        width: Int = 10,
        height: Int = 10,
        defaultScale: Double = 1.0,
        preview: () -> Unit = { FontUtil.drawStringWithShadow(module.name, 0.0, 0.0) }
    ) {
        val id = module.settings.count { it.name.startsWith("xHud") }
        val xHud = NumberSetting("xHud_$id", default = xDefault.toDouble(), visibility = Visibility.HIDDEN)
        val yHud = NumberSetting("yHud_$id", default = yDefault.toDouble(), visibility = Visibility.HIDDEN)
        val scaleHud = NumberSetting("scaleHud_$id", defaultScale, 0.1, 4.0, 0.01, visibility = Visibility.HIDDEN)

        module.addSettings(xHud, yHud, scaleHud)

        this.parentModule = module

        this.xSett = xHud
        this.ySett = yHud
        this.scale = scaleHud

        this.width = width
        this.height = height

        this.preview = preview
    }

    /**
     * It is advised to use the other constructor unless this one is required.
     */
    constructor(
        module: Module,
        xHud: NumberSetting,
        yHud: NumberSetting,
        width: Int = 10,
        height: Int = 10,
        scale: NumberSetting,
        preview: () -> Unit = { FontUtil.drawStringWithShadow(module.name, 0.0, 0.0) }
    ) {
        this.parentModule = module

        this.xSett = xHud
        this.ySett = yHud
        this.scale = scale

        this.width = width
        this.height = height

        this.preview = preview
    }

    /**
     * Resets the position of this hud element by setting the value of xSett and ySett to their default.
     *
     * Can be overridden in the implementation.
     */
    open fun resetElement() {
        xSett.value = xSett.default
        ySett.value = ySett.default
        scale.value = scale.default
    }

    /**
     * Handles scroll wheel action for this element.
     * Can be overridden in implementation.
     */
    open fun scroll(amount: Int) {
        this.scale.value += amount * zoomIncrement
    }

    /**
     * This will initiate the hud render and translate to the correct position and scale.
     */
    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        this.partialTicks = event.partialTicks
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(scale.value, scale.value, 1.0)

        renderHud()

        GlStateManager.popMatrix()
    }

    /**
     * Override this method in your implementations.
     *
     * This method is responsible for rendering the HUD element.
     * Within this method coordinates are already transformed regarding to the HUD position [x],[x] and [scale].
     */
    abstract fun renderHud()

    /**
     * Use this method to dynamically update [HudElement] dimensions
     * (I couldn't come up with anything else without putting a lot of effort)
     * @see [TestHud]
     */
    open fun setDimensions() = Unit

    /**
     * Used for moving the hud element.
     * Draws a rectangle in place of the actual element
     */
    fun renderPreview(isDragging: Boolean) {
        if (!this.enabled) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        val scaleValue = scale.value
        GlStateManager.scale(scaleValue, scaleValue, 1.0)

        // render coords and scale if dragging
        if (isDragging) {
            val text = "x: ${x * 2} y: ${y * 2} ${if (scaleValue != 1.0) "${floor(scaleValue * 100) / 100}" else ""}"

            val sr = ScaledResolution(mc)

            val w = mc.fontRendererObj.getStringWidth(text) + 4

            val x2 = 4.0 + if (sr.scaledWidth * scaleValue < (x + (width + w) * scaleValue) * scaleValue) -w else width
            val y2 = -1.0 + if (y - 12 * scaleValue < 5 * scaleValue) height else -12

            val adjX = x2 * scaleValue // still schizo but Idgaf
            GlStateManager.pushMatrix()
            GlStateManager.scale(1.0 / scaleValue, 1.0 / scaleValue, 1.0)
            HUDRenderUtils.renderRect(adjX - 2, y2 - 2, w.toDouble(), 12.0, Color(21, 21, 21, 200))
            FontUtil.drawStringWithShadow(text, adjX, y2, Color.WHITE.rgb)
            GlStateManager.popMatrix()
        } else {
            HUDRenderUtils.renderRect(-2.0, -2.0, width.toDouble() + 3, height.toDouble(), Color(21, 21, 21, 200)) // bg
            this.preview()
        }

        // border
        HUDRenderUtils.renderRectBorder(
            -2.0,
            -2.0,
            width.toDouble() + 3,
            height.toDouble(),
            0.5,
            Color(208, 208, 208)
        )

        GlStateManager.popMatrix()
    }
}