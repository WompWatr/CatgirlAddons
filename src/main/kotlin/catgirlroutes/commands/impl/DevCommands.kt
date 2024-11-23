package catgirlroutes.commands.impl

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.commands.commodore
import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.dungeon.DungeonUtils.currentFullRoom
import catgirlroutes.utils.dungeon.DungeonUtils.getRealYaw
import catgirlroutes.utils.dungeon.DungeonUtils.getRelativeYaw
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import me.odinmain.utils.toVec3


val devCommands = commodore("dev") {

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
        val room = currentFullRoom ?: return@runs
        ChatUtils.chatMessage(
            """
            ---------
            Player Yaw: ${mc.thePlayer.rotationYaw}
            Relative Yaw: ${room.getRelativeYaw(mc.thePlayer.rotationYaw)}
            --------
            """.trimIndent())
    }

    literal("realyaw").runs {
        val room = currentFullRoom ?: return@runs
        ChatUtils.chatMessage(
            """
            ---------
            Player Yaw: ${mc.thePlayer.rotationYaw}
            Relative Yaw: ${room.getRealYaw(mc.thePlayer.rotationYaw)}
            --------
            """.trimIndent())
    }
}