package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.StringSelectorSetting
import catgirlroutes.module.settings.impl.StringSetting
import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.MovementUtils.clearBlocks
import catgirlroutes.utils.MovementUtils.moveToBlock
import catgirlroutes.utils.MovementUtils.movementKeysDown
import catgirlroutes.utils.MovementUtils.targetBlocks
import catgirlroutes.utils.PacketUtils.sendPacket
import catgirlroutes.utils.PlayerUtils.airClick
import catgirlroutes.utils.PlayerUtils.isHolding
import catgirlroutes.utils.Utils
import catgirlroutes.utils.Utils.lore
import catgirlroutes.utils.Utils.removeFormatting
import catgirlroutes.utils.VecUtils.toVec3
import catgirlroutes.utils.dungeon.DungeonClass
import catgirlroutes.utils.dungeon.DungeonUtils.dungeonTeammatesNoSelf
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.dungeon.LeapUtils.leap
import catgirlroutes.utils.render.WorldRenderUtils.drawCustomSizedBoxAt
import catgirlroutes.utils.render.WorldRenderUtils.drawSquare
import catgirlroutes.utils.rotation.FakeRotater.clickAt
import catgirlroutes.utils.rotation.RotationUtils.getYawAndPitch
import catgirlroutes.utils.rotation.RotationUtils.snapTo
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import net.minecraft.util.StringUtils
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color

