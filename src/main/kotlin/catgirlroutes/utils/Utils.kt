package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.dungeon.tiles.Rotations
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraft.util.*
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.round
import kotlin.math.sqrt


object Utils {
    private val FORMATTING_CODE_PATTERN = Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE)

    val String?.noControlCodes: String
        get() = this?.let { FORMATTING_CODE_PATTERN.replace(it, "") } ?: ""

    fun Any?.equalsOneOf(vararg options: Any?): Boolean {
        return options.any { this == it }
    }

    fun String.containsOneOf(options: Collection<String>, ignoreCase: Boolean = false): Boolean {
        return options.any { this.contains(it, ignoreCase) }
    }

    /**
     * Checks if the first value in the pair equals the first argument and the second value in the pair equals the second argument.
     */
    fun Pair<Any?, Any?>?.equal(first: Any?, second: Any?): Boolean {
        return this?.first == first && this?.second == second
    }

    val ItemStack?.lore: List<String>
        get() = this?.tagCompound?.getCompoundTag("display")?.getTagList("Lore", 8)?.let {
            List(it.tagCount()) { i -> it.getStringTagAt(i) }
        }.orEmpty()


    fun Event.postAndCatch(): Boolean {
        return runCatching {
            MinecraftForge.EVENT_BUS.post(this)
        }.onFailure {
            it.printStackTrace()
            //logger.error("An error occurred", it)
            val style = ChatStyle()
            style.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/od copy ```${it.stackTraceToString().lineSequence().take(10).joinToString("\n")}```") // odon clint
            style.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText("§6Click to copy the error to your clipboard."))
            modMessage(" Caught an ${it::class.simpleName ?: "error"} at ${this::class.simpleName}. §cPlease click this message to copy and send it in the Odin discord!")}.getOrDefault(isCanceled)
    }

    fun removeFormatting(text: String): String {
        return text.replace("[\u00a7&][0-9a-fk-or]".toRegex(), "")
    }

    fun distanceToPlayer(x: Double, y: Double, z: Double): Double {
        return sqrt((mc.renderManager.viewerPosX - x) * (mc.renderManager.viewerPosX - x) +
                (mc.renderManager.viewerPosY - y) * (mc.renderManager.viewerPosY - y) +
                (mc.renderManager.viewerPosZ - z) * (mc.renderManager.viewerPosZ - z)
        )
    }

    fun distanceToPlayer(x: Int, y: Int, z: Int): Double {
        return sqrt((mc.renderManager.viewerPosX - x) * (mc.renderManager.viewerPosX - x) +
                (mc.renderManager.viewerPosY - y) * (mc.renderManager.viewerPosY - y) +
                (mc.renderManager.viewerPosZ - z) * (mc.renderManager.viewerPosZ - z)
        )
    }

    fun EntityPlayer?.isOtherPlayer(): Boolean {
        return this != null && this != mc.thePlayer && this.uniqueID.version() != 2
    }

    val ItemStack?.unformattedName: String
        get() = this?.displayName?.noControlCodes ?: ""

    val ItemStack?.extraAttributes: NBTTagCompound?
        get() = this?.getSubCompound("ExtraAttributes", false)

    val ItemStack?.skyblockID: String
        get() = this?.extraAttributes?.getString("id") ?: ""

    fun runOnMCThread(run: () -> Unit) {
        if (!mc.isCallingFromMinecraftThread) mc.addScheduledTask(run) else run()
    }

    private val romanMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
    fun romanToInt(s: String): Int {
        var result = 0
        for (i in 0 until s.length - 1) {
            val current = romanMap[s[i]] ?: 0
            val next = romanMap[s[i + 1]] ?: 0
            result += if (current < next) -current else current
        }
        return result + (romanMap[s.last()] ?: 0)
    }

    private val intToRomanMap = mapOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    fun intToRoman(num: Int): String = intToRomanMap.entries
        .sortedByDescending { it.key }
        .fold("" to num) { (result, n), (value, symbol) ->
            result + symbol.repeat(n / value) to n % value
        }.first

    /**
     * Adds the given coordinates to the Vec3.
     */
    fun Vec3.addVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3 {
        return this.addVector(x.toDouble(), y.toDouble(), z.toDouble())
    }

    /**
     * Removes the given coordinates to the Vec3.
     */
    fun Vec3.subtractVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3 {
        return this.addVector(-x.toDouble(), -y.toDouble(), -z.toDouble())
    }

    /**
     * Adds the given coordinates to the Vec3.
     */
    fun Vec3i.addVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3i {
        return Vec3i(this.x + x.toInt(), this.y + y.toInt(), this.z + z.toInt())
    }

    fun Vec3.rotateToNorth(rotation: Rotations): Vec3 {
        return when (rotation) {
            Rotations.NORTH -> Vec3(-this.xCoord, this.yCoord, -this.zCoord)
            Rotations.WEST -> Vec3(this.zCoord, this.yCoord, -this.xCoord)
            Rotations.SOUTH -> Vec3(this.xCoord, this.yCoord, this.zCoord)
            Rotations.EAST -> Vec3(-this.zCoord, this.yCoord, this.xCoord)
            else -> this
        }
    }

    fun rotationNumber(rotation: Rotations): Int {
        return when (rotation) {
            Rotations.WEST -> -1
            Rotations.NORTH -> 0
            Rotations.EAST -> 1
            Rotations.SOUTH -> 2

            //WEST = -1 | -90
            //NORTH = 0 | 0
            //EAST = 1 | 90
            //SOUTH = 2 | 180
            else -> throw IllegalStateException("Unable to get integer facing of $this")
        }
    }

    fun Vec3.rotateAroundNorth(rotation: Rotations): Vec3 {
        return when (rotation) {
            Rotations.NORTH -> Vec3(-this.xCoord, this.yCoord, -this.zCoord)
            Rotations.WEST -> Vec3(-this.zCoord, this.yCoord, this.xCoord)
            Rotations.SOUTH -> Vec3(this.xCoord, this.yCoord, this.zCoord)
            Rotations.EAST -> Vec3(this.zCoord, this.yCoord, -this.xCoord)
            else -> this
        }
    }

    fun renderText(
        // render utils probably?
        text: String,
        x: Int,
        y: Int,
        scale: Double = 1.0,
        color: Int = 0xFFFFFF,
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()

        GlStateManager.scale(scale, scale, scale)

        var yOffset = y - mc.fontRendererObj.FONT_HEIGHT
        text.split("\n").forEach {
            yOffset += (mc.fontRendererObj.FONT_HEIGHT * scale).toInt()
            mc.fontRendererObj.drawString(
                it,
                round(x / scale).toFloat(),
                round(yOffset / scale).toFloat(),
                color,
                true
            )
        }

        GlStateManager.popMatrix()
        GlStateManager.enableDepth()
        GlStateManager.enableBlend()
    }
    /**
     * Profiles the specified function with the specified string as profile section name.
     * Uses the minecraft profiler.
     *
     * @param name The name of the profile section.
     * @param func The code to profile.
     */
    inline fun profile(name: String, func: () -> Unit) {
        startProfile(name)
        func()
        endProfile()
    }

    /**
     * Starts a minecraft profiler section with the specified name + "Catgirl: ".
     * */
    fun startProfile(name: String) {
        mc.mcProfiler.startSection("Catgirl: $name")
    }

    /**
     * Ends the current minecraft profiler section.
     */
    fun endProfile() {
        mc.mcProfiler.endSection()
    }

    data class Vec2(val x: Int, val z: Int)
    data class Vec2f(var x: Float, var y: Float)
    data class Vec3f(val x: Float, val y: Float, val z: Float)
    data class Vec4f(val x: Float, val y: Float, val z: Float, val w: Float)
}

