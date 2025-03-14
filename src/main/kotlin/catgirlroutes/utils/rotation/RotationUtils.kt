package catgirlroutes.utils.rotation

import catgirlroutes.CatgirlRoutes.Companion.mc
import com.github.stivais.commodore.Commodore
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.rotation.RotationUtils.rotateSmoothly
import net.minecraft.util.Vec3
import net.minecraft.util.Vec4b
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import kotlin.math.atan2
import kotlin.math.sqrt

object RotationUtils {

    fun snapTo(yaw: Float, pitch: Float) {
        mc.thePlayer.rotationYaw = yaw
        mc.thePlayer.rotationPitch = pitch
    }

    fun getYawAndPitch(x: Double, y:Double, z:Double): Pair<Float, Float> {
        val dx = x - mc.thePlayer.posX
        val dy = y - (mc.thePlayer.posY + mc.thePlayer.eyeHeight)
        val dz = z - mc.thePlayer.posZ

        val horizontalDistance = sqrt(dx * dx + dz * dz )

        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return Pair(normalizedYaw.toFloat(), pitch.toFloat())
    }

    var aiming = false
    var yaw = 0f
    var pitch = 0f
    var time = 0
    var initYaw = 0f
    var initPitch = 0f
    var initTime = System.currentTimeMillis()

    var targets = arrayListOf<Vec3>()

    @SubscribeEvent
    fun onTick(event: RenderWorldLastEvent) {
        if (aiming) return
        if (targets.isEmpty()) return
        rotateSmoothly(targets[0].xCoord.toFloat(), targets[0].yCoord.toFloat(), targets[0].zCoord.toInt())
        targets.removeFirst()
    }

    fun rotateSmoothly(yawTarget: Float, pitchTarget: Float, timeTarget: Int) {
        aiming = true
        yaw = yawTarget
        pitch = pitchTarget
        time = timeTarget
        while (yawTarget >= 180) yaw -= 360;
        while (pitchTarget >= 180) pitch -= 360;
        initYaw = mc.thePlayer.rotationYaw
        initPitch = mc.thePlayer.rotationPitch
        initTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!aiming) return
        val progress = if (time <= 0) {
            1.0
        } else {
            ((System.currentTimeMillis() - initTime).toDouble() / time).coerceIn(0.0, 1.0)
        }
        val amount = bezier(progress)
        mc.thePlayer.rotationYaw = initYaw + (yaw - initYaw) * amount.toFloat()
        mc.thePlayer.rotationPitch = initPitch + (pitch - initPitch) * amount.toFloat()
        if (progress >= 1) aiming = false
    }

    fun bezier(t: Double): Double {
        return (1 - t) * (1 - t) * (1 - t) * 0 + 3 * (1 - t) * (1 - t) * t * 1 + 3 * (1 - t) * t * t * 1 + t * t * t * 1;
    }
}

val rotationDebug = Commodore("rot") {
    literal("set").runs{
            yaw: Float, pitch: Float, time : Int ->
        rotateSmoothly(yaw, pitch, time)
    }
}