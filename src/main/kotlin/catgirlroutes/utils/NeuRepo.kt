package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.scope
import catgirlroutes.ui.clickgui.util.FontUtil.capitalizeOnlyFirst
import catgirlroutes.utils.Utils.intToRoman
import catgirlroutes.utils.Utils.noControlCodes
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraft.util.ResourceLocation

object NeuRepo {
    var repoItems = mutableListOf<RepoItem>()
    var mobs = mutableListOf<JsonObject>()
    var constants = mutableListOf<JsonObject>()

    init {
        updateRepo()
    }

    private fun updateRepo() {
        runBlocking(scope.coroutineContext) {
            val result = downloadRepo()
            result?.let {
                val gson = Gson()

                val repoItemLists: List<RepoItem> = gson.fromJson(gson.toJson(it.first), object : TypeToken<List<RepoItem>>() {}.type)

                repoItems = repoItemLists.map { item ->
                    // convert shitty neu skyblockID to normal format (WISDOM;2 -> ENCHANTMENT_WISDOM_2)
                    if (item.skyblockID.contains(";") && item.id == "minecraft:enchanted_book") {
                        val parts = item.skyblockID.split(";")
                        if (parts.size == 2) {
                            var enchantment = parts[0]
                            val tier = parts[1]
                            item.skyblockID = "ENCHANTMENT_${enchantment}_${tier}"
                            // also change name of enchantments
                            val colour = if (enchantment.startsWith("ULTIMATE")) "§9§d§l" else "§9"
                            enchantment = if (enchantment.contains("WISE")) enchantment else enchantment.replace("ULTIMATE_", "")
                            item.name = "$colour${enchantment.replace("_", " ").capitalizeWords()} ${intToRoman(tier.toInt())}"
                        }
                    }
                    item
                }.toMutableList()

                mobs = it.second.toMutableList()
                constants = it.third.toMutableList()
            }
            updateItemsWithAuctionData()
            updateItemsWithBazaarData()
            println("Successfully loaded NeuRepo")
        }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalizeOnlyFirst() }

    private fun updateItemsWithAuctionData() = runBlocking {
        val auctionDataJson = getDataFromServer("https://moulberry.codes/lowestbin.json")
        if (auctionDataJson.isNotEmpty()) {
            val auctionMap = Gson().fromJson(auctionDataJson, JsonObject::class.java)
                .entrySet().associate { it.key to it.value.asDouble }

            repoItems.forEach { item ->
                val price = auctionMap[item.skyblockID] ?: 0.0
                item.auction = price > 0.0
                item.price = price
            }
        }
    }

    private fun updateItemsWithBazaarData() = runBlocking {
        val bazaarDataJson = getDataFromServer("https://api.hypixel.net/v2/skyblock/bazaar")
        if (bazaarDataJson.isNotEmpty()) {
            val products = Gson().fromJson(bazaarDataJson, JsonObject::class.java)
                .getAsJsonObject("products") ?: return@runBlocking

            repoItems.forEach { item ->
                val price = products[item.skyblockID]?.asJsonObject
                    ?.getAsJsonObject("quick_status")?.get("buyPrice")?.asDouble ?: 0.0
                if (price > 0.0) {
                    item.bazaar = true
                    item.price = price
                }
            }
        }
    }

    fun getItemFromID(skyblockID: String): RepoItem? = repoItems.find { it.skyblockID == skyblockID }

    fun getItemFromName(name: String, contains: Boolean = true): RepoItem? {
        return if (contains) {
            repoItems.find { it.name.noControlCodes.contains(name) }
        } else {
            repoItems.find { it.name == name }
        }
    }

    // modified schizo shit from neu
    private val itemStackCache: MutableMap<String, ItemStack> = HashMap()
    fun RepoItem.toStack(
        useCache: Boolean = true,
        copyStack: Boolean = false
    ): ItemStack {
        var cacheEnabled = useCache

        if (this.skyblockID == "_") cacheEnabled = false

        if (cacheEnabled) {
            itemStackCache[this.skyblockID]?.let { stack ->
                return if (copyStack) stack.copy() else stack
            }
        }

        val stack = ItemStack(net.minecraft.item.Item.itemRegistry.getObject(ResourceLocation(this.id)), 1, this.damage)

        if (stack.item == null) {
            return ItemStack(net.minecraft.item.Item.getItemFromBlock(Blocks.stone), 0, 255) // Purple broken texture item
        } else {
            try {
                val tag: NBTTagCompound = JsonToNBT.getTagFromJson(this.nbt)
                stack.tagCompound = tag
            } catch (ignored: NBTException) { }

            val display = NBTTagCompound().apply {
                if (stack.tagCompound != null && stack.tagCompound!!.hasKey("display")) {
                    this.merge(stack.tagCompound!!.getCompoundTag("display"))
                }
                this.setTag("Lore", this@toStack.lore.processLore())
            }
            val tag = stack.tagCompound ?: NBTTagCompound()
            tag.setTag("display", display)
            stack.tagCompound = tag
        }

        if (cacheEnabled) itemStackCache[this.id] = stack
        return if (copyStack) stack.copy() else stack
    }

    private fun List<String>.processLore(): NBTTagList {
        val nbtLore = NBTTagList()
        for (line in this) {
            if (!line.contains("Click to view recipes!") &&
                !line.contains("Click to view recipe!")) {
                nbtLore.appendTag(NBTTagString(line))
            }
        }
        return nbtLore
    }
}

data class RepoItem(
    @SerializedName("itemid") val id: String,
    @SerializedName("displayname") var name: String,
    @SerializedName("nbttag") val nbt: String,
    @SerializedName("lore") val lore: List<String>,
    @SerializedName("internalname") var skyblockID: String,
    @SerializedName("damage") val damage: Int,
    var auction: Boolean = false,
    var bazaar: Boolean = false,
    var price: Double = 0.0
)