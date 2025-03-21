package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.wrapText
import catgirlroutes.ui.notification.Notification
import catgirlroutes.ui.notification.NotificationType
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.pow

/**
 * Usage: Notifications.send()
 */
object Notifications {
    private val notifications = mutableListOf<Notification>()

    fun send(title: String, description: String = "", duration: Double = 2000.0, type: NotificationType = NotificationType.INFO, icon: String? = null) {
        notifications.add(Notification(title, description, duration, type, icon))
    }

    @SubscribeEvent
    fun onRender(event: TickEvent.RenderTickEvent) {
        if (event.phase != TickEvent.Phase.END || notifications.isEmpty()) return

        notifications.removeIf { it.endTime <= System.currentTimeMillis() }

        val sr = ScaledResolution(mc)
        var y: Double = sr.scaledHeight - 3.0

        for (notification in notifications) {
            val width: Double = maxOf(150.0, FontUtil.getStringWidthDouble(notification.title) + 10.0)
            val lineSpacing = fontHeight + 2
            val iconOffset = if (notification.icon != null) 15 else 0
            val lines = wrapText(notification.description, width - lineSpacing - iconOffset * 1.8) // 1.8 because don't even ask it's 2 am
            val height: Double = 20.0 + lines.size * lineSpacing

            var x: Double = sr.scaledWidth - width - 2
            val elapsedTime: Long = System.currentTimeMillis() - notification.startTime
            val remainingTime: Long = notification.endTime - System.currentTimeMillis()
            x += calculateX(elapsedTime, width) + calculateX(remainingTime, width)

            val adjustedX = x - iconOffset
            val adjustedWidth = width + iconOffset
            var adjustedHeight = height + iconOffset
            if (notification.icon != null && (notification.title.isEmpty() || notification.description.isEmpty())) adjustedHeight += lineSpacing

            notification.draw(adjustedX, y - adjustedHeight, adjustedWidth, adjustedHeight)

            adjustedHeight *= calculateH(elapsedTime) * calculateH(remainingTime)

            y -= adjustedHeight + 3
        }
    }

    private fun calculateX(time: Long, width: Double): Double {
        return when {
            time in 100L..250L -> {
                val progress = (250L - time) / 150.0
                Easing.easeInQuad(progress) * (width + 2) //change this for diff animation
            }
            time < 100L -> Double.MAX_VALUE // animation out
            else -> 0.0
        }
    }

    private fun calculateH(time: Long): Double {
        return if (time < 100L) {
            val progress = time / 100.0
            Easing.easeInQuad(progress) //change this for diff animation
        } else 1.0
    }

    //add functions for easing here
    object Easing { // todo: probably make a util
        fun easeOutQuad(t: Double): Double = t * (2 - t)
        fun easeInQuad(t: Double): Double = t * t
        fun easeInOutQuad(t: Double): Double = if (t < 0.5) 2 * t * t else -1 + (4 - 2 * t) * t

        fun easeOutBack(t: Double): Double {
            val c1 = 1.70158
            val c3 = c1 + 1

            return 1 + c3 * (t - 1).pow(3) + c1 * (t - 1).pow(2)
        }
    }

}