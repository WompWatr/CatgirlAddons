package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.ServerTickEvent
import catgirlroutes.module.Module
import catgirlroutes.module.settings.RegisterHudElement
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.hud.HudElement
import catgirlroutes.utils.dungeon.DungeonUtils.getMageCooldownMultiplier
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.render.HUDRenderUtils.renderRect
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.awt.Color

object InvincibilityTimer : Module(
    name = "Invincibility Timer"
) {
    private val dungeonOnly = BooleanSetting("Dungeons only", false)
    private val bossOnly = BooleanSetting("Boss only", false)
    private val serverTicks = BooleanSetting("Use server ticks", false)
    private val mageReduction = BooleanSetting("Account for mage cooldown reduction", false)
    private val cataLevel = NumberSetting("Catacombs level", 0.0, 0.0, 50.0, 1.0)

    init {
        this.addSettings(
            this.dungeonOnly,
            this.bossOnly,
            this.serverTicks,
            this.mageReduction,
            this.cataLevel
        )
    }

    var spiritTicks = 0
    var bonzoTicks = 0
    var phoenixTicks = 0
    var phoenix = false

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        val msg = event.message.unformattedText

        when {
            "Second Wind Activated! Your Spirit Mask saved your life!" in msg -> spiritTicks = (600 * if (this.mageReduction.enabled) getMageCooldownMultiplier() else 1.0).toInt()
            "Bonzo's Mask saved your life!" in msg -> bonzoTicks = ((7200 - cataLevel.value.toInt() * 72) * if (this.mageReduction.enabled) getMageCooldownMultiplier() else 1.0).toInt()
            "Your Phoenix Pet saved you from certain death!" in msg -> phoenixTicks = 1200
            "You summoned your" in msg || "Autopet equipped your" in msg -> phoenix = msg.contains("Phoenix")
            "You despawned your Phoenix!" in msg -> phoenix = false
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase !== TickEvent.Phase.START || serverTicks.enabled) return
        spiritTicks -= 1
        phoenixTicks -= 1
        bonzoTicks -= 1
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        if (!serverTicks.enabled) return
        this.spiritTicks -= 1
        this.phoenixTicks -= 1
        this.bonzoTicks -= 1
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        this.spiritTicks = 0
        this.bonzoTicks = 0
        this.phoenixTicks = 0
        this.phoenix = false
    }

    ////

    @RegisterHudElement
    object TimerHud : HudElement(
        this,
        0, 0,
        mc.fontRendererObj.getStringWidth("Phoenix: 30.2") + 6,
        (mc.fontRendererObj.FONT_HEIGHT + 2) * 3,
    ) {
        override fun renderHud() {
            if ((dungeonOnly.enabled && !inDungeons) || (bossOnly.enabled && !inBoss)) return

            val name = mc.thePlayer?.inventory?.armorInventory?.get(3)?.displayName
            val offset = when {
                name?.contains("Bonzo") == true -> 0.0
                name?.contains("Spirit") == true -> 10.0
                else -> 10000.0  // some shit to hide some shit
            }

            val bonzoReady = stupid("Bonzo", bonzoTicks, phoenix && offset == 0.0 && phoenixTicks < 0.0)
            val spiritReady = stupid("Spirit", spiritTicks, phoenix && offset == 10.0 && phoenixTicks < 0.0)
            val phoenixReady = stupid("Phoenix", phoenixTicks, phoenix && ((offset == 10.0 && spiritTicks < 0.0) || (offset == 0.0 && bonzoTicks < 0.0)))

            FontUtil.drawStringWithShadow(bonzoReady, 6.0, 0.0, Color.PINK.rgb)
            FontUtil.drawStringWithShadow(spiritReady, 6.0, 10.0, Color.PINK.rgb)
            FontUtil.drawStringWithShadow(phoenixReady, 6.0, 20.0, Color.PINK.rgb)

            renderRect(0.0, offset + 2.0, 3.0, 3.0, Color.PINK)
            if (phoenix) renderRect(0.0, 22.0, 3.0, 3.0, Color.PINK)
        }

        override fun preview() {
            FontUtil.drawStringWithShadow("Bonzo: §a✔", 6.0, 0.0, Color.PINK.rgb)
            FontUtil.drawStringWithShadow("Spirit: §a✔", 6.0, 10.0, Color.PINK.rgb)
            FontUtil.drawStringWithShadow("Phoenix: §c30.2", 6.0, 20.0, Color.PINK.rgb)
            renderRect(0.0, 2.0, 3.0, 3.0, Color.PINK)
        }

        private fun stupid(name: String, ticks: Int, highlight: Boolean): String {  // stupid thing for colour highlight
            val c = if (ticks < 0 && highlight) "§c" else if (ticks <= 0) "§a" else "§c"
            val time = if (ticks <= 0) "✔" else "%.1f".format(ticks / 20.0)
            return "$name: $c$time"
        }
    }
}