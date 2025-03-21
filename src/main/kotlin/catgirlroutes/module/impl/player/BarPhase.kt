package catgirlroutes.module.impl.player

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.PlayerUtils
import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.Utils.equalsOneOf
import catgirlroutes.utils.VecUtils.multiply
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


// edited flopper
object BarPhase: Module(
    "Bar phase",
    Category.PLAYER
) {
    private val phaseDelay = NumberSetting("Phase delay", 0.0, 0.0, 5.0, 1.0, unit = "t")
//    private val blockClip = BooleanSetting("Block clip in TP Maze", false) // todo

    init {
        addSettings(
            phaseDelay,
//            blockClip
        )
    }

    private var phaseTicks = 0

    const val minCoord = 0.446f
    const val maxCoord = 0.5455f
    const val range = 0.018

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent){
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return
        if (!mc.thePlayer.isCollidedHorizontally || !mc.thePlayer.onGround) {
            this.phaseTicks = 0
            return
        }
        val pos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        if (mc.theWorld.getBlockState(pos).block.equals(Blocks.iron_bars) || mc.theWorld.getBlockState(pos.up()).block.equals(Blocks.iron_bars)) {
            val dir = direction() ?: run {
                this.phaseTicks = 0
                return
            }

            val loc = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
            val offsVec = Vec3(0.0, 1.0,0.0).crossProduct(Vec3(dir.directionVec)).multiply(0.3)

            val flag = flag(loc, offsVec, dir)
            val flag2 = flag(loc.addVector(0.0, 0.5, 0.0), offsVec, dir)

            if ((flag || flag2) && this.phaseTicks >= this.phaseDelay.value) {
                PlayerUtils.relativeClip(
                    -sin((dir.horizontalIndex * 90f) * Math.PI / 180) * 0.7,
                    if (!flag) 0.5 else 0.0,
                    cos((dir.horizontalIndex * 90f) * Math.PI / 180) * 0.7,
                    false
                )
                this.phaseTicks = 0
            } else this.phaseTicks++
        } else this.phaseTicks = 0
    }

    private fun direction(): EnumFacing?{
        return  when {
            inRange(posX, this.minCoord - 0.3f) -> EnumFacing.EAST
            inRange(posZ, this.minCoord - 0.3f) -> EnumFacing.SOUTH
            inRange(posX, this.maxCoord + 0.3f) -> EnumFacing.WEST
            inRange(posZ, this.maxCoord + 0.3f) -> EnumFacing.NORTH
            else -> null
        }
    }

    private fun inRange(a: Double, coord: Float): Boolean {
        val b = if ((a > 0)) a % 1 else 1 + a % 1
        return abs(b - coord) <= this.range
    }

    private fun flag(loc: Vec3, offsVec: Vec3, dir: EnumFacing): Boolean {
        return mc.theWorld.getBlockState(BlockPos(loc.add(offsVec)).offset(dir)).isGoog
                && mc.theWorld.getBlockState(BlockPos(loc.subtract(offsVec)).offset(dir)).isGoog
                && mc.theWorld.getBlockState(BlockPos(loc.add(offsVec)).offset(dir).up()).isGoog
                && mc.theWorld.getBlockState(BlockPos(loc.subtract(offsVec)).offset(dir).up()).isGoog
    }

    private val IBlockState.isGoog: Boolean get() =
        !this.block.material.isSolid || this.block.equalsOneOf(Blocks.stone_pressure_plate, Blocks.wooden_pressure_plate,
            Blocks.heavy_weighted_pressure_plate, Blocks.light_weighted_pressure_plate)


//    private var lastX = Double.MAX_VALUE
//    private var lastZ = Double.MAX_VALUE
//    private var lagbackRegister = false
//    private var lagBacked = false

//    @SubscribeEvent
//    fun onMotion(event: MotionUpdateEvent.Post) {
//        if (!mc.thePlayer.onGround) return
//        val blockPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 0.5, mc.thePlayer.posZ)
//        val blockState = mc.theWorld.getBlockState(blockPos)
//        if (blockState.block != Blocks.iron_bars || panesConnected(blockPos, blockState)) return
//
//        blockState.block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)
//        val aabb = blockState.block.getCollisionBoundingBox(mc.theWorld, blockPos, blockState)
//
//        adjustPos(aabb.minX, aabb.maxX, lastX, mc.thePlayer.posX)
//        adjustPos(aabb.minZ, aabb.maxZ, lastZ, mc.thePlayer.posZ)
//    }
//
//    @SubscribeEvent
//    fun onS08(event: PacketReceiveEvent) {
//        if (event.packet !is S08PacketPlayerPosLook || !lagbackRegister) return
//        lagbackRegister = false
//        lagBacked = true
//        scheduleTask(10) { lagBacked = false}
//    }
//
//    private fun adjustPos(min: Double, max: Double, lastValue: Double, pos: Double) {
//        if (abs(min - max) > 0.3) return
//
//        val playerWidth = mc.thePlayer.width / 2.0
//        val closerToMin = abs(pos - min) < abs(pos - max)
//        val boundaryAdjustment = if (closerToMin) max + playerWidth + 0.1 else min - playerWidth + 0.1
//        val isStupid = closerToMin && pos - 0.3 < min || !closerToMin && pos + 0.3 > max
//
//        if (isStupid && !lagBacked && mc.thePlayer.isCollidedHorizontally) {
//            mc.thePlayer.setPosition(
//                if (pos == mc.thePlayer.posX) boundaryAdjustment else mc.thePlayer.posX,
//                mc.thePlayer.posY,
//                if (pos == mc.thePlayer.posZ) boundaryAdjustment else mc.thePlayer.posZ
//            )
//            lagbackRegister = true
//            scheduleTask(2) { lagbackRegister = false }
//            if (pos == mc.thePlayer.posX) lastX = boundaryAdjustment else lastZ = boundaryAdjustment
//        } else if (lastValue != boundaryAdjustment && lastValue != Double.MAX_VALUE) {
//            if (pos == mc.thePlayer.posX) lastX = Double.MAX_VALUE else lastZ = Double.MAX_VALUE
//        }
//    }
//
//    private fun panesConnected(pos: BlockPos?, state: IBlockState): Boolean {
//        val block = state.block as BlockPane
//        val ns = block.canPaneConnectTo(mc.theWorld, pos, EnumFacing.NORTH) ||
//                block.canPaneConnectTo(mc.theWorld, pos, EnumFacing.SOUTH)
//        val we = block.canPaneConnectTo(mc.theWorld, pos, EnumFacing.WEST) ||
//                block.canPaneConnectTo(mc.theWorld, pos, EnumFacing.EAST)
//        return ns && we || !ns && !we
//    }
}