fun Event.postAndCatch(): Boolean { //THIS MAKES NO SENSE HELP
    return runCatching {
        MinecraftForge.EVENT_BUS.post(this)
    }.onFailure {
        it.printStackTrace()
        //logger.error("An error occurred", it)
        val style = ChatStyle()
        style.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/od copy ```${it.stackTraceToString().lineSequence().take(10).joinToString("\n")}```") // odon clint
        style.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText("§6Click to copy the error to your clipboard."))
        modMessage(" Caught an ${it::class.simpleName ?: "error"} at ${this::class.simpleName}. §cPlease click this message to copy and send it in the Odin discord!")}.getOrDefault(isCanceled)
}


// todo: cleanup, merge with neurepo similar shit
private val itemStackCache: MutableMap<String, ItemStack> = HashMap()

fun JsonObject.toItemStack(
    useCache: Boolean = true,
    copyStack: Boolean = false
): ItemStack {
    var cacheEnabled = useCache
    if (this == null) return ItemStack(Items.painting, 1, 10)

    val internalName = this["internalname"]?.asString ?: return ItemStack(Items.painting, 1, 10)
    if (internalName == "_") cacheEnabled = false

    if (cacheEnabled) {
        itemStackCache[internalName]?.let { cachedStack ->
            return if (copyStack) cachedStack.copy() else cachedStack
        }
    }

    val itemId = this["itemid"]?.asString ?: "minecraft:stone"
    var stack = ItemStack(Item.itemRegistry.getObject(ResourceLocation(itemId)))

    if (this.has("count")) {
        stack.stackSize = this["count"].asInt
    }

    if (stack.item == null) {
        stack = ItemStack(Item.getItemFromBlock(Blocks.stone), 0, 255) // Purple broken texture item
    } else {
        if (this.has("damage")) {
            stack.itemDamage = this["damage"].asInt
        }

        if (this.has("nbttag")) {
            try {
                val tag = JsonToNBT.getTagFromJson(this["nbttag"].asString)
                stack.tagCompound = tag
            } catch (ignored: NBTException) {
            }
        }

        if (this.has("lore")) {
            val display = stack.tagCompound?.getCompoundTag("display") ?: NBTTagCompound()
            display.setTag("Lore", this["lore"].asJsonArray.processLore())
            val tag = stack.tagCompound ?: NBTTagCompound()
            tag.setTag("display", display)
            stack.tagCompound = tag
        }
    }

    if (cacheEnabled) itemStackCache[internalName] = stack
    return if (copyStack) stack.copy() else stack
}

