package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.utils.PlayerUtils.findDistanceToAirBlocksLegacy
import catgirlroutes.utils.PlayerUtils.relativeClip
import catgirlroutes.utils.render.HUDRenderUtils.sr
import catgirlroutes.utils.renderText
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs

object LavaClip : Module(
    "Lava Clip",
    category = Category.DUNGEON,
    description = "Clips you x blocks down when jumping into lava."
){
    private val lavaClipDistance: NumberSetting = NumberSetting("Lava Clip distance", 15.0, 0.0, 50.0, 1.0, "Distance to clip down")

    init {
        this.addSettings(
            lavaClipDistance
        )
    }

    private var lavaClipping = false
    private var veloCancelled = true

    override fun onKeyBind() {
        if (this.enabled) lavaClipToggle(lavaClipDistance.value * -1)
    }

    private var adjustedDistance: Double? = lavaClipDistance.value * -1

    fun lavaClipToggle(distance: Double = 0.0, onlyToggle: Boolean = false) {
        lavaClipping = !lavaClipping
        veloCancelled = !lavaClipping && !onlyToggle
        if (!veloCancelled) adjustedDistance = distance
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!mc.thePlayer.isInLava || !lavaClipping) return
        lavaClipping = false
        if (adjustedDistance == 0.0) adjustedDistance = findDistanceToAirBlocksLegacy()

        adjustedDistance?.let {
            relativeClip(0.0, -abs(it), 0.0)
        } ?: run {
            veloCancelled = true
        }
    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !lavaClipping || mc.ingameGUI == null) return
        renderText(if (adjustedDistance == 0.0) "Lava clipping" else "Lava clipping $adjustedDistance")
    }

    @SubscribeEvent
    fun onPacket(event: PacketReceiveEvent) {
        if (veloCancelled || event.packet !is S12PacketEntityVelocity || event.packet.entityID != mc.thePlayer.entityId) return

        if (event.packet.motionY == 28000) {
            event.isCanceled = true
            veloCancelled = true
        }
    }
}