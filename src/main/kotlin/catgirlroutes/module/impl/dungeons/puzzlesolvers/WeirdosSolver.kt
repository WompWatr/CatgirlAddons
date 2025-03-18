package catgirlroutes.module.impl.dungeons.puzzlesolvers

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.impl.dungeons.puzzlesolvers.Puzzles.weirdosSolver
import catgirlroutes.utils.BlockAura
import catgirlroutes.utils.BlockAura.blockArray
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.EntityAura
import catgirlroutes.utils.EntityAura.entityArray
import catgirlroutes.utils.Utils.noControlCodes
import catgirlroutes.utils.VecUtils.addRotationCoords
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoom
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoomName
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.render.WorldRenderUtils.drawBoxAtBlock
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos
import net.minecraft.util.StringUtils.stripControlCodes
import net.minecraftforge.client.event.ClientChatReceivedEvent
import java.awt.Color

object AutoWeirdos: Module(
    "Auto Weirdos",
    category = Category.DUNGEON
){

    private val solutions = listOf("The reward isn't in any of our chests.",
        "The reward is not in my chest!",
        "My chest doesn't have the reward. We are all telling the truth.",
        "My chest has the reward and I'm telling the truth!",
        "Both of them are telling the truth.",
        "At least one of them is lying, and the reward is not in"
    )
    private var bozos = mutableListOf<String>()
    private var clickedBozos = mutableListOf<Int>()
    private var removedChests = mutableListOf<BlockPos>()
    private var correctBozo: String? = null
    private var correctChest: BlockPos? = null
    private var addedChest: Boolean = false

    /**
     * Used to check incoming chat messages for solutions to the three weirdos puzzle.
     */
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2 || !inDungeons) return
        if (currentRoomName != "Three Weirdos") return
        val unformatted = stripControlCodes(event.message.unformattedText)
        if (unformatted.contains("[NPC]")) {
            val npcName = unformatted.substring(unformatted.indexOf("]") + 2, unformatted.indexOf(":"))
            var isSolution = false
            for (solution in solutions) {
                if (unformatted.contains(solution)) {
                    modMessage("§c§l${stripControlCodes(npcName)} §2has the blessing.")
                    isSolution = true
                    correctBozo = npcName
                    break
                }
            }
            // if the NPC message does not match a solution add that npcs name to the bozo list
            if (!isSolution) {
                if (!bozos.contains(npcName)) {
                    bozos.add(npcName)
                }
            }
        }
    }

    /**
     * Handles the removal of the chests in the room and puts the correct chest back.
     */
    fun onTick() {
        if (!inDungeons || !weirdosSolver.value) return
        if (currentRoomName != "Three Weirdos") return
        if (addedChest) return
        if (!Puzzles.weirdosAuto.value) return

        mc.theWorld.loadedEntityList
            .filter { it is EntityArmorStand && it.name.contains("CLICK") }
            .forEach { entity ->
                if (clickedBozos.contains(entity.entityId)) return@forEach
                entityArray.add(EntityAura.EntityAuraAction(entity, C02PacketUseEntity.Action.INTERACT_AT))
                clickedBozos.add(entity.entityId)
            }

        val correctNPC = mc.theWorld.loadedEntityList.find { it is EntityArmorStand && it.name.noControlCodes == correctBozo } ?: return
        val room = currentRoom ?: return
        correctChest = BlockPos(correctNPC.posX - 0.5, 69.0, correctNPC.posZ - 0.5).addRotationCoords(room.rotation, -1, 0)
        blockArray.add(BlockAura.BlockAuraAction(correctChest!!, 6.0))
        addedChest = true
    }

    fun renderWorld() {
        if (!inDungeons || !weirdosSolver.value) return
        if (currentRoomName != "Three Weirdos") return
        val correctNPC = mc.theWorld.loadedEntityList.find { it is EntityArmorStand && it.name.noControlCodes == correctBozo } ?: return
        val room = currentRoom ?: return
        correctChest = BlockPos(correctNPC.posX - 0.5, 69.0, correctNPC.posZ - 0.5).addRotationCoords(room.rotation, -1, 0)
        drawBoxAtBlock(correctChest!!, Color.GREEN)
    }

    /**
     * Resets the values when changing world.
     */
    fun onWorldChange() {
        bozos = mutableListOf()
        clickedBozos = mutableListOf()
        removedChests = mutableListOf<BlockPos>()
        correctBozo = null
        correctChest = null
        addedChest = false
    }
}