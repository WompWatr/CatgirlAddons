package catgirlroutes.module.impl.player

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.RegisterHudElement
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.DropdownSetting
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.hud.HudElement
import catgirlroutes.utils.LocationManager.inSkyblock
import catgirlroutes.utils.SkyblockPlayer
import catgirlroutes.utils.SkyblockPlayer.DEF_REGEX
import catgirlroutes.utils.SkyblockPlayer.HP_REGEX
import catgirlroutes.utils.SkyblockPlayer.MANA_REGEX
import catgirlroutes.utils.SkyblockPlayer.MANA_USAGE_REGEX
import catgirlroutes.utils.SkyblockPlayer.OVERFLOW_REGEX
import catgirlroutes.utils.SkyblockPlayer.SALVATION_REGEX
import catgirlroutes.utils.SkyblockPlayer.SECRETS_REGEX
import catgirlroutes.utils.SkyblockPlayer.STACKS_REGEX
import catgirlroutes.utils.SkyblockPlayer.currentSecrets
import catgirlroutes.utils.SkyblockPlayer.maxSecrets
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.render.HUDRenderUtils.drawItemStackWithText
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedOutline
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedRect
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.text.NumberFormat
import java.util.*


object PlayerDisplay: Module(
    name = "Player Display",
    category = Category.PLAYER,
    tag = TagType.WHIP
) {

    private val hideHealth = BooleanSetting("Hide health")
    private val hideArmour = BooleanSetting("Hide armour")
    private val hideHunger = BooleanSetting("Hide hunger")

    private val healthDropdown = DropdownSetting("Health dropdown")
    private val health = BooleanSetting("Health").withDependency(healthDropdown)
    private val healthColour = ColorSetting("Health colour", Color(255,85,85), false).withDependency(healthDropdown) { health.enabled }
    private val healthBar = BooleanSetting("Health bar").withDependency(healthDropdown)
    private val healthBarColour = ColorSetting("Health bar colour", Color(255,85,85)).withDependency(healthDropdown) { healthBar.enabled }
    private val effectiveHealth = BooleanSetting("Effective health").withDependency(healthDropdown)

    private val manaDropdown = DropdownSetting("Mana dropdown")
    private val mana = BooleanSetting("Mana").withDependency(manaDropdown)
    private val manaColour = ColorSetting("Mana colour", Color(85, 85, 255), false).withDependency(manaDropdown) { mana.enabled }
    private val manaBar = BooleanSetting("Mana bar").withDependency(manaDropdown)
    private val manaBarColour = ColorSetting("Mana bar colour", Color(85, 85, 255)).withDependency(manaDropdown) { manaBar.enabled }
    private val manaUsage = BooleanSetting("Mana usage").withDependency(manaDropdown)
    private val overflowMana = BooleanSetting("Overflow mana").withDependency(manaDropdown)

    private val otherDropdown = DropdownSetting("Other")
    private val defence = BooleanSetting("Defence").withDependency(otherDropdown)
    private val defenceColour = ColorSetting("Defence colour", Color( 85, 255, 85), false).withDependency(otherDropdown) { defence.enabled }
    private val speed = BooleanSetting("Speed").withDependency(otherDropdown)
    private val stacks = BooleanSetting("Crimson stacks").withDependency(otherDropdown)
    private val salvation = BooleanSetting("Salvation").withDependency(otherDropdown)
    private val secrets = BooleanSetting("Secret display").withDependency(otherDropdown)
    private val sbaStyle = BooleanSetting("SBA secrets style").withDependency(otherDropdown) { secrets.enabled }

    private val barWidth = 75.0
    private val barHeight = 7.0

    init {
        addSettings(
            this.hideHealth,
            this.hideArmour,
            this.hideHunger,

            this.healthDropdown,
            this.health,
            this.healthColour,
            this.healthBar,
            this.healthBarColour,
            this.effectiveHealth,

            this.manaDropdown,
            this.mana,
            this.manaColour,
            this.manaBar,
            this.manaBarColour,
            this.manaUsage,
            this.overflowMana,

            this.otherDropdown,
            this.defence,
            this.defenceColour,
            this.speed,
            this.stacks,
            this.salvation,
            this.secrets,
            this.sbaStyle
        )
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Pre) {
        if (event.isCanceled) return
        if (!inSkyblock) return

        event.isCanceled = when (event.type) {
            RenderGameOverlayEvent.ElementType.HEALTH -> hideHealth.enabled
            RenderGameOverlayEvent.ElementType.ARMOR -> hideArmour.enabled
            RenderGameOverlayEvent.ElementType.FOOD -> hideHunger.enabled
            else -> return
        }
    }

    fun modifyActionBar(text: String): String {
        if (!this.enabled) return text
        var toReturn = text

        if (health.enabled) {
            toReturn = toReturn.replace(HP_REGEX, "")
        }
        if (mana.enabled) {
            toReturn = toReturn.replace(MANA_REGEX, "")
        }
        if (overflowMana.enabled) {
            toReturn = toReturn.replace(OVERFLOW_REGEX, "")
        }
        if (defence.enabled) {
            toReturn = toReturn.replace(DEF_REGEX, "")
        }
        if (stacks.enabled) {
            toReturn = toReturn.replace(STACKS_REGEX, "")
        }
        if (salvation.enabled) {
            toReturn = toReturn.replace(SALVATION_REGEX, "")
        }
        if (manaUsage.enabled) {
            toReturn = toReturn.replace(MANA_USAGE_REGEX, "")
        }
        if (secrets.enabled) {
            toReturn = toReturn.replace(SECRETS_REGEX, "")
        }

        return toReturn
    }

    @RegisterHudElement
    object HealthHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("1,200/10,900"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("1,200/10,900", 0.0, 0.0, healthColour.value.rgb) }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !health.enabled) return
            FontUtil.drawStringWithShadow("${(SkyblockPlayer.health + SkyblockPlayer.absorption).commas()}/${SkyblockPlayer.maxHealth.commas()}", 0.0, 0.0, healthColour.value.rgb)
        }
    }

    @RegisterHudElement
    object HealthBarHud : HudElement(
        this, 0, 0,
        barWidth.toInt(), barHeight.toInt() + 4,
        preview = {
            drawRoundedRect(0.0, 0.0, barWidth, barHeight, 5.0, Color(ColorUtil.bgColor))
            drawRoundedRect(0.0, 0.0, 50.0, barHeight, 5.0, healthBarColour.value)
            drawRoundedOutline(0.0, 0.0, barWidth, barHeight, 5.0, 1.0, Color(208, 208, 208))
        }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !healthBar.enabled) return
            val fillWidth = SkyblockPlayer.health.toFloat() / SkyblockPlayer.maxHealth.toFloat() * barWidth
            drawRoundedRect(0.0, 0.0, barWidth, barHeight, 5.0, Color(ColorUtil.bgColor))
            drawRoundedRect(0.0, 0.0, fillWidth, barHeight, 5.0, healthBarColour.value)
            drawRoundedOutline(0.0, 0.0, barWidth, barHeight, 5.0, 1.0, Color(208, 208, 208))
        }
    }

    @RegisterHudElement
    object EffectiveHealthHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("54,879"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("54,879", 0.0, 0.0) }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !effectiveHealth.enabled) return
            FontUtil.drawStringWithShadow(SkyblockPlayer.effectiveHealth.commas(), 0.0, 0.0)
        }
    }

    @RegisterHudElement
    object ManaHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("1,300/10,900"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("1,300/10,900", 0.0, 0.0, manaColour.value.rgb) }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !mana.enabled) return
            FontUtil.drawStringWithShadow("${SkyblockPlayer.mana.commas()}/${SkyblockPlayer.maxMana.commas()}", 0.0, 0.0, manaColour.value.rgb)
        }
    }

    @RegisterHudElement
    object ManaBarHud : HudElement(
        this, 0, 0,
        barWidth.toInt(), barHeight.toInt() + 4,
        preview = {
            drawRoundedRect(0.0, 0.0, barWidth, barHeight, 5.0, Color(ColorUtil.bgColor))
            drawRoundedRect(0.0, 0.0, 50.0, barHeight, 5.0, manaBarColour.value)
            drawRoundedOutline(0.0, 0.0, barWidth, barHeight, 5.0, 1.0, Color(208, 208, 208))
        }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !manaBar.enabled) return
            val fillWidth = SkyblockPlayer.mana.toFloat() / SkyblockPlayer.maxMana.toFloat() * barWidth
            drawRoundedRect(0.0, 0.0, barWidth, barHeight, 5.0, Color(ColorUtil.bgColor))
            drawRoundedRect(0.0, 0.0, fillWidth, barHeight, 5.0, manaBarColour.value)
            drawRoundedOutline(0.0, 0.0, barWidth, barHeight, 5.0, 1.0, Color(208, 208, 208))
        }
    }

    @RegisterHudElement
    object ManaUsageHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("§b-50 Mana (§6Speed Boost§b)"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("§b-50 Mana (§6Speed Boost§b)", 0.0, 0.0) }

    ) {
        override fun renderHud() {
            if (!inSkyblock || !manaUsage.enabled || SkyblockPlayer.manaUsage.isEmpty()) return
            FontUtil.drawStringWithShadow(SkyblockPlayer.manaUsage, 0.0, 0.0)
        }
    }

    @RegisterHudElement
    object OverflowManaHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("600ʬ"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("600ʬ", 0.0, 0.0) }

    ) {
        override fun renderHud() {
            if (!inSkyblock || !overflowMana.enabled || SkyblockPlayer.overflowMana == 0) return
            FontUtil.drawStringWithShadow("${SkyblockPlayer.overflowMana.commas()}ʬ", 0.0, 0.0)
        }
    }

    @RegisterHudElement
    object DefenceHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("10,000"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("10,000", 0.0, 0.0, defenceColour.value.rgb) }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !defence.enabled) return
            FontUtil.drawStringWithShadow(SkyblockPlayer.defence.commas(), 0.0, 0.0, defenceColour.value.rgb)
        }
    }

    @RegisterHudElement
    object SpeedHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("✦500"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("✦500", 0.0, 0.0) }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !speed.enabled) return
            FontUtil.drawStringWithShadow("✦${SkyblockPlayer.speed}", 0.0, 0.0)
        }
    }

    @RegisterHudElement
    object StacksHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("10ᝐ"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("10ᝐ", 0.0, 0.0) }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !stacks.enabled || SkyblockPlayer.stacks.isEmpty()) return
            FontUtil.drawStringWithShadow(SkyblockPlayer.stacks, 0.0, 0.0)
        }
    }

    @RegisterHudElement
    object SalvationHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("T3!"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = { FontUtil.drawStringWithShadow("T3!", 0.0, 0.0) }
    ) {
        override fun renderHud() {
            if (!inSkyblock || !salvation.enabled || SkyblockPlayer.salvation == 0) return
            FontUtil.drawStringWithShadow("T${SkyblockPlayer.salvation}!", 0.0, 0.0)
        }
    }

    @RegisterHudElement
    object SecretsHud : HudElement(
        this, 0, 0,
        mc.fontRendererObj.getStringWidth("5/10 Secrets"),
        mc.fontRendererObj.FONT_HEIGHT + 2,
        preview = {
            if (sbaStyle.enabled) {
                drawItemStackWithText(ItemStack(Blocks.chest), 0.0, 0.0)
                FontUtil.drawStringWithShadow("§7Secrets", 16.0, 0.0)
                val textWidth = FontUtil.getStringWidth("Secrets")
                val totalWidth = FontUtil.getStringWidth("5/10")
                FontUtil.drawStringWithShadow("§e5§7/§e10", 16.0 + 2.0 + textWidth / 2.0 - totalWidth / 2.0, 11.0)
            } else {
                FontUtil.drawStringWithShadow("§e5/10 Secrets", 0.0, 0.0)
            }
        }
    ) {
        override fun renderHud() {
            if (!inDungeons || inBoss || !secrets.enabled) return
            val colour = when (currentSecrets / maxSecrets.toDouble()) {
                in 0.0..0.5 -> "§c"
                in 0.5..0.75 -> "§e"
                else -> "§a"
            }
            if (sbaStyle.enabled) {
                drawItemStackWithText(ItemStack(Blocks.chest), 0.0, 0.0)
                FontUtil.drawStringWithShadow("§7Secrets", 16.0, 0.0)
                val secretsText = if (currentSecrets > -1) "$colour$currentSecrets§7/$colour$maxSecrets" else "§7Unknown"
                val textWidth = FontUtil.getStringWidth("Secrets")
                val totalWidth = FontUtil.getStringWidth(secretsText)
                FontUtil.drawStringWithShadow(secretsText, 16.0 + 2.0 + textWidth / 2.0 - totalWidth / 2.0, 11.0)
            } else if (currentSecrets > -1) {
                FontUtil.drawStringWithShadow("$colour$currentSecrets/$maxSecrets Secrets", 0.0, 0.0)
            }
        }

        override fun setDimensions() {
            if (sbaStyle.enabled) {
                this.width = 16 + 2 + FontUtil.getStringWidth("Secrets")
                this.height = FontUtil.fontHeight * 2 + 2
            } else {
                this.width = FontUtil.getStringWidth("5/10 Secrets")
                this.height = FontUtil.fontHeight + 2
            }
        }
    }

    private fun Number.commas(): String = NumberFormat.getInstance(Locale.US).format(this)

    fun String.formatMana(): String = this.split(" (", ")").let { "§b${it[0]} (§6${it[1]}§b)" } // temp maybe
}