object Auto4: Module(
    "Auto 4",
    category = Category.DUNGEON,
    description = "Automatically does fourth dev.",
    tag = TagType.WHIP
){

    private val autoPlate = BooleanSetting("Auto plate")

    private val aimSnap = BooleanSetting("Aim snapping")

    private val statusMessages = BooleanSetting("Status messages")

    private val deviceTextOn = StringSetting("On text")
    private val deviceTextOff = StringSetting("Off text")

    private var autoLeap = BooleanSetting("Auto leap")
    private var leapMode = StringSelectorSetting("Leap mode", "Name", arrayListOf("Name", "Class")).withDependency {autoLeap.enabled}

    private var devLeap = StringSelectorSetting("Leap name", "None", arrayListOf("None")).withDependency { leapMode.selected == "Name" && autoLeap.enabled}
    private var action = ActionSetting("update") { updateTeammates() }.withDependency { leapMode.selected == "Name" && autoLeap.enabled}

    private var devLeapClass = StringSelectorSetting("Leap class", "None", arrayListOf("Mage", "Bers", "Arch", "Tank", "Heal", "None")).withDependency { leapMode.selected == "Class" && autoLeap.enabled}

    private val forceTerm = BooleanSetting("Force term")

    init {
        addSettings(autoPlate, aimSnap, statusMessages, deviceTextOn, deviceTextOff, autoLeap, leapMode, devLeap, action, devLeapClass, forceTerm)
    }

    private fun updateTeammates() {
        val teammates = arrayListOf<String>()
        if (dungeonTeammatesNoSelf.isEmpty()) return
        dungeonTeammatesNoSelf.forEach{ teammate ->
            teammates.add(teammate.name)
        }
        teammates.add("None")
        devLeap.options = teammates
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!inDungeons || event.type.toInt() == 2 || !inBoss || !onDev()) return
        val message = StringUtils.stripControlCodes(event.message.unformattedText)
        if (message.matches(Regex("(\\w+) completed a device! \\((.*?)\\)"))) {
            if (leapMode.selected == "Name" && autoLeap.value) leap(devLeap.selected)
            else if (leapMode.selected == "Class" && autoLeap.value) leap(classEnumMapping[devLeapClass.index])
            if (statusMessages.value) ChatUtils.commandAny("/pc [CGA] I4 completed")
        } else if (message.matches(Regex("☠ (\\w{1,16}) .* and became a ghost\\."))) {
            ChatUtils.commandAny("/pc [CGA] I4 not completed")
        }
    }

    data class BowStats (
        var isTerm: Boolean,
        var cooldown: Int
    )

    private val devBlocks = listOf(
        BlockPos(64, 126, 50),
        BlockPos(66, 126, 50),
        BlockPos(68, 126, 50),
        BlockPos(64, 128, 50),
        BlockPos(66, 128, 50),
        BlockPos(68, 128, 50),
        BlockPos(64, 130, 50),
        BlockPos(66, 130, 50),
        BlockPos(68, 130, 50)
    )

    private val classEnumMapping = arrayListOf(DungeonClass.Mage, DungeonClass.Berserk, DungeonClass.Archer, DungeonClass.Tank, DungeonClass.Healer, DungeonClass.Unknown)

    private fun onDev(): Boolean {
        return mc.thePlayer.posX in 62.0..65.0 && mc.thePlayer.posY == 127.0 && mc.thePlayer.posZ in 34.0..37.0
    }

    private fun platePressed(): Boolean {
        val plate = mc.theWorld.getBlockState(BlockPos(63, 127, 35))
        if (plate.block != Blocks.light_weighted_pressure_plate) return false
        return plate.block.getMetaFromState(plate) > 0
    }

    var doneBlocks = mutableListOf<Vec3>()

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!inDungeons) return
        if (platePressed()) {
            drawSquare(63.5, 127.01, 35.5, 3.0, 3.0, Color.GREEN, phase = false)
            if (currentBlock != null && !doneBlocks.contains(currentBlock!!.toVec3())) doneBlocks.add(currentBlock!!.toVec3())
            doneBlocks.forEach {block ->
                drawCustomSizedBoxAt(block.xCoord, block.yCoord, block.zCoord, 1.0, 1.0, 1.0, Color.RED, filled = true)
            }
            if (currentBlock == null) return
            val block = currentBlock!!.toVec3()
            drawCustomSizedBoxAt(block.xCoord, block.yCoord, block.zCoord, 1.0, 1.0, 1.0, Color.GREEN, filled = true)
            if (forceTerm.value) {
                if (block.xCoord == 64.0 && !doneBlocks.contains(Vec3(block.xCoord + 2, block.yCoord, block.zCoord))) drawCustomSizedBoxAt(block.xCoord + 2, block.yCoord, block.zCoord, 1.0, 1.0, 1.0, Color.ORANGE, filled = true)
                else if (block.xCoord != 64.0 && !doneBlocks.contains(Vec3(block.xCoord - 2, block.yCoord, block.zCoord))) drawCustomSizedBoxAt(block.xCoord - 2, block.yCoord, block.zCoord, 1.0, 1.0, 1.0, Color.ORANGE, filled = true)
            }
        }
        else if (onDev())  {
            drawSquare(63.5, 127.01, 35.5, 3.0, 3.0, Color.ORANGE, phase = false)
        }
        else {
            doneBlocks = mutableListOf()
            currentBlock = null
            drawSquare(63.5, 127.01, 35.5, 3.0, 3.0, Color.RED, phase = false)
        }


    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !onDev() || mc.ingameGUI == null || !inDungeons) return

        var text = "${deviceTextOff.value} ${doneBlocks.size}/9"
        var color = 0xFA5F55
        if (platePressed()) {
            text = "${deviceTextOn.value} ${doneBlocks.size}/9"
            color = 0x00FF7F
        }
        val sr = ScaledResolution(mc)
        val scale = 1.5
        val textWidth = mc.fontRendererObj.getStringWidth(text) * scale
        val x = (sr.scaledWidth / 2 - textWidth / 2).toInt()
        val y = sr.scaledHeight / 2 - 28
        Utils.renderText(text, x, y, scale, color)
    }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Ava is such a good girl
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || !inDungeons) return
        if (!onDev() || !autoPlate.value) return
        if (mc.thePlayer.posX == 63.5 && mc.thePlayer.posZ == 35.5) return
        if (movementKeysDown()) {
            clearBlocks()
            return
        }
        moveToBlock(63.5, 35.5)
    }

    var cancelNext = false

    private fun shootBlock() {
        if (currentBlock == null || !onDev() || !inDungeons) return
        val coords = aimCoords(currentBlock!!)
        val rotation = getYawAndPitch(coords.xCoord, coords.yCoord, coords.zCoord)
        if (aimSnap.value) snapTo(rotation.first, rotation.second)
        //clickAt(rotation.first, rotation.second)
        sendPacket(C05PacketPlayerLook(rotation.first, rotation.second, mc.thePlayer.onGround))
        cancelNext = true
        airClick()
    }

    private fun aimCoords(coords: BlockPos): Vec3 {
        val offsetMap = mapOf(
            68 to -0.7,
            66 to -0.5,
            64 to 1.5
        )

        var xoffset = offsetMap[coords.x]
        if (!forceTerm.value) xoffset = 0.5

        return Vec3(coords.x + xoffset!!, coords.y + 1.1, coords.z.toDouble())
    }

    private fun getBow(): BowStats {
        val isTerm = isHolding("TERMINATOR")
        var shotSpeed = 300
        val lore = mc.thePlayer.heldItem.lore
        lore.forEach{line ->
            val regex = Regex("Shot Cooldown: (\\d\\.?\\d+)s")
            val regexMatch = regex.find(removeFormatting(line))
            if (regexMatch != null) shotSpeed = regexMatch.value.toInt() * 1000
        }
        return BowStats(isTerm, shotSpeed)
    }

    var currentBlock: BlockPos? = null

    @SubscribeEvent
    fun onPacket(event: PacketReceiveEvent) {
        if (event.packet is S23PacketBlockChange) {
            if (!devBlocks.contains(event.packet.blockPosition) || event.packet.blockState.block != Blocks.emerald_block) return
            currentBlock = event.packet.blockPosition
            shootBlock()
            return
        } else if (event.packet is S22PacketMultiBlockChange) {
            event.packet.changedBlocks.forEach {block ->
                if (!devBlocks.contains(block.pos) || block.blockState.block != Blocks.emerald_block) return@forEach
                currentBlock = block.pos
                shootBlock()
                return
            }
        } else if (event.packet is S08PacketPlayerPosLook && autoPlate.enabled && targetBlocks.isNotEmpty() && targetBlocks.first() == Vec3(63.5, 0.0, 35.5)) {
            clearBlocks()
        } else if (event.packet is C03PacketPlayer && cancelNext) {
            cancelNext = false
            event.isCanceled = true
        }
    }
}