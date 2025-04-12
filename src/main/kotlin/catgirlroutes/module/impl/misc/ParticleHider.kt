package catgirlroutes.module.impl.misc

import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.utils.dungeon.DungeonUtils
import catgirlroutes.utils.dungeon.M7Phases
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ParticleHider : Module(
    "Particle hider",
    Category.MISC
) {
    @SubscribeEvent
    fun onPacket(event: PacketReceiveEvent) { event.isCanceled = event.packet is S2APacketParticles && DungeonUtils.getF7Phase() != M7Phases.P5 }
}