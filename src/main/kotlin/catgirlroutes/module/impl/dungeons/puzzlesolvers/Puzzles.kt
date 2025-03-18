package catgirlroutes.module.impl.dungeons.puzzlesolvers

import catgirlroutes.events.impl.MotionUpdateEvent
import catgirlroutes.events.impl.PacketSentEvent
import catgirlroutes.events.impl.RoomEnterEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.impl.dungeons.puzzlesolvers.WaterSolver.waterInteract
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.DropdownSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.clock.Executor
import catgirlroutes.utils.clock.Executor.Companion.register
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color

object Puzzles : Module (
    "Puzzles",
    Category.DUNGEON,
    "Puzzle solvers"
) {
    private val fillDropdown: DropdownSetting = DropdownSetting("Ice fill", false)
    var fillSolver: BooleanSetting = BooleanSetting("Ice fill solver", false).withDependency(fillDropdown)
    var fillAuto: BooleanSetting = BooleanSetting("Auto ice fill", false).withDependency(fillDropdown) { fillSolver.enabled }
    var fillDelay: NumberSetting = NumberSetting("Auto ice fill delay", 2.0, 1.0, 10.0, 1.0).withDependency(fillDropdown) { fillSolver.enabled && fillAuto.enabled }

    private val tttDropdown: DropdownSetting = DropdownSetting("Tic tac toe", false)
    private var tttSolver: BooleanSetting = BooleanSetting("Tic tac toe solver", false).withDependency(tttDropdown)
    private val renderNext = BooleanSetting("Show Next Move", true, description = "Shows which move is next.").withDependency(tttDropdown) { tttSolver.enabled }
    var tttAuto: BooleanSetting = BooleanSetting("Auto TTT", false).withDependency(tttDropdown) { tttSolver.enabled }
    var tttReach: NumberSetting = NumberSetting("Auto TTT reach", 4.5, 1.0, 6.0, 0.1).withDependency(tttDropdown) { tttSolver.enabled && tttAuto.enabled }

    private val wbDropdown: DropdownSetting = DropdownSetting("Water board", false)
    private val wbSolver: BooleanSetting = BooleanSetting("Water Board Solver", false, description = "Shows you the solution to the water puzzle.").withDependency(wbDropdown)
    private val wbTracer: BooleanSetting = BooleanSetting("Show Tracer", true, description = "Shows a tracer to the next lever.").withDependency(wbDropdown) { wbSolver.enabled }

    private val weirdosDropdown: DropdownSetting = DropdownSetting("Three weirdos", false)
    val weirdosSolver: BooleanSetting = BooleanSetting("Weirdo solver", false).withDependency(weirdosDropdown)
    val weirdosAuto = BooleanSetting("Auto weirdos", false).withDependency(weirdosDropdown) { weirdosSolver.enabled }

    init {
        addSettings(
            fillDropdown,
            fillSolver,
            fillAuto,
            fillDelay,

            tttDropdown,
            tttSolver,
            renderNext,
            tttAuto,
            tttReach,

            wbDropdown,
            wbSolver,
            wbTracer,

            weirdosDropdown,
            weirdosSolver,
            weirdosAuto
        )

        Executor(500) {
            if (!inDungeons || inBoss) return@Executor
            if (wbSolver.value) WaterSolver.scan()
        }.register()
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        IceFillSolver.reset()
        TicTacToeSolver.onWorldLoad()
        WaterSolver.reset()
        AutoWeirdos.onWorldChange()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onMotionUpdate(event: MotionUpdateEvent.Post) {
        TicTacToeSolver.onMotion()
    }

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (event.packet is C08PacketPlayerBlockPlacement) {
            if (wbSolver.value) waterInteract(event.packet)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        AutoWeirdos.onChat(event)
    }

    @SubscribeEvent
    fun onWorldLast(event: RenderWorldLastEvent) {
        if (fillSolver.enabled) IceFillSolver.onRenderWorld(Color.GREEN)
        if (tttSolver.enabled) TicTacToeSolver.onRenderWorld()
        if (wbSolver.enabled)   WaterSolver.onRenderWorld(wbTracer.value)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        TicTacToeSolver.onTick()
        WaterSolver.onTick()
        AutoWeirdos.onTick()
    }

    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) {
        IceFillSolver.onRoomEnter(event)
    }
}