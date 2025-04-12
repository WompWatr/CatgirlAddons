package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import net.minecraft.network.play.server.S02PacketChat
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// taken from kp which is inspired by odon clint
object SkyblockPlayer {
    val health: Int
        get() = if (mc.thePlayer != null) (this.maxHealth * mc.thePlayer.health / mc.thePlayer.maxHealth).toInt() else 0
    var maxHealth: Int = 0
    var absorption: Int = 0

    var defence: Int = 0

    var mana: Int = 0
    var maxMana: Int = 0

    var overflowMana: Int = 0
    var stacks: String = ""
    var salvation: Int = 0

    var effectiveHealth: Int = 0
    val speed: Int
        get() = if (mc.thePlayer != null) (mc.thePlayer.capabilities.walkSpeed * 1000).toInt() else 0

    var manaUsage: String = ""

    var currentSecrets: Int = -1
    var maxSecrets: Int = -1

    val HP_REGEX = Regex("§[c6]([\\d,]+)/([\\d,]+)❤") // §c1389/1390❤ , §62181/1161❤
    val DEF_REGEX = Regex("§a([\\d,]+)§a❈ Defense") // §a593§a❈ Defense
    val MANA_REGEX = Regex("§b([\\d,]+)/([\\d,]+)✎( Mana)?") // §b550/550✎ Mana§r

    val OVERFLOW_REGEX = Regex("§3([\\d,]+)ʬ") // §3100ʬ
    val STACKS_REGEX = Regex("§6([0-9]+[ᝐ⁑Ѫ])") // §610⁑
    val SALVATION_REGEX = Regex("T([1-3])!") // no idea

    val MANA_USAGE_REGEX = Regex("§b-[\\d,]+ Mana \\(§6.+?§b\\)|§c§lNOT ENOUGH MANA") // §b-50 Mana (§6Speed Boost§b) , §c§lNOT ENOUGH MANA
    val SECRETS_REGEX = Regex("\\s*§7(\\d+)/(\\d+) Secrets") // §76/10 Secrets§r

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: PacketReceiveEvent) {
        if (event.packet !is S02PacketChat || event.packet.type.toInt() != 2) return
        val message = event.packet.chatComponent.formattedText.replace(",", "")

        HP_REGEX.find(message)?.destructured?.let { (abs, max) ->
            this.maxHealth = max.toInt()
            this.absorption = (abs.toInt() - max.toInt()).coerceAtLeast(0)
            this.effectiveHealth = this.maxHealth * (1 + this.defence / 100)
        }

        this.defence = DEF_REGEX.find(message)?.groupValues?.get(1)?.toIntOrNull() ?: this.defence

        MANA_REGEX.find(message)?.destructured?.let { (mana, maxMana) ->
            this.mana = mana.toInt()
            this.maxMana = maxMana.toInt()
        }

        this.overflowMana = OVERFLOW_REGEX.find(message)?.destructured?.component1()?.toInt() ?: 0

        this.stacks = STACKS_REGEX.find(message)?.destructured?.component1() ?: ""

        this.salvation = SALVATION_REGEX.find(message)?.destructured?.component1()?.toInt() ?: 0

        this.manaUsage = MANA_USAGE_REGEX.find(message)?.value ?: ""

        SECRETS_REGEX.find(message)?.destructured?.let { (current, max) ->
            this.currentSecrets = current.toInt()
            this.maxSecrets = max.toInt()
        } ?: run {
            this.currentSecrets = -1
            this.maxSecrets = -1
        }
    }
}
