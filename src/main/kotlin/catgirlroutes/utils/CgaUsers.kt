package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.CONFIG_DOMAIN
import catgirlroutes.CatgirlRoutes.Companion.RESOURCE_DOMAIN
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.CatgirlRoutes.Companion.scope
import com.google.gson.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import java.io.File
import java.io.StringReader
import java.lang.reflect.Type
import javax.imageio.ImageIO

object CgaUsers { // todo: add capes with gif

    var users: HashMap<String, CgaUser> = HashMap()

    data class CgaUser(
        val xScale: Float = 1f,
        val yScale: Float = 1f,
        val zScale: Float = 1f,
        val cape: ResourceLocation = ResourceLocation(RESOURCE_DOMAIN, "default_cape.png")
    )

    data class UserData(
        val username: String,
        val uuid: String,
        val permission: Int,
        val dimensions: Triple<Float, Float, Float>,
        val cape: String
    )

    class UserDeserializer : JsonDeserializer<UserData> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): UserData {
            val jsonObject = json?.asJsonObject
            val name = jsonObject?.get("name")?.asString
            val uuid = jsonObject?.get("uuid")?.asString
            val permission = jsonObject?.get("permission")?.asInt
            val dimensionsJsonObject = JsonParser().parse(jsonObject?.get("dimensions")?.asString?.let { StringReader(it) })?.asJsonObject
            val sizeTriple = Triple(
                dimensionsJsonObject?.get("x")?.asFloat ?: 0f,
                dimensionsJsonObject?.get("y")?.asFloat ?: 0f,
                dimensionsJsonObject?.get("z")?.asFloat ?: 0f
            )
//            val cape = jsonObject?.get("cape")?.asString
            // cape is null because I added a new column in table
            val cape = if (jsonObject?.get("cape")?.isJsonNull == true) { // todo: remove when all users updated
                "yxrdIat"
            } else {
                jsonObject?.get("cape")?.asString
            }

            return UserData(name ?: "", uuid ?: "", permission ?: 1, sizeTriple, cape ?: "yxrdIat")
        }
    }

    fun updateUsers(): HashMap<String, CgaUser> {
        runBlocking(scope.coroutineContext) {
            val data = getDataFromServer().ifEmpty { return@runBlocking }
            val gson = GsonBuilder().registerTypeAdapter(UserData::class.java, UserDeserializer())?.create() ?: return@runBlocking
            gson.fromJson(data, Array<UserData>::class.java).forEach {
                val capeResourceLocation = getCapeResourceLocation(it.cape)
                users[it.username] = CgaUser(it.dimensions.first, it.dimensions.second, it.dimensions.third, capeResourceLocation)
            }
        }
        // add player if not on the list
        if (mc.thePlayer != null && !users.containsKey(mc.thePlayer.name)) catgirlroutes.module.impl.render.CgaUser.updateUser.doAction()
        println(users)
        return users
    }

    private suspend fun getCapeResourceLocation(capeId: String): ResourceLocation {
        return run {
            val dir = File("${mc.mcDataDir}/config/$CONFIG_DOMAIN/capes")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val capeFile = File("${dir.path}/$capeId.png")
            if (!capeFile.exists()) {
                val imageUrl = "https://i.imgur.com/$capeId.png"
                if (!downloadImageFromServer(imageUrl, capeFile)) {
                    println("Failed to download cape: $imageUrl")
//                    if (mc.theWorld != null) ChatUtils.modMessage("Failed to download cape: $imageUrl")
                    return@run ResourceLocation(RESOURCE_DOMAIN, "default_cape.png")
                }
            }

            // I don't even know what this shit is but ty stackoverflow
            // it's something like promise idk
            val deferredResource = CompletableDeferred<ResourceLocation>()

            mc.addScheduledTask {
                try {
                    val texture = DynamicTexture(ImageIO.read(capeFile))
                    val resourceLocation = mc.textureManager.getDynamicTextureLocation(RESOURCE_DOMAIN, texture)
                    deferredResource.complete(resourceLocation)
                } catch (e: Exception) {
                    deferredResource.completeExceptionally(e)
                }
            }

            try {
                deferredResource.await()
            } catch (e: Exception) {
                e.printStackTrace()
                ResourceLocation(RESOURCE_DOMAIN, "default_cape.png")
            }

        }
    }

    init {
        updateUsers()
    }

}
