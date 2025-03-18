package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.ReceiveChatPacketEvent
import catgirlroutes.events.impl.RenderEntityEvent
import catgirlroutes.events.impl.SecretPickupEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.*
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.PlayerUtils.playLoudSound
import catgirlroutes.utils.Utils.unformattedName
import catgirlroutes.utils.dungeon.DungeonUtils.dungeonItemDrops
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.render.WorldRenderUtils.drawBlock
import catgirlroutes.utils.render.WorldRenderUtils.drawEntityBox
import net.minecraft.entity.item.EntityItem
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object Secrets : Module( // todo: use secretevent
    "Secrets",
     Category.DUNGEON,
) {
    private val soundOptions = arrayListOf(
        "note.pling",
        "mob.blaze.hit",
        "fire.ignite",
        "random.orb",
        "random.break",
        "mob.guardian.land.hit",
        "Custom"
    )

    private val chimeDropdown = DropdownSetting("Chime dropdown")
    private val secretChime = BooleanSetting("Secret chime").withDependency(chimeDropdown)
    private val chimeSound = StringSelectorSetting("Chime sound", "note.pling", soundOptions, "Sound selection.").withDependency(chimeDropdown) { secretChime.enabled }
    private val chimeCustom = StringSetting("Custom chime sound", "note.pling", description = "Name of a custom sound to play. This is used when Custom is selected in the Sound setting.").withDependency(chimeDropdown) { chimeSound.selected == "Custom" && secretChime.enabled }

    private val dropSound = StringSelectorSetting("Drop sound", "note.pling", soundOptions, "Sound selection for item pickups.").withDependency(chimeDropdown) { secretChime.enabled  }
    private val dropCustom = StringSetting("Custom drop sound", "note.pling", description = "Name of a custom sound to play for item pickups. This is used when Custom is selected in the DropSound setting.").withDependency(chimeDropdown) { dropSound.selected == "Custom" && secretChime.enabled }

    private val highlightDropdown = DropdownSetting("Highlight dropdown")
    private val secretClicks = BooleanSetting("Secret clicks").withDependency(highlightDropdown)
    private val outline = BooleanSetting("Outline").withDependency(highlightDropdown) { secretClicks.enabled }
    private val clickColour = ColorSetting("Click colour", Color.GREEN).withDependency(highlightDropdown) { secretClicks.enabled }
    private val lockedColour = ColorSetting("Locked colour", Color.RED).withDependency(highlightDropdown){ secretClicks.enabled }

    private val itemDropdown = DropdownSetting("Item dropdown")
    private val itemHighlight = BooleanSetting("Item highlight").withDependency(itemDropdown)
    private val closeColour = ColorSetting("Close colour", Color.GREEN).withDependency(itemDropdown) { itemHighlight.enabled }
    private val farColour = ColorSetting("Far colour", Color.RED).withDependency(itemDropdown) { itemHighlight.enabled }
    private val playSound = BooleanSetting("Play sound").withDependency(itemDropdown) { itemHighlight.enabled }
    private val itemSound = StringSelectorSetting("Sound", "note.pling", soundOptions, "Sound selection.").withDependency(itemDropdown) { itemHighlight.enabled && playSound.enabled }
    private val itemCustom = StringSetting("Custom sound", "note.pling").withDependency(itemDropdown) { itemHighlight.enabled && playSound.enabled && itemSound.selected == "Custom" }

    private val volume = NumberSetting("Volume", 1.0, 0.0, 1.0, 0.01, "Volume of the sound.").withDependency { secretChime.enabled || playSound.enabled }
    private val pitch = NumberSetting("Pitch", 2.0, 0.0, 2.0, 0.01, "Pitch of the sound.").withDependency { secretChime.enabled || playSound.enabled}

    private data class Secret(val blockPos: BlockPos, var isLocked: Boolean = false)
    private val clickedSecrets = CopyOnWriteArrayList<Secret>()
    private var lastPlayed = System.currentTimeMillis()

    init {
        this.addSettings(
            chimeDropdown,
            secretChime,
            chimeSound,
            chimeCustom,
            dropSound,
            dropCustom,

            highlightDropdown,
            secretClicks,
            outline,
            clickColour,
            lockedColour,

            itemDropdown,
            itemHighlight,
            closeColour,
            farColour,
            playSound,
            itemSound,
            itemCustom,

            volume,
            pitch
        )
    }

    @SubscribeEvent
    fun onSecret(event: SecretPickupEvent) {
        if (!inDungeons) return
        if (event is SecretPickupEvent.Interact) secretHighlight(event.blockPos)
        playSecretSound(getSound(event is SecretPickupEvent.Item))
    }

    @SubscribeEvent
    fun onChat(event: ReceiveChatPacketEvent) {
        if (this.secretClicks.enabled || event.packet.chatComponent.unformattedText != "That chest is locked!") return
        this.clickedSecrets.lastOrNull()?.isLocked = true
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (!inDungeons) return

        if (this.clickedSecrets.isNotEmpty()) {
            this.clickedSecrets.forEach {
                val colour = if (it.isLocked) lockedColour.value else clickColour.value

                drawBlock(it.blockPos, colour, filled = true)
                if (this.outline.enabled) drawBlock(it.blockPos, colour.withAlpha(255))
            }
        }

        if (this.itemHighlight.enabled) {
            mc.theWorld.loadedEntityList.filter { it is EntityItem && dungeonItemDrops.contains(it.entityItem.unformattedName) }
                .forEach {
                    var colour = farColour.value

                    if (mc.thePlayer.getDistanceToEntity(it) < 3.5) {
                        playLoudSound(getSound(isHighlight = true), this.volume.value.toFloat() * 0.2f, this.pitch.value.toFloat())
                        colour = closeColour.value
                    }
                    drawEntityBox(it, colour, colour, false, true, event.partialTicks, 0.0f, 0.1f)
                }
        }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityEvent) {
        if (!inDungeons || inBoss || !this.itemHighlight.enabled) return
        if (event.entity !is EntityItem) return
        if (!dungeonItemDrops.contains((event.entity as EntityItem).entityItem.unformattedName)) return
        event.isCanceled = true
    }

    /**
     * Returns the sound from the selector setting, or the custom sound when the last element is selected
     */
    private fun getSound(isItemDrop: Boolean = false, isHighlight: Boolean = false): String {
        return if (isItemDrop && !isHighlight)
            if (dropSound.index < chimeSound.options.size - 1)
                dropSound.selected
            else
                dropCustom.text
        else if (!isHighlight)
            if (chimeSound.index < chimeSound.options.size - 1)
                chimeSound.selected
            else
                chimeCustom.text
        else if (itemSound.index < itemSound.options.size - 1)
            itemSound.selected
        else
            itemCustom.text
    }

    private fun playSecretSound(sound: String) {
        if (System.currentTimeMillis() - lastPlayed > 10 && this.secretChime.enabled) {
            playLoudSound(sound, volume.value.toFloat(), pitch.value.toFloat())
            lastPlayed = System.currentTimeMillis()
        }
    }

    private fun secretHighlight(blockPos: BlockPos) {
        if (!this.secretClicks.enabled || this.clickedSecrets.any { it.blockPos == blockPos }) return
        this.clickedSecrets.add(Secret(blockPos))
        scheduleTask(20) { this.clickedSecrets.removeFirstOrNull() }
    }
}