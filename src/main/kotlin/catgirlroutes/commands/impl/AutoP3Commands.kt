package catgirlroutes.commands.impl

import catgirlroutes.CatgirlRoutes.Companion.mc
import com.github.stivais.commodore.Commodore
import catgirlroutes.commands.impl.RingManager.allRings
import catgirlroutes.commands.impl.RingManager.loadRings
import catgirlroutes.commands.impl.RingManager.saveRings
import catgirlroutes.module.impl.dungeons.AutoP3
import catgirlroutes.module.impl.dungeons.AutoP3.selectedRoute
import catgirlroutes.module.impl.dungeons.Blink
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.ChatUtils.getPrefix
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.Utils.distanceToPlayer
import com.github.stivais.commodore.utils.GreedyString
import com.github.stivais.commodore.utils.SyntaxException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.Vec3
import java.io.File

object RingManager {
    var rings: MutableList<Ring> = mutableListOf()
    var allRings: MutableList<Ring> = mutableListOf()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("config/catgirlroutes/rings_data.json")

    fun loadRings() {
        if (file.exists()) {
            allRings = gson.fromJson(file.readText(), object : TypeToken<List<Ring>>() {}.type)
            rings = allRings.filter { it.route == route }.toMutableList()
        }
    }

    fun saveRings() {
        file.writeText(gson.toJson(allRings))
    }
}

data class Ring(
    val type: String,
    var location: Vec3,
    var yaw: Float,
    var pitch: Float,
    var height: Float,
    var width: Float,
    var lookBlock: Vec3?,
    var depth: Float?,
    var arguments: List<String>?,
    var delay: Int?,
    var command: String?,
    var packets: MutableList<Blink.BlinkC06>,
    var route: String,
)

var route: String = selectedRoute.value
var ringEditMode: Boolean = false
var blinkEditMode: Boolean = false
var ringTypes: List<String> = listOf("velo", "walk", "look", "stop", "bonzo", "boom", "hclip", "block", "edge", "vclip", "jump", "align", "command", "blink", "movement")


