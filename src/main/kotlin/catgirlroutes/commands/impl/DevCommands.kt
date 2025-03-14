package catgirlroutes.commands.impl

import catgirlroutes.CatgirlRoutes.Companion.mc
import com.github.stivais.commodore.Commodore
import catgirlroutes.module.impl.dungeons.puzzlesolvers.IceFillSolver
import catgirlroutes.utils.BlockAura
import catgirlroutes.utils.BlockAura.blockArray
import catgirlroutes.utils.BlockAura.breakArray
import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.EntityAura
import catgirlroutes.utils.EntityAura.entityArray
import catgirlroutes.utils.NeuRepo
import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.VecUtils.toVec3
import catgirlroutes.utils.dungeon.DungeonUtils
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoom
import catgirlroutes.utils.dungeon.DungeonUtils.getRealYaw
import catgirlroutes.utils.dungeon.DungeonUtils.getRelativeCoords
import catgirlroutes.utils.dungeon.DungeonUtils.getRelativeYaw
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos


val devCommands = Commodore("dev") {

    literal("help").runs { // todo: add description
        modMessage("""
            List of commands:
              §7/relativecoords
              §7/relativeyaw
              §7/realyaw
        """.trimIndent())
    }

    literal("relativecoords").runs {
        val block = mc.objectMouseOver?.blockPos ?: return@runs
        ChatUtils.chatMessage(
            """
            ---------
            Middle: $block
            Relative Coords: ${DungeonUtils.currentRoom?.getRelativeCoords(block.toVec3())?.toString()}
            --------
            """.trimIndent())
    }

    literal("relativeyaw").runs {
        val room = currentRoom ?: return@runs
        ChatUtils.chatMessage(
            """
            ---------
            Player Yaw: ${mc.thePlayer.rotationYaw}
            Relative Yaw: ${room.getRelativeYaw(mc.thePlayer.rotationYaw)}
            --------
            """.trimIndent())
    }

    literal("realyaw").runs {
        val room = currentRoom ?: return@runs
        ChatUtils.chatMessage(
            """
            ---------
            Player Yaw: ${mc.thePlayer.rotationYaw}
            Relative Yaw: ${room.getRealYaw(mc.thePlayer.rotationYaw)}
            --------
            """.trimIndent())
    }

    literal("icefill").runs {
        ChatUtils.chatMessage(
            """
            ---------
            IceFill solution: ${IceFillSolver.currentPatterns}
            --------
            """.trimIndent())
    }

    literal("icefillreset").runs {
        IceFillSolver.reset()
        ChatUtils.chatMessage("Resetting fill")
    }

    literal("swaptest").runs {
        swapFromName("orange")
        swapFromName("magenta")
        swapFromName("light blue")
        swapFromName("yellow")
        swapFromName("lime")
        scheduleTask(0) {
            swapFromName("pink")
            swapFromName("gray")
            swapFromName("light gray")
            swapFromName("cyan")
        }
    }
    literal("entityaura").runs {
        mc.theWorld.loadedEntityList.forEach{entity ->
            if (entity is EntityHorse || entity is EntitySlime || entity is EntityArmorStand) entityArray.add(EntityAura.EntityAuraAction(entity, C02PacketUseEntity.Action.INTERACT_AT))
        }
    }
    literal("blockaura").runs {
        val eyePos = mc.thePlayer.getPositionEyes(0f)
        val blockPos1: BlockPos = BlockPos(eyePos.xCoord - 20, eyePos.yCoord - 20, eyePos.zCoord - 20)
        val blockPos2: BlockPos = BlockPos(eyePos.xCoord + 20, eyePos.yCoord + 20, eyePos.zCoord + 20)
        val blocks = BlockPos.getAllInBox(blockPos1, blockPos2)
        for (block in blocks) {
            val blockstate = mc.theWorld.getBlockState(block)
            if (blockstate.block == Blocks.chest || blockstate.block == Blocks.lever || blockstate.block == Blocks.emerald_block || blockstate.block == Blocks.stone_button) blockArray.add(
                BlockAura.BlockAuraAction(block, 6.0))
        }
    }
    literal("breakaura").runs {
        val eyePos = mc.thePlayer.getPositionEyes(0f)
        val blockPos1: BlockPos = BlockPos(eyePos.xCoord - 20, eyePos.yCoord - 20, eyePos.zCoord - 20)
        val blockPos2: BlockPos = BlockPos(eyePos.xCoord + 20, eyePos.yCoord + 20, eyePos.zCoord + 20)
        val blocks = BlockPos.getAllInBox(blockPos1, blockPos2)
        for (block in blocks) {
            val blockstate = mc.theWorld.getBlockState(block)
            if (blockstate.block == Blocks.chest || blockstate.block == Blocks.lever || blockstate.block == Blocks.emerald_block || blockstate.block == Blocks.stone_button) breakArray.add(block)
        }
    }
    literal("moveentity").runs { x: Double, y: Double, z: Double ->
        mc.thePlayer.moveEntity(x, y, z)
    }
    literal("ystop").runs {
        mc.thePlayer.motionY = 0.0
    }

    literal("repoinfo").runs {
        ChatUtils.chatMessage("""
            ---------------
            Items: ${NeuRepo.repoItems.size}
            |Bazaar: ${NeuRepo.repoItems.filter { it.bazaar }.size}
            |Auction: ${NeuRepo.repoItems.filter { it.auction }.size}
            Mobs: ${NeuRepo.mobs.size}
            Constants: ${NeuRepo.constants.size}
            ---------------
        """.trimIndent())
    }
}