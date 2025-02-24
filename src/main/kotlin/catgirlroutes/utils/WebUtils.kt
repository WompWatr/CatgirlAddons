package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.configPath
import catgirlroutes.config.DataManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

suspend fun hasBonusPaulScore(): Boolean = withTimeoutOrNull(5000) {
    val response: String = URL("https://api.hypixel.net/resources/skyblock/election").readText()
    val jsonObject = JsonParser().parse(response).asJsonObject
    val mayor = jsonObject.getAsJsonObject("mayor") ?: return@withTimeoutOrNull false
    val name = mayor.get("name")?.asString ?: return@withTimeoutOrNull false
    return@withTimeoutOrNull if (name == "Paul") {
        mayor.getAsJsonArray("perks")?.any { it.asJsonObject.get("name")?.asString == "EZPZ" } == true
    } else false
} == true

suspend fun sendDataToServer(body: String, url: String = "https://cga.mvpu.pro/cga/users" ): String = withTimeoutOrNull(5000) { // arasfjoiadjf.p-e.kr
    return@withTimeoutOrNull try {
        val connection = withContext(Dispatchers.IO) {
            URL(url).openConnection()
        } as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        val writer = OutputStreamWriter(connection.outputStream)
        withContext(Dispatchers.IO) {
            writer.write(body)
        }
        withContext(Dispatchers.IO) {
            writer.flush()
        }

        val responseCode = connection.responseCode
        val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        response
    } catch (_: Exception) { "" }
} ?: ""

suspend fun getDataFromServer(url: String = "https://cga.mvpu.pro/cga/users"): String {
    return withTimeoutOrNull(10000) {
        try {
            val connection = withContext(Dispatchers.IO) {
                URL(url).openConnection()
            } as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "CGA/1.0")

            val responseCode = connection.responseCode

            if (responseCode != 200) return@withTimeoutOrNull ""
            val inputStream = connection.inputStream
            val response = inputStream.bufferedReader().use { it.readText() }

            connection.disconnect()

            response
        } catch (_: Exception) { "" }
    } ?: ""
}

suspend fun downloadImageFromServer(url: String, outputFile: File): Boolean {
    return withTimeoutOrNull(10000) {
        try {
            val connection = withContext(Dispatchers.IO) {
                URL(url).openConnection()
            } as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("Accept", "image/*")

            val responseCode = connection.responseCode

            if (responseCode != 200) return@withTimeoutOrNull false

            val contentType = connection.contentType
            if (contentType.equals("text/html")) return@withTimeoutOrNull false

            withContext(Dispatchers.IO) {
                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            connection.disconnect()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    } ?: false
}


suspend fun downloadRepo(url: String = "https://github.com/NotEnoughUpdates/NotEnoughUpdates-Repo/archive/master.zip"): Triple<List<JsonObject>, List<JsonObject>, List<JsonObject>>? {
    return withTimeoutOrNull(20000) {
        try {
            println("Downloading NeuRepo")
            val items = mutableListOf<JsonObject>()
            val mobs = mutableListOf<JsonObject>()
            val constants = mutableListOf<JsonObject>()

            val eTagFile = configPath.resolve("repo/ETAG.txt")
            withContext(Dispatchers.IO) {
                if (!eTagFile.exists()) eTagFile.parentFile?.mkdirs()?.also { eTagFile.createNewFile() }
            }

            val previousETag = String(Files.readAllBytes(eTagFile.toPath()), Charsets.UTF_8)
            val currentETag = withContext(Dispatchers.IO) {
                (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 5000
                    readTimeout = 5000
                    if (previousETag.isNotEmpty()) setRequestProperty("If-None-Match", previousETag)
                }.run {
                    takeIf { responseCode != 304 }?.getHeaderField("ETag") ?: previousETag
                }
            }

            if (currentETag != previousETag) {
                println("ETags don't match. Updating")
                withContext(Dispatchers.IO) {
                    val urlConnection = URL(url).openConnection() as HttpURLConnection
                    urlConnection.apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    if (urlConnection.responseCode == 200) {
                        ZipInputStream(urlConnection.inputStream).use { zip ->
                            var entry: ZipEntry? = zip.nextEntry
                            while (entry != null) {
                                if (entry.name.endsWith(".json")) {
//                                    println("NeuRepo: ${entry.name}")
                                    val jsonContent = zip.bufferedReader().readText()
                                    val value = JsonParser().parse(jsonContent).asJsonObject
                                    when {
                                        entry.name.contains("/items/") -> items.add(value)
                                        entry.name.contains("/mobs/") -> mobs.add(value)
                                        entry.name.contains("/constants/") -> constants.add(value)
                                    }
                                }
                                entry = zip.nextEntry
                            }
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    Files.write(eTagFile.toPath(), currentETag.toByteArray(StandardCharsets.UTF_8))
                    DataManager.saveDataToFile(configPath.resolve("repo/items.json"), items)
                    DataManager.saveDataToFile(configPath.resolve("repo/mobs.json"), mobs)
                    DataManager.saveDataToFile(configPath.resolve("repo/constants.json"), constants)
                }
            } else {
                println("ETags match")
                withContext(Dispatchers.IO) {
                    items.addAll(DataManager.loadDataFromFile(configPath.resolve("repo/items.json")))
                    mobs.addAll(DataManager.loadDataFromFile(configPath.resolve("repo/mobs.json")))
                    constants.addAll(DataManager.loadDataFromFile(configPath.resolve("repo/constants.json")))
                }
            }

            Triple(items, mobs, constants)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}