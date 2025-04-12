package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketSentEvent
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.MovementUtils.setKey
import catgirlroutes.utils.PacketUtils
import catgirlroutes.utils.Utils
import catgirlroutes.utils.Utils.noControlCodes
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0CPacketInput
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S1BPacketEntityAttach
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs
import kotlin.math.pow

object InstaMid : Module(
    "Insta mid",
    category = Category.DUNGEON,
    description = "A module that instantly teleports you to Necron's platform."
){
    private var preparing = false
    private var active = false
    private var riding = false

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (!active || (event.packet !is C03PacketPlayer && event.packet !is C0CPacketInput)) return

        event.isCanceled = true
        riding = mc.thePlayer.isRiding

        if (riding) {
            preparing = false
            return
        }

        if (!preparing) {
            active = false
            preparing = true
            PacketUtils.sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(54.0, 65.0, 76.0, 0F, 0F, false))
            setKey("shift", false)
        }
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packet !is S1BPacketEntityAttach || !isOnPlatform() || event.packet.entityId != mc.thePlayer.entityId || event.packet.vehicleEntityId < 0) return
        preparing = true
        active = true

        modMessage("Attempting to instamid")
    }

    @SubscribeEvent
    fun onChat(event: PacketReceiveEvent) {
        if (event.packet !is S02PacketChat || event.packet.type.toInt() != 0 || !isOnPlatform()) return
        val message = event.packet.chatComponent.unformattedText.noControlCodes
        if (message == "[BOSS] Necron: You went further than any human before, congratulations.") {
            modMessage("Preparing to instamid")
            setKey("shift", true)
        }
    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !riding || mc.ingameGUI == null) return
        val sr = ScaledResolution(mc)
        val text = "Instamid active"
        val width = sr.scaledWidth / 2 - mc.fontRendererObj.getStringWidth(text) / 2
        Utils.renderText(
            text = text,
            x = width,
            y = sr.scaledHeight / 2 + 10
        )
    }

    private fun isOnPlatform(): Boolean {
        return mc.thePlayer?.let { player ->
            player.posY in 64.0..100.0 && abs(player.posX - 54.5).pow(2) + abs(player.posZ - 76.5).pow(2) < 56.25
        } ?: false
    }
}