fun JsonArray.processLore(): NBTTagList {
    val nbtLore = NBTTagList()
    for (line in this) {
        val lineStr = line.asString
        if (!lineStr.contains("Click to view recipes!") &&
            !lineStr.contains("Click to view recipe!")) {
            nbtLore.appendTag(NBTTagString(lineStr))
        }
    }
    return nbtLore
}

fun String.toJsonObject(): JsonObject {
    val jsonElement = JsonParser().parse(this)
    return jsonElement.asJsonObject
}

fun ItemStack.toJson(): JsonObject {
    val tag = this.tagCompound ?: NBTTagCompound()

    var lore = arrayOf<String>()
    if (tag.hasKey("display", 10)) {
        val display = tag.getCompoundTag("display")
        if (display.hasKey("Lore", 9)) {
            val list = display.getTagList("Lore", 8)
            lore = Array(list.tagCount()) { list.getStringTagAt(it) }
        }
    }

    if (this.displayName.endsWith(" Recipes")) {
        this.setStackDisplayName(this.displayName.dropLast(8))
    }

    if (lore.isNotEmpty() && (lore.last().contains("Click to view recipes!") || lore.last().contains("Click to view recipe!"))) {
        lore = lore.dropLast(2).toTypedArray()
    }

    val json = JsonObject()
    json.addProperty("itemid", this.item.registryName.toString())
    json.addProperty("displayname", this.displayName)
    json.addProperty("nbttag", tag.toString())
    json.addProperty("damage", this.itemDamage)

    val jsonLore = JsonArray()
    for (line in lore) {
        jsonLore.add(JsonPrimitive(line))
    }
    json.add("lore", jsonLore)

    return json
}

