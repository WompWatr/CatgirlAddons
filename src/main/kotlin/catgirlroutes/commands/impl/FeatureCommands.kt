package catgirlroutes.commands.impl

import com.github.stivais.commodore.Commodore
import catgirlroutes.module.impl.dungeons.LavaClip
import catgirlroutes.module.impl.dungeons.SecretAura
import catgirlroutes.module.impl.misc.InventoryButtons
import catgirlroutes.module.impl.player.BlockClip
import catgirlroutes.module.impl.player.PearlClip
import catgirlroutes.utils.ChatUtils.modMessage

val pearlClip = Commodore("pearlclip") {
    runs { depth: Double? ->
        PearlClip.pearlClip(depth ?: 0.0)
    }
}

val lavaClip = Commodore("lavaclip") {
    runs { depth: Double? ->
        LavaClip.lavaClipToggle(depth ?: 0.0)
    }
}

val blockClip = Commodore("blockclip") {
    runs {  distance: Double? ->
        BlockClip.blockClip(distance ?: 1.0)
    }
}

val aura = Commodore("cgaaura") {

    literal("help").runs {
        modMessage("""
            List of AutoP3 commands:
              §7/cgaaura enable §8: §renables Secret Aura
              §7/cgaaura disable §8: §rdisables Secret Aura
              §7/cgaaura clear §8: §rclears clicked blocks
        """.trimIndent())
    }

    literal("enable").runs {
        if (SecretAura.enabled) return@runs
        SecretAura.onKeyBind()
    }

    literal("disable").runs {
        if (!SecretAura.enabled) return@runs
        SecretAura.onKeyBind()
    }

    literal("clear").runs {
        SecretAura.clearBlocks()
        modMessage("Blocks cleared!")
    }
}

val inventoryButtons = Commodore("cgabuttons") {
    runs {
        InventoryButtons.editMode.doAction()
    }
}


