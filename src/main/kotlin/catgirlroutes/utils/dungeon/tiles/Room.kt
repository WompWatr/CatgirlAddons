package catgirlroutes.utils.dungeon.tiles

import catgirlroutes.utils.Vec2
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.lang.reflect.Type

data class Room(
    var rotation: Rotations = Rotations.NONE,
    var data: RoomData,
    var clayPos: BlockPos = BlockPos(0, 0, 0),
    val roomComponents: MutableSet<RoomComponent>,
)

data class RoomComponent(val x: Int, val z: Int, val core: Int = 0) {
    val vec2 = Vec2(x, z)
    val vec3 = Vec3(x.toDouble(), 70.0, z.toDouble())
}

data class RoomData(
    val name: String,
    val type: RoomType,
    val cores: List<Int>,
    val crypts: Int,
    val secrets: Int,
    val trappedChests: Int,
)

class RoomDataDeserializer : JsonDeserializer<RoomData> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): RoomData {
        val jsonObject = json?.asJsonObject
        val name = jsonObject?.get("name")?.asString ?: ""
        val type = context?.deserialize(jsonObject?.get("type"), RoomType::class.java) ?: RoomType.NORMAL
        val coresType = object : TypeToken<List<Int>>() {}.type
        val cores = context?.deserialize<List<Int>>(jsonObject?.get("cores"), coresType).orEmpty()
        val crypts = jsonObject?.get("crypts")?.asInt ?: 0
        val secrets = jsonObject?.get("secrets")?.asInt ?: 0
        val trappedChests = jsonObject?.get("trappedChests")?.asInt ?: 0

        return RoomData(name, type, cores, crypts, secrets, trappedChests)
    }
}