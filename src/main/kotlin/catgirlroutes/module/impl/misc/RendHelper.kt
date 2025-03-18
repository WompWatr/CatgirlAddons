package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.utils.ChatUtils.commandAny
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.MovementUtils.jump
import catgirlroutes.utils.PlayerUtils.swapFromName
import com.sun.org.apache.xpath.internal.operations.Bool
import kotlinx.serialization.builtins.BooleanArraySerializer
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object RendHelper : Module(
    name = "Rend Helper",
    category = Category.MISC
) {
    val wardenSetting = BooleanSetting("Warden Swap", false)
    val rotationSetting = BooleanSetting("Rotate", false)

    init {
        this.addSettings(wardenSetting, rotationSetting)
    }

    var dpsPhase = false
    var clickedBone = true
    var clickedWarden = true
    var cancelWindow = false

    fun getWarden(): Int? {
        val container = mc.thePlayer.openContainer ?: return null
        for ((index, slot) in container.inventory.withIndex()) {
            if (slot != null && slot.displayName != null) {
                debugMessage(slot.displayName)
                if (slot.displayName.contains("Necrotic Warden Helmet")) {
                    return index
                }
            }
        }
        return null
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.message.unformattedText.contains("[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!")) {
            dpsPhase = true
            if (wardenSetting.value) {
                clickedBone = false
                clickedWarden = false
            }
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        dpsPhase = false
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            if (clickedBone) return
            val item = mc.thePlayer.heldItem.displayName
            if (item.contains("Bonemerang")) {
                clickedBone = true
                jump()
                    scheduleTask(1) {
                        swapFromName("Blade of the Volcano")
                        commandAny("eq")
                }
            }
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketReceiveEvent) {
        if (event.packet !is S2DPacketOpenWindow) return
        if (!dpsPhase) return
        if (cancelWindow) {
            cancelWindow = false
            event.isCanceled = true
            mc.thePlayer.closeScreen()
            return
        }
        if (clickedWarden) return
        debugMessage("OPEN")
        if (!event.packet.windowTitle.unformattedText.contains("Equipment")) return
        debugMessage("VALID")
        scheduleTask(3) {
            val slot = getWarden()
            if (slot == null) {
                modMessage("Warden Helmet not found!")
                mc.thePlayer.closeScreen()
                return@scheduleTask
            }
            debugMessage(slot)
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, 2, 3, mc.thePlayer)
            debugMessage("CLICKED")
            mc.thePlayer.closeScreen()
        }
        cancelWindow = true
        clickedWarden = true
    }

    @SubscribeEvent
    fun onTp(event: PacketReceiveEvent) {
        if (event.packet !is S08PacketPlayerPosLook) return
        if (event.packet.y in 5.9..6.1 && dpsPhase && rotationSetting.value) {

            val cubes = mc.theWorld.loadedEntityList
                .filterIsInstance<EntityMagmaCube>()

            val kuudra = cubes.find { cube -> cube.width.toInt() == 15 && cube.health <= 100000 }

            if (kuudra != null) {
                val x = kuudra.posX
                val z = kuudra.posZ
                var yaw = 0

                if (x < -128) {
                    yaw = 90
                } else if (x > -72) {
                    yaw = -90
                } else if (z < -132){
                    yaw = 179
                }

                scheduleTask(0) {
                    mc.thePlayer.rotationYaw = yaw.toFloat()
                    mc.thePlayer.rotationPitch = 0f
                }
            }
        }
    }
}