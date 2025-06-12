package catgirlroutes.utils.autop3.actions

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.impl.dungeons.Blink
import catgirlroutes.module.impl.dungeons.Blink.packetArray
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.autop3.RingsManager.blinkCd
import catgirlroutes.utils.autop3.RingsManager.blinkEditMode
import catgirlroutes.utils.customtriggers.TypeName
import net.minecraft.network.play.client.C03PacketPlayer

@TypeName("blink")
class BlinkRing(var packets: MutableList<Blink.BlinkC06> = mutableListOf()) : RingAction() {
//    override val description: String = "teleports the player"
    override fun execute(ring: Ring) {
        if (blinkEditMode) return
        if (packets.size == 0) return modMessage("Not enough packets.")
        if (packetArray >= packets.size && !blinkCd) {
            scheduleTask(0) {
                blinkCd = true
                packets.forEach { packet -> // todo make a util
                    mc.netHandler.networkManager.sendPacket(
                        C03PacketPlayer.C04PacketPlayerPosition(
                            packet.x,
                            packet.y,
                            packet.z,
                            packet.onGround
                        )
                    )
                    packetArray -= 1
                }
                mc.thePlayer.setPosition(packets.last().x, packets.last().y, packets.last().z)
            }
            scheduleTask(1) {
                blinkCd = false
            }
        }
    }
}
