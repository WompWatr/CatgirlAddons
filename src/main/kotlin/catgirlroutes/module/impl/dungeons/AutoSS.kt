package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.BlockChangeEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.Visibility
import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.LocationManager
import catgirlroutes.utils.clock.Executor
import catgirlroutes.utils.clock.Executor.Companion.register
import catgirlroutes.utils.render.WorldRenderUtils.drawCustomSizedBoxAt
import catgirlroutes.utils.render.WorldRenderUtils.drawStringInWorld
import catgirlroutes.utils.rotation.RotationUtils.getYawAndPitch
import catgirlroutes.utils.rotation.RotationUtils.rotateSmoothly
import catgirlroutes.utils.rotation.RotationUtils.targets
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.random.Random

/** @Author Kaze.0707**/

//Sponsored by chinese spyware

object AutoSS : Module(
    name = "AutoSS",
    Category.DUNGEON
){
    val delay: NumberSetting = NumberSetting("Delay", 200.0, 50.0, 500.0, 10.0, unit = "ms")
    private val forceDevice: BooleanSetting = BooleanSetting("Force Device", false, visibility = Visibility.ADVANCED_ONLY)
    private val resetSS: ActionSetting = ActionSetting("Reset SS") {reset(); doingSS = false; clicked = false}
    private val autoStart: NumberSetting = NumberSetting("Autostart delay", 125.0, 50.0, 200.0, 1.0, unit = "ms")
    private val smoothRotate: BooleanSetting = BooleanSetting("Rotate", false)
    private val time: NumberSetting = NumberSetting("Rotation Speed", 200.0, 0.0, 500.0, 10.0).withDependency { this.smoothRotate.enabled }
    private val dontCheck: BooleanSetting = BooleanSetting("Faster SS?", false)

    init {
        ssLoop()
        this.addSettings(delay, forceDevice, resetSS, autoStart, smoothRotate, time, dontCheck)
    }

    var lastClickAdded = System.currentTimeMillis()
    var next = false
    var progress = 0
    var doneFirst = false
    var doingSS = false
    var clicked = false
    var clicks = ArrayList<BlockPos>()
    var wtflip = System.currentTimeMillis()
    var clickedButton: Vec3? = null
    var allButtons = ArrayList<Vec3>()

    fun reset() {
        allButtons.clear()
        clicks.clear()
        next = false
        progress = 0
        doneFirst = false
        doingSS = false
        clicked = false
        debugMessage("Reset!")
    }

    override fun onKeyBind() {
        start()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        reset()
    }

    fun start() {
        allButtons.clear()
        val startButton: BlockPos = BlockPos(110, 121, 91)
        val (yaw, pitch) = getYawAndPitch(110.875, 121.5, 91.5)
        if (smoothRotate.value) {
            rotateSmoothly(yaw, pitch, time.value.toInt())
        }
        if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return
        if (!clicked) {
            debugMessage("Starting SS")
            debugMessage(System.currentTimeMillis())
            reset()
            clicked = true
            doingSS = true
            Thread{
                try {
                    for (i in 0 until 2) {
                        reset()
                        clickButton(startButton.x, startButton.y, startButton.z)
                        Thread.sleep(Random.nextInt(autoStart.value.toInt(), autoStart.value.toInt() * 1136 / 1000).toLong())
                    }
                    doingSS = true
                    clickButton(startButton.x, startButton.y, startButton.z)
                } catch (e: Exception) {
                    modMessage("NIGGER")
                }
            }.start()
        }
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        val msg = event.message.unformattedText
        val startButton: BlockPos = BlockPos(110, 121, 91)
        if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return
        if (msg.contains("Device")) {
            debugMessage(System.currentTimeMillis())
        }
        if (!msg.contains("Who dares trespass into my domain")) return
        debugMessage("Starting SS")
        start()
    }

    private fun ssLoop() {
        Executor(10) {
            if (System.currentTimeMillis() - lastClickAdded + 1 < delay.value) return@Executor
            if (mc.theWorld == null) return@Executor
            if (!this.enabled) return@Executor
            if (!LocationManager.inSkyblock && !forceDevice.value) return@Executor
            val detect: Block = mc.theWorld.getBlockState(BlockPos(110, 123, 92)).block
            val startButton: BlockPos = BlockPos(110, 121, 91)

            if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return@Executor

            var device = false

            mc.theWorld.loadedEntityList
                .filterIsInstance<EntityArmorStand>()
                .filter { it.getDistanceToEntity(mc.thePlayer) < 6 && it.displayName.unformattedText.contains("Device") }
                .forEach { _ ->
                    device = true
                }

            if (forceDevice.value) device = true

            if (!device) {
                clicked = false
                return@Executor
            }

            if ((detect == Blocks.stone_button || (dontCheck.value && doneFirst)) && doingSS) {
                if (!doneFirst && clicks.size == 3) {
                    clicks.removeAt(0)
                    allButtons.removeAt(0)
                }
                doneFirst = true
                if (progress < clicks.size) {
                    val next: BlockPos = clicks[progress]
                    if (mc.theWorld.getBlockState(next).block == Blocks.stone_button) {
                        if (smoothRotate.value) {
                            val (yaw, pitch) = getYawAndPitch(next.x.toDouble() + 0.875, next.y.toDouble() + 0.5, next.z.toDouble() + 0.5)
                            targets.add(Vec3(yaw.toDouble(), pitch.toDouble(), time.value))
                        }
                        clickButton(next.x, next.y, next.z)
                        progress++
                    }
                }
            }
        }.register()
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!this.enabled) return
        if (!LocationManager.inSkyblock && !forceDevice.value) return
        if (mc.theWorld == null) return

        val startButton: BlockPos = BlockPos(110, 121, 91)

        if (System.currentTimeMillis() - lastClickAdded > delay.value) clickedButton = null

        if (mc.thePlayer.getDistanceSqToCenter(startButton) < 1600) {
            if (clickedButton != null) {
                drawCustomSizedBoxAt(clickedButton!!.xCoord + 0.875, clickedButton!!.yCoord + 0.375, clickedButton!!.zCoord + 0.3125, 0.125, 0.25, 0.375, Color.PINK, filled = true)
            }
            allButtons.forEachIndexed{index, location ->
                drawStringInWorld((index + 1).toString(), Vec3(location.xCoord - 0.0625, location.yCoord + 0.5625, location.zCoord + 0.5), scale = 0.02f, shadow = true, depthTest = false)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val mop: MovingObjectPosition = mc.objectMouseOver ?: return
        if (System.currentTimeMillis() - wtflip < 1000) return
        wtflip = System.currentTimeMillis()
        val startButton: BlockPos = BlockPos(110, 121, 91)
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && startButton == event.pos && startButton == mop.blockPos && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            clicked = false
            reset()
            start()
        }
    }

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (event.pos.x == 111 && event.pos.y >= 120 && event.pos.y <= 123 && event.pos.z >= 92 && event.pos.z <= 95) {
            val button: BlockPos = BlockPos(110, event.pos.y, event.pos.z)
            if (event.update.block == Blocks.sea_lantern) {
                if (clicks.size == 2) {
                    if (clicks[0] == button && !doneFirst) {
                        doneFirst = true
                        clicks.removeFirst()
                        allButtons.removeFirst()
                    }
                }
                if (!clicks.contains(button)) {
                    debugMessage("Added to clicks: x: ${event.pos.x}, y: ${event.pos.y}, z: ${event.pos.z}")
                    progress = 0
                    clicks.add(button)
                    allButtons.add(Vec3(event.pos.x.toDouble(), event.pos.y.toDouble(), event.pos.z.toDouble()))
                }
            }
        }
    }

    fun clickButton(x: Int, y: Int, z: Int) {
        if (mc.thePlayer.getDistanceSqToCenter(BlockPos(x, y, z)) > 25) return
        debugMessage("Clicked at: x: ${x}, y: ${y}, z: ${z}. Time: ${System.currentTimeMillis()}")
        clickedButton = Vec3(x.toDouble(), y.toDouble(), z.toDouble())
        lastClickAdded = System.currentTimeMillis()
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(BlockPos(x, y, z), 4, mc.thePlayer.heldItem, 0.875f, 0.5f, 0.5f))
    }
}