package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.events.impl.PacketSentEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.MovementUtils.restartMovement
import catgirlroutes.utils.MovementUtils.stopMovement
import catgirlroutes.utils.PacketUtils.sendPacket
import catgirlroutes.utils.PlayerUtils.airClick
import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.rotation.RotationUtils.snapTo
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import net.minecraft.block.Block
import net.minecraft.block.BlockSkull
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*
import kotlin.math.*

object Doorless: Module(
    "Doorless",
    category = Category.DUNGEON,
    tag = TagType.WHIP
){
    private var doorClip = BooleanSetting("Clip")
    private var doorMotion = BooleanSetting("Motion")
    private val regenSkulls = BooleanSetting("Regenerate skulls")
    private val regenDelay = NumberSetting("Skulls regeneration delay", 10.0, 0.0, 20.0, 1.0, unit = "t").withDependency { this.regenSkulls.enabled }
    private val babyProof = BooleanSetting("Baby proof")
    private val babyProofRadius = NumberSetting("Baby proof radius", 5.0, 1.0, 15.0, unit = "m").withDependency { this.babyProof.enabled }

    init {
        this.addSettings(
            this.doorClip,
            this.doorMotion,
            this.regenSkulls,
            this.regenDelay,
            this.babyProof,
            this.babyProofRadius
        )
    }

    private var inDoor = false
    private var lastUse = System.currentTimeMillis()
    private val validBlocks = listOf(Blocks.coal_block, Blocks.stained_hardened_clay, Blocks.barrier)
    private val skullIds = listOf(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JjYmJmOTRkNjAzNzQzYTFlNzE0NzAyNmUxYzEyNDBiZDk4ZmU4N2NjNGVmMDRkY2FiNTFhMzFjMzA5MTRmZCJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ5ZDgwYjc5NDQyY2YxYTNhZmVhYTIzN2JkNmFkYWFhY2FiMGMyODgzMGZiMzZiNTcwNGNmNGQ5ZjU5MzdjNCJ9fX0="
    )

    private var initialCoords = Vec3(0.0, 0.0, 0.0)
    private var initialYaw = 0F
    private var initialPitch = 0F
    private var xOffset = 0
    private var zOffset = 0

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (event.packet !is C03PacketPlayer || !inDungeons || inDoor || !mc.thePlayer.isCollidedVertically ||
            mc.thePlayer?.heldItem?.item != Item.getItemById(368) || System.currentTimeMillis() - lastUse < 1000 ||
            !event.packet.isMoving) return

        val posX = event.packet.positionX
        val posY = event.packet.positionY
        val posZ = event.packet.positionZ
        if (posX > 0 || posZ > 0 || posX < -200 || posZ < -200) return
        val xDec = (posX + 200) % 1
        val zDec = (posZ + 200) % 1
        var yaw = -1F
        var pitch = -1F
        zOffset = 0
        xOffset = 0

        fun check(x1: Int, z1: Int, x2: Int, z2: Int) =
            validBlocks.contains(getBlockFloor(posX + x1, posY, posZ + z1)) ||
                    validBlocks.contains(getBlockFloor(posX + x2, posY, posZ + z2))

        when {
            zDec.tol(0.7) && xDec.inRange(0.3,0.7) && check(1,2,-1,2) -> { yaw=0F; pitch=77F; this.zOffset++ }
            xDec.tol(0.3) && zDec.inRange(0.3,0.7) && check(-2,1,-2,-1) -> { yaw=90F; pitch=77F; this.xOffset-- }
            zDec.tol(0.3) && xDec.inRange(0.3,0.7) && check(-1,-2,1,-2) -> { yaw=180F; pitch=77F; this.zOffset-- }
            xDec.tol(0.7) && zDec.inRange(0.3,0.7) && check(2,-1,2,1) -> { yaw=270F; pitch=77F; this.xOffset++ }
            zDec.tol(0.95) && xDec.inRange(0.3,0.7) && check(1,2,-1,2) -> { yaw=0F; pitch=84F; this.zOffset++ }
            xDec.tol(0.05) && zDec.inRange(0.3,0.7) && check(-2,1,-2,-1) -> { yaw=90F; pitch=84F; this.xOffset-- }
            zDec.tol(0.05) && xDec.inRange(0.3,0.7) && check(-1,-2,1,-2) -> { yaw=180F; pitch=84F; this.zOffset-- }
            xDec.tol(0.95) && zDec.inRange(0.3,0.7) && check(2,-1,2,1) -> { yaw=270F; pitch=84F; this.xOffset++ }
        }

        if (yaw < 0F || pitch < 0F) return
        (mc.theWorld?.getTileEntity(getBlockPosFloor(posX + xOffset, posY + 1, posZ + zOffset)) as? TileEntitySkull)
        ?.takeIf { it.playerProfile?.properties?.get("textures")?.firstOrNull()?.value in this.skullIds } ?: return
        inDoor = true
        initialYaw = mc.thePlayer.rotationYaw
        initialPitch = mc.thePlayer.rotationPitch
        initialCoords = Vec3(posX, posY, posZ)
        mc.thePlayer.swingItem()
        sendPacket(C06PacketPlayerPosLook(posX, posY, posZ, yaw, pitch, event.packet.isOnGround))
        airClick()
        waitingForS08 = true
        event.isCanceled = true
    }

    private var waitingForS08 = false

    @SubscribeEvent
    fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packet !is S08PacketPlayerPosLook || !waitingForS08) return

        val posX = event.packet.x
        val posY = event.packet.y
        val posZ = event.packet.z

        inDoor = false
        lastUse = System.currentTimeMillis()
        if ((posY - initialCoords.yCoord) != 1.0) {
            waitingForS08 = false
            return
        }

        val walkSpeed = mc.thePlayer.capabilities.walkSpeed
        val isWall = mc.theWorld.getBlockState(getBlockPosFloor(posX, posY - 1, posZ)).block == Blocks.cobblestone_wall
        val distance = if (isWall) walkSpeed * 3.2 else walkSpeed * 8.5

        if (isWall) {
            setBlocksInLine(posX, posY - 1, posZ, 0, 4)
            setWalls(posX, posY - 1, posZ)
        }

        stopMovement()
        scheduleTask(0) {
            snapTo(initialYaw, initialPitch)
            if (doorClip.value) clipForward(distance)
        }
        scheduleTask(1) {
            restartMovement()
            if (doorMotion.value) motion(walkSpeed)
        }

        setBlocksInLine(posX, posY, posZ, 0, 4)
        setBlocksInLine(posX, posY + 1, posZ, 0, 4)
        setWalls(posX, posY, posZ)
        setWalls(posX, posY + 1, posZ)

        if (this.regenSkulls.enabled) scheduleTask(regenDelay.value.toInt()) {
            setBlockAt(posX + xOffset * 4, posY, posZ + zOffset * 4)
        }
        waitingForS08 = false
    }

    private val placedGlassBlocks = mutableSetOf<BlockPos>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null || !inDungeons || !this.babyProof.enabled || event.phase != TickEvent.Phase.START) return

        val xz = getXZinRadius(this.babyProofRadius.value.toInt())
        if (xz.any { (x, z) -> isValidBlock(mc.theWorld.getBlockState(BlockPos(x, 69, z))) }) {
            val glass = findGlassPos(xz)
            glass?.let {
                this.placedGlassBlocks.addAll(it)
                it.forEach { pos ->
                    setBlockAt(pos.x, 69, pos.z, 20)
                    setBlockAt(pos.x, 71, pos.z, 20)
                }
            }
        } else {
            clearBlocks()
        }
    }

    private fun clearBlocks() {
        this.placedGlassBlocks.forEach {
            setBlockAt(it.x, 69, it.z, 0)
            setBlockAt(it.x, 71, it.z, 0)
        }
        this.placedGlassBlocks.clear()
    }

    private fun findGlassPos(xz: List<Pair<Int, Int>>): List<BlockPos>? {
        for ((x, z) in xz) {
            // x
            if ((0..2).all { isValidBlock(mc.theWorld.getBlockState(BlockPos(x + it, 69, z))) }) {
                for (dz in listOf(1, -1)) {
                    if ((0..2).all { mc.theWorld.isAirBlock(BlockPos(x + it, 69, z + dz)) }) {
                        return buildList {
                            addAll((0..2).map { BlockPos(x + it, 69, z + dz) })
                            add(BlockPos(x - 2, 69, z + dz * 2))
                            add(BlockPos(x + 4, 69, z + dz * 2))
                        }
                    }
                }
            }

            // z
            if ((0..2).all { isValidBlock(mc.theWorld.getBlockState(BlockPos(x, 69, z + it))) }) {
                for (dx in listOf(1, -1)) {
                    if ((0..2).all { mc.theWorld.isAirBlock(BlockPos(x + dx, 69, z + it)) }) {
                        return buildList {
                            addAll((0..2).map { BlockPos(x + dx, 69, z + it) })
                            add(BlockPos(x + dx * 2, 69, z - 2))
                            add(BlockPos(x + dx * 2, 69, z + 4))
                        }
                    }
                }
            }
        }

        return null
    }

    private fun isValidBlock(blockState: IBlockState?): Boolean {
        val block = blockState?.block
        return block == Blocks.coal_block ||
                block == Blocks.barrier ||
                (block == Blocks.stained_hardened_clay && blockState?.block?.damageDropped(blockState) == 14)
    }

    private fun getXZinRadius(radius: Int): List<Pair<Int, Int>> {
        val pX = floor(posX).toInt()
        val pZ = floor(posZ).toInt()
        return buildList {
            for (r in 0..radius) {
                for (x in pX - r..pX + r) add(x to pZ - r)
                for (z in pZ - r + 1..pZ + r) add(pX + r to z)
                for (x in pX + r - 1 downTo pX - r) add(x to pZ + r)
                for (z in pZ + r - 1 downTo pZ - r) add(pX - r to z)
            }
        }
    }

    private fun Double.tol(v: Double) = isWithinTolerence(this, v)
    private fun Double.inRange(a: Double, b: Double) = this > a && this < b

    private fun setBlocksInLine(baseX: Double, baseY: Double, baseZ: Double, blockId: Int, count: Int) {
        for (i in 1..count) {
            setBlockAt(baseX + xOffset * i, baseY, baseZ + zOffset * i, blockId)
        }
    }

    private fun setWalls(baseX: Double, baseY: Double, baseZ: Double) {
        listOf(1, -1).forEach {
            setBlockAt(
                baseX + xOffset * 2 + zOffset * it,
                baseY,
                baseZ + zOffset * 2 + xOffset * it,
                20
            )
        }
    }

    private fun motion(walkSpeed: Float) {
        val yawRad = (Math.round(mc.thePlayer.rotationYaw / 90) * 90).toDouble() * (Math.PI / 180)
        val velocity = walkSpeed * 2.7 + (0.22 * (walkSpeed / 1.32))
        mc.thePlayer.motionX = velocity * -sin(yawRad)
        mc.thePlayer.motionZ = velocity * cos(yawRad)
    }

    private fun clipForward(distance: Double) {
        val radians = mc.thePlayer.rotationYaw * Math.PI / 180
        var newX = mc.thePlayer.posX
        var newZ = mc.thePlayer.posZ

        if (abs(-sin(radians)) > abs(cos(radians))) newX += distance * sign(-sin(radians))
        else newZ += distance * sign(cos(radians))
        mc.thePlayer.setPosition(newX, mc.thePlayer.posY, newZ)
        debugMessage(distance)
    }

    private fun isWithinTolerence(n1: Double, n2: Double): Boolean {
        return abs(n1 - n2) < 1e-4;
    }

    private fun getBlockPosFloor(x: Double, y: Double, z: Double): BlockPos {
        return BlockPos(floor(x), floor(y), floor(z));
    }

    private fun getBlockFloor(x: Double, y: Double, z: Double): Block {
        return mc.theWorld.getBlockState(BlockPos(floor(x), floor(y), floor(z))).block;
    }

    private fun setBlockAt(x: Double, y: Double, z: Double, id: Int) {
        val blockPos = getBlockPosFloor(x, y, z)
        setBlockAt(blockPos.x, blockPos.y, blockPos.z, id)
    }

    private fun setBlockAt(x: Int, y: Int, z: Int, id: Int) {
        val blockPos = BlockPos(x, y, z)
        mc.theWorld.setBlockState(blockPos, Block.getStateById(id))
        mc.theWorld.markBlockForUpdate(blockPos)
    }

    private fun setBlockAt(x: Double, y: Double, z: Double) {
        val blockPos = getBlockPosFloor(x, y, z)
        val skullBlockState: IBlockState = Blocks.skull.defaultState
            .withProperty(BlockSkull.FACING, net.minecraft.util.EnumFacing.DOWN) // doesn't really change anything visually. cba to fix
        mc.theWorld.setBlockState(blockPos, skullBlockState)

        val tileEntity = mc.theWorld.getTileEntity(blockPos) as? TileEntitySkull
        if (tileEntity != null) {
            val gameProfile = GameProfile(UUID.randomUUID(), null)
            gameProfile.properties.put("textures", Property("textures", skullIds[0]))
            tileEntity.playerProfile = gameProfile
            tileEntity.markDirty()
            mc.theWorld.markBlockForUpdate(blockPos)
        }
    }
}