val autoP3Commands = Commodore("p3") {

    literal("help").runs {
        modMessage("""
            List of AutoP3 commands:
              §7/p3 add §5<§dtype§5> [§ddepth§5] [§dargs..§5] §8: §radds a ring (§7/p3 add §rfor info)
              §7/p3 edit (em) §8: §rmakes rings inactive
              §7/p3 toggle §8: §rtoggles the module
              §7/p3 remove §5<§drange§5>§r §8: §rremoves rings in range (default value - 2)
              §7/p3 undo §8: §rremoves last placed node
              §7/p3 clearroute §8: §rclears current route
              §7/p3 clear §8: §rclears ALL routes
              §7/p3 load §5[§droute§5]§r §8: §rloads selected route
              §7/p3 save §8: §rsaves current route
              §7/p3 help §8: §rshows this message
        """.trimIndent())
    }

    literal("add") {
        runs {
            modMessage("""
                Usage: §7/p3 add §5<§dtype§5> [§ddepth§5] [§dargs..§5]
                  List of types: §7${ringTypes.joinToString()} 
                    §7- walk §8: §rmakes the player walk
                    §7- look §8: §rturns player's head
                    §7- stop §8: §rstops the player from moving
                    §7- bonzo §8: §ruses bonzo staff
                    §7- boom §8: §ruses boom tnt
                    §7- edge §8: §rjumps from block's edge
                    §7- vclip §8: §rlava clips with a specified depth
                    §7- jump §8: §rmakes the player jump
                    §7- align §8: §raligns the player with the centre of a block
                    §7- command §8: §rexecutes a specified command
                    §7- blink §8: §rteleports you
                    §7- movement §8: §rreplays a movement recording
                  List of args: §7w_, h_, delay_, look, walk, term, stop
                    §7- w §8: §rring width (w1 - default)
                    §7- h §8: §rring height (h1 - default)
                    §7- delay §8: §rring action delay (delay0 - default)
                    §7- look §8: §rturns player's head
                    §7- walk §8: §rmakes the player walk
                    §7- term §8: §ractivates the node when terminal opens
                    §7- stop §8: §rsets your velocity to 0
                    §7- fullstop §8: §rfully stops the player similar to stop ring
                    §7- exact §8: §rplaces the ring at your exact position
                    §7- block §8: §rlooks at a block instead of yaw and pitch
            """.trimIndent())
        }

        executable {
            param("type") {
                parser { string: String ->
                    if (!ringTypes.contains(string)) throw SyntaxException("Invalid argument.")
                    string
                }
                suggests { ringTypes }
            }

            runs { type: String, text: GreedyString? ->
                var depth: Float? = null
                var height = 1F
                var width = 1F
                var delay: Int? = null
                val arguments = mutableListOf<String>()
                var lookBlock: Vec3? = null
                var command: String? = null
                val packets = mutableListOf<Blink.BlinkC06>()

                if (!ringTypes.contains(type)) {
                    return@runs modMessage(
                        """
                    §cInvalid ring type!
                      §rTypes: §7${ringTypes.joinToString()}
                """.trimIndent()
                    )
                }

                /**
                 * regex shit is for commands (/p3 add command <"cmd"> [args])
                 */
                val args = text?.string?.let { Regex("\"[^\"]*\"|\\S+").findAll(it).map { m -> m.value }.toList() }
                    ?: emptyList()
                debugMessage(args)

                when (type) {
                    "block" -> {
                        lookBlock = mc.thePlayer.rayTrace(40.0, 1F).hitVec
                    }

                    "vclip" -> {
                        depth = args.firstOrNull { it.toFloatOrNull() != null }?.toFloat()
                            ?: return@runs modMessage("Usage: §7/p3 add §dvclip §5<§ddepth§5> [§dargs..§5]")
                    }

                    "command" -> {
                        command = args.firstOrNull { it.startsWith('"') && it.endsWith('"') }?.replace("\"", "")
                            ?: return@runs modMessage("Usage: §7/p3 add §dcommand §5<§d\"cmd\"§5> [§dargs..§5]")
                    }
                }

                var x = Math.round(mc.renderManager.viewerPosX * 2) / 2.0
                var y = Math.round(mc.renderManager.viewerPosY * 2) / 2.0
                var z = Math.round(mc.renderManager.viewerPosZ * 2) / 2.0

                args.forEach { arg ->
                    when {
                        arg.startsWith("w") && arg != "walk" -> width = arg.slice(1 until arg.length).toFloat()
                        arg.startsWith("h") && arg != "hclip" -> height = arg.slice(1 until arg.length).toFloat()
                        arg.startsWith("delay") -> {
                            delay = arg.substring(5).toIntOrNull() ?: return@runs modMessage("§cInvalid delay!")
                        }

                        arg == "exact" -> {
                            x = mc.renderManager.viewerPosX
                            y = mc.renderManager.viewerPosY
                            z = mc.renderManager.viewerPosZ
                        }

                        arg == "block" -> {
                            lookBlock = mc.thePlayer.rayTrace(40.0, 1F).hitVec
                            arguments.add(arg)
                        }

                        arg in listOf("stop", "look", "walk", "term", "fullstop", "block", "term") -> arguments.add(arg)
                    }
                }

                if (type == "align") width = 1.0F

                val location = Vec3(x, y, z)
                val yaw = mc.renderManager.playerViewY
                val pitch = mc.renderManager.playerViewX

                val ring = Ring(
                    type,
                    location,
                    yaw,
                    pitch,
                    height,
                    width,
                    lookBlock,
                    depth,
                    arguments,
                    delay,
                    command,
                    packets,
                    route
                )

                allRings.add(ring)

                modMessage("${type.capitalize()} ring placed!")
                saveRings()
                loadRings()

            }
        }
    }

    literal("edit").runs {
        ringEditMode = !ringEditMode
        modMessage("EditMode ${if (ringEditMode) "§aenabled" else "§cdisabled"}")
    }

    literal("em").runs {
        ringEditMode = !ringEditMode
        modMessage("EditMode ${if (ringEditMode) "§aenabled" else "§cdisabled"}")
    }

    literal("bem").runs {
        blinkEditMode = !blinkEditMode
        modMessage("Blink edit ${if (blinkEditMode) "§aenabled" else "§cdisabled"}")
    }

    literal("toggle").runs {
        AutoP3.onKeyBind()
    }

    literal("remove").runs { range: Double? ->
        allRings = allRings.filter { ring -> ring.route != route || distanceToPlayer(ring.location.xCoord, ring.location.yCoord, ring.location.zCoord) >= (range ?: 2.0) }.toMutableList()
        modMessage("Removed")
        saveRings()
        loadRings()
    }

    literal("undo").runs {
        modMessage("Undone " + allRings.last().type)
        allRings.removeLast()
        saveRings()
        loadRings()
    }

    literal("clearroute").runs {
        mc.thePlayer?.addChatMessage(
            ChatComponentText("${getPrefix()} §8»§r Are you sure you want to clear §nCURRENT§r route?")
                .apply {
                    chatStyle = ChatStyle().apply {
                        chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/p3 clearrouteconfirm")
                        chatHoverEvent = HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ChatComponentText("Click to clear §nCURRENT§r route!")
                        )
                    }
                }
        )
    }

    literal("clearrouteconfirm").runs {
        allRings = allRings.filter { ring -> ring.route != route }.toMutableList()
        modMessage("$route cleared!")
        saveRings()
        loadRings()
    }

    literal("clear").runs {
        mc.thePlayer?.addChatMessage(
            ChatComponentText("${getPrefix()} §8»§r Are you sure you want to clear §nALL§r routes?")
                .apply {
                    chatStyle = ChatStyle().apply {
                        chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/p3 clearconfirm")
                        chatHoverEvent = HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ChatComponentText("Click to clear §nALL§r routes!")
                        )
                    }
                }
        )
    }

    literal("clearconfirm").runs {
        allRings = mutableListOf()
        modMessage("All routes cleared!")
        saveRings()
        loadRings()
    }

    literal("load").runs { routeName: String? ->
        route = routeName ?: selectedRoute.value
        selectedRoute.text = route
        modMessage("Loaded $route")
        loadRings()
    }

    literal("save").runs {
        modMessage("Saved")
        saveRings()
    }
}