package catgirlroutes.module.impl.render

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.RenderEntityModelEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.settings.impl.StringSelectorSetting
import catgirlroutes.utils.dungeon.DungeonUtils
import catgirlroutes.utils.dungeon.M7Phases
import catgirlroutes.utils.render.OutlineUtils
import catgirlroutes.utils.render.WorldRenderUtils.draw2DBoxByEntity
import catgirlroutes.utils.render.WorldRenderUtils.drawBoxByEntity
import net.minecraft.entity.boss.EntityWither
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object BossESP : Module(
    name = "Boss ESP",
    category = Category.RENDER,
    description = "ESP for the Withers."
) {
    private val style = StringSelectorSetting("Style","Box", arrayListOf("Box", "Outline", "2D"), "Esp render style to be used.")
    private val lineWidth = NumberSetting("Line width", 4.0, 1.0, 8.0, 1.0)
    private val color = ColorSetting("Boss ESP color", Color(0,0,255), collapsible = false, description = "Color for the Boss ESP")

    init{
        this.addSettings(
            style,
            lineWidth,
            color
        )
    }

    private var wither: EntityWither? = null;

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (DungeonUtils.getF7Phase() == M7Phases.Unknown) return
        mc.theWorld.loadedEntityList
            .filterIsInstance<EntityWither>()
            .filter { !it.isInvisible && it.renderSizeModifier == 1f }
            .forEach {
                wither = it
                when (style.selected) {
                    "Box" -> drawBoxByEntity(it, color.value, it.width.toDouble(), it.height.toDouble(), event.partialTicks, lineWidth.value, true)
                    "2D" -> draw2DBoxByEntity(it, color.value, event.partialTicks, lineWidth.value.toFloat(), true)
                }
            }
    }

    @SubscribeEvent
    fun onRenderEntityModel (event: RenderEntityModelEvent) {
        if (style.selected == "Outline" && event.entity == wither) {
            OutlineUtils.outlineESP(event, lineWidth.value.toFloat(), color.value, true)
        }
    }
}