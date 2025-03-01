package catgirlroutes.module.impl.dungeons.puzzlesolvers

import catgirlroutes.utils.BlockAura
import catgirlroutes.utils.BlockAura.blockArray
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.VecUtils.equal
import catgirlroutes.utils.VecUtils.toBlockPos
import catgirlroutes.utils.VecUtils.toVec3
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoom
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoomName
import catgirlroutes.utils.dungeon.DungeonUtils.getRealCoords
import catgirlroutes.utils.getBlockAt
import catgirlroutes.utils.render.WorldRenderUtils.drawLine
import catgirlroutes.utils.render.WorldRenderUtils.drawStringInWorld
import catgirlroutes.utils.render.WorldRenderUtils.drawTracer
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.awt.Color
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object WaterSolver {
    private var waterSolutions: JsonObject

    init {
        val isr = WaterSolver::class.java.getResourceAsStream("/watertimes.json")?.let { InputStreamReader(it, StandardCharsets.UTF_8) }
        waterSolutions = JsonParser().parse(isr).asJsonObject
    }

    private var solutions = HashMap<LeverBlock, Array<Double>>()
    private var patternIdentifier = -1
    private var openedWater = -1L

    fun scan() = with (currentRoom) {
        if (this?.data?.name != "Water Board" || patternIdentifier != -1) return@with
        val extendedSlots = WoolColor.entries.joinToString("") { if (it.isExtended) it.ordinal.toString() else "" }.takeIf { it.length == 3 } ?: return

        patternIdentifier = when {
            getBlockAt(getRealCoords(14, 77, 27)) == Blocks.hardened_clay -> 0 // right block == clay
            getBlockAt(getRealCoords(16, 78, 27)) == Blocks.emerald_block -> 1 // left block == emerald
            getBlockAt(getRealCoords(14, 78, 27)) == Blocks.diamond_block -> 2 // right block == diamond
            getBlockAt(getRealCoords(14, 78, 27)) == Blocks.quartz_block  -> 3 // right block == quartz
            else -> return@with modMessage("§cFailed to get Water Board pattern.")
        }

        modMessage("$patternIdentifier || ${WoolColor.entries.filter { it.isExtended }.joinToString(", ") { it.name.lowercase() }}")

        solutions.clear()
        waterSolutions[patternIdentifier.toString()].asJsonObject[extendedSlots].asJsonObject.entrySet().forEach {
            solutions[
                when (it.key) {
                    "diamond_block" -> LeverBlock.DIAMOND
                    "emerald_block" -> LeverBlock.EMERALD
                    "hardened_clay" -> LeverBlock.CLAY
                    "quartz_block"  -> LeverBlock.QUARTZ
                    "gold_block"    -> LeverBlock.GOLD
                    "coal_block"    -> LeverBlock.COAL
                    "water"         -> LeverBlock.WATER
                    else -> LeverBlock.NONE
                }
            ] = it.value.asJsonArray.map { it.asDouble }.toTypedArray()
        }
    }

    fun onRenderWorld(showTracer: Boolean) {
        if (patternIdentifier == -1 || solutions.isEmpty() || currentRoomName != "Water Board") return

        val solutionList = solutions
            .flatMap { (lever, times) -> times.drop(lever.i).map { Pair(lever, it) } }
            .sortedBy { (lever, time) -> time + if (lever == LeverBlock.WATER) 0.01 else 0.0 }


        if (showTracer) {
            val firstSolution = solutionList.firstOrNull()?.first ?: return
            drawTracer(firstSolution.leverPos.addVector(.5, .5, .5), Color.GREEN)

            if (solutionList.size > 1 && firstSolution.leverPos != solutionList[1].first.leverPos) {
                drawLine(firstSolution.leverPos.addVector(.5, .5, .5), solutionList[1].first.leverPos.addVector(.5, .5, .5), Color.RED, 1.5f)
            }
        }

        solutions.forEach { (lever, times) ->
            times.drop(lever.i).forEachIndexed { index, time ->
                drawStringInWorld(when {
                    openedWater == -1L && time == 0.0 -> "§a§lCLICK ME!"
                    openedWater == -1L -> "§e${time}s"
                    else ->
                        (openedWater + time * 1000L - System.currentTimeMillis()).takeIf { it > 0 }?.let { "§e${String.format(Locale.US, "%.2f", it / 1000)}s" } ?: "§a§lCLICK ME!"
                }, lever.leverPos.addVector(0.5, (index + lever.i) * 0.5 + 1.5, 0.5), scale = 0.04f)
            }
        }
    }

    fun onTick() {
        if (patternIdentifier == -1 || solutions.isEmpty() || currentRoomName != "Water Board") return

        val currentTime = System.currentTimeMillis()

        val solutionList = solutions
            .flatMap { (lever, times) ->
                times.drop(lever.i).map { time ->
                    val timeRemaining = when {
                        openedWater == -1L && time == 0.0 -> 0.0
                        openedWater == -1L -> time
                        else -> (openedWater + time * 1000L - currentTime).takeIf { it > 0 }?.div(1000.0) ?: 0.0
                    }
                    Pair(lever, timeRemaining)
                }
            }
            .sortedBy { (lever, time) -> time + if (lever == LeverBlock.WATER) 0.01 else 0.0 }

        val solution = solutionList
        solution.forEach {
            if (blockArray.contains(BlockAura.BlockAuraAction(it.first.leverPos.toBlockPos(), 6.0)) || it.second != 0.0) return
            blockArray.add(BlockAura.BlockAuraAction(it.first.leverPos.toBlockPos(), 6.0))
        }
    }

    fun waterInteract(event: C08PacketPlayerBlockPlacement) {
        if (solutions.isEmpty()) return
        LeverBlock.entries.find { it.leverPos.equal(event.position.toVec3()) }?.let {
            if (it == LeverBlock.WATER && openedWater == -1L) openedWater = System.currentTimeMillis()
            it.i++
        }
    }

    fun reset() {
        LeverBlock.entries.forEach { it.i = 0 }
        patternIdentifier = -1
        solutions.clear()
        openedWater = -1
    }

    private enum class WoolColor(val relativePosition: BlockPos) {
        PURPLE(BlockPos(15, 56, 19)),
        ORANGE(BlockPos(15, 56, 18)),
        BLUE(BlockPos(15, 56, 17)),
        GREEN(BlockPos(15, 56, 16)),
        RED(BlockPos(15, 56, 15));

        val isExtended: Boolean get() =
            currentRoom?.let { getBlockAt(it.getRealCoords(relativePosition)) == Blocks.wool } == true
    }

    private enum class LeverBlock(val relativePosition: Vec3, var i: Int = 0) {
        QUARTZ(Vec3(20.0, 61.0, 20.0)),
        GOLD(Vec3(20.0, 61.0, 15.0)),
        COAL(Vec3(20.0, 61.0, 10.0)),
        DIAMOND(Vec3(10.0, 61.0, 20.0)),
        EMERALD(Vec3(10.0, 61.0, 15.0)),
        CLAY(Vec3(10.0, 61.0, 10.0)),
        WATER(Vec3(15.0, 60.0, 5.0)),
        NONE(Vec3(0.0, 0.0, 0.0));

        val leverPos: Vec3
            get() = currentRoom?.getRealCoords(relativePosition) ?: Vec3(0.0, 0.0, 0.0)
    }
}