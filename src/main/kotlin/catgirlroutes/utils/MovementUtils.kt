package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.mc
import com.github.stivais.commodore.Commodore
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.MovementUtils.addBlock
import catgirlroutes.utils.MovementUtils.clearBlocks
import catgirlroutes.utils.MovementUtils.moveToBlock
import catgirlroutes.utils.rotation.RotationUtils.getYawAndPitch
import net.minecraft.block.Block
import net.minecraft.client.settings.GameSettings.isKeyDown
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object MovementUtils {
    fun setKey(key: String, down: Boolean) {
        when(key) {
            "w" -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.keyCode, down)
            "a" -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.keyCode, down)
            "s" -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.keyCode, down)
            "d" -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.keyCode, down)
            "jump" -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.keyCode, down)
            "shift" -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, down)
            "sprint" -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, down)
        }
    }

    fun movementKeysDown(): Boolean {
        if (mc.gameSettings.keyBindForward.isKeyDown) return true
        if (mc.gameSettings.keyBindLeft.isKeyDown) return true
        if (mc.gameSettings.keyBindRight.isKeyDown) return true
        if (mc.gameSettings.keyBindBack.isKeyDown) return true
        if (mc.gameSettings.keyBindJump.isKeyDown) return true
        return false
    }

    fun stopVelo() {
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
    }

    fun stopMovement() {
        setKey("w", false)
        setKey("a", false)
        setKey("s", false)
        setKey("d", false)
    }

    fun restartMovement() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.keyCode, isKeyDown(mc.gameSettings.keyBindForward))
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.keyCode, isKeyDown(mc.gameSettings.keyBindLeft))
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.keyCode, isKeyDown(mc.gameSettings.keyBindBack))
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.keyCode, isKeyDown(mc.gameSettings.keyBindRight))
    }

    fun jump() {
        scheduleTask(0) {setKey("jump", true)}
        scheduleTask(2) {setKey("jump", false)}
    }

    private var shouldEdge = false

    fun edge() {
        shouldEdge = true
    }

    @SubscribeEvent
    fun checkForEdging(event: RenderWorldLastEvent) {
        if (!shouldEdge) return
        val playerPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)
        val blockID = Block.getIdFromBlock(mc.theWorld.getBlockState(playerPos).block)
        if (blockID == 0) {
            jump()
            shouldEdge = false
        }
    }

    var targetBlocks = arrayListOf<Vec3>()

    fun clearBlocks() {
        targetBlocks.clear()
    }

    fun moveToBlock(x: Double, z: Double) {
        targetBlocks.add(0, Vec3(x, 0.0, z)) // y coord doesn't matter anyway
    }

    fun addBlock(x: Double, z: Double) {
        targetBlocks.add(Vec3(x, 0.0, z))
    }

    var airTicks = 0
    var lastX = 0.0
    var lastZ = 0.0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (targetBlocks.isEmpty()) return
        val targetBlock = targetBlocks[0]

        if (mc.thePlayer?.onGround == true) {
            airTicks = 0
        } else {
            airTicks += 1
        }

        val(yaw, pitch) = getYawAndPitch(targetBlock.xCoord, targetBlock.yCoord, targetBlock.zCoord)

        val speed = if(!mc.thePlayer.isSneaking) {
            mc.thePlayer.capabilities.walkSpeed
        } else {
            mc.thePlayer.capabilities.walkSpeed * 3 / 10 /// doesnt let me do 0.3???
        }
        val radians = yaw * Math.PI / 180 // todo: MathUtils?
        val x = -sin(radians) * speed * 2.806
        val z = cos(radians) * speed * 2.806

        if (airTicks < 2) {
            mc.thePlayer.motionX = x
            mc.thePlayer.motionZ = z
            lastX = x
            lastZ = z
        } else {
            //assume max acceleration
            mc.thePlayer.motionX = lastX * 0.91 + 0.0512 * speed * -sin(radians)
            mc.thePlayer.motionZ = lastZ * 0.91 + 0.0512 * speed * cos(radians)
            lastX = lastX * 0.91 + 0.2 * -sin(radians)
            lastZ = lastZ * 0.91 + 0.2 * cos(radians)
        }

        if (mc.thePlayer?.onGround == true) {
            if (
                abs(mc.thePlayer.posX - targetBlock.xCoord) <= 1.3 * mc.thePlayer.capabilities.walkSpeed &&
                abs(mc.thePlayer.posZ - targetBlock.zCoord) <= 1.3 * mc.thePlayer.capabilities.walkSpeed
            ) {
                mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
                mc.thePlayer.setPosition(targetBlock.xCoord, mc.thePlayer.posY, targetBlock.zCoord)
                targetBlocks.removeFirst()
                return
            }
        }
    }
}

val thisshit = Commodore("mottar") { //motiontarget
    literal("set").runs{
            x: Double, z: Double ->
        moveToBlock(x, z)
    }
    literal("add").runs {
            x: Double, z: Double ->
        addBlock(x, z)
    }
    literal("stop").runs{
        clearBlocks()
    }
    literal("test").runs{
        addBlock(145.5, 160.5)
        addBlock(145.5, 161.5)
        addBlock(144.5, 161.5)
        addBlock(143.5, 161.5)
        addBlock(143.5, 162.5)
        addBlock(143.5, 163.5)
        addBlock(144.5, 163.5)
        addBlock(145.5, 163.5)
        addBlock(145.5, 164.5)
        addBlock(145.5, 165.5)
    }
}
