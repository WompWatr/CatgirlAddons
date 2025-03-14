package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.utils.ChatUtils.devMessage
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.PlayerUtils.rightClick
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import kotlin.random.Random

object AutoExcavator : Module(
    name = "Auto Excavator",
    category = Category.MISC
) {
    val clickDelay = NumberSetting("Click Delay", 150.0, 0.0, 300.0, 10.0, unit = "ms")
    val randomDelay = NumberSetting("Random Delay", 0.0, 0.0, 100.0, 5.0, unit = "ms")
    val firstClickDelay = NumberSetting("First Click Delay", 300.0, 0.0, 1000.0, 10.0, unit = "ms")
    init {
        this.addSettings(
            clickDelay,
            randomDelay,
            firstClickDelay
        )
    }
    var phase = 1
    var menuOne = mutableListOf<Int>()
    var menuTwo = mutableListOf<Int>()
    var scrapFound = false
    var chiselFound = false
    var lastClick = System.currentTimeMillis()
    var shouldClick = false

    @SubscribeEvent
    fun onS2D(event: PacketReceiveEvent) {
        if (!this.enabled) return
        if (event.packet !is S2DPacketOpenWindow) return
        lastClick = System.currentTimeMillis() + firstClickDelay.value.toLong()
        val title = event.packet.windowTitle.unformattedText
        if (!title.contains("Fossil")) return
        shouldClick = false
        scrapFound = false
        chiselFound = false

        if (phase == 1) {
            menuOne.add(31)
            phase = 2
        } else {
            phase = 1
        }
    }

    @SubscribeEvent
    fun onS2F(event: PacketReceiveEvent) {
        if (event.packet !is S2FPacketSetSlot) return
        val slot = event.packet.func_149173_d()
        val itemStack = event.packet.func_149174_e()

        val item = itemStack?.item
        val name = itemStack?.displayName //Dirt
        val metadata = itemStack?.metadata
        val registryName = item?.registryName //minecraft:stained_glass_pane
                
        if (phase == 1) {
            if (registryName == "minecraft:stained_glass_pane") {
                when (metadata) {
                    12 -> menuTwo.add(slot)
                    5 -> menuTwo.add(0, slot)
                    4 -> {
                        shouldClick = false
                        menuTwo.clear()
                        menuOne.clear()
                    }
                }
            }
            if (slot > 80) {
                
                shouldClick = true
            }
            if (slot <= 53 && name?.contains("Fossil") == true) {
                shouldClick = false
                menuOne.clear()
                menuTwo.clear()
                modMessage("Fossil found! You can now use a other mod to find the fossil")
                //TODO implement a way to automatically use the algorithm for finding fossil and then click these slots but for now we will just exit
                return
            }
            
        }
        if (phase == 2) {
            if (name?.contains("Chisel") == true && slot > 53) {
                if (chiselFound) return
                chiselFound = true
                devMessage("Chisel found")
                menuOne.add(0, slot)
            }
            if (name?.contains("Scrap") == true && slot > 53) {
                if (scrapFound) return
                scrapFound = true
                menuOne.add(0, slot)
                devMessage("Scrap found")
            }
            if (slot > 80) {
                shouldClick = true
            }

            
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (mc.ingameGUI == null) return
        if (!shouldClick) return
        if (lastClick + clickDelay.value + (if (randomDelay.value <= 0) 0.0 else Random.nextDouble(0.0, randomDelay.value)) > System.currentTimeMillis()) return
        lastClick = System.currentTimeMillis()
        if (phase == 2) {
            if (!scrapFound || !chiselFound) {
                phase = 2
                modMessage("No scrap or chisel found!")
                menuOne.clear()
                menuTwo.clear()
                shouldClick = false
                return
            }
            if (menuOne.isEmpty()) {
                shouldClick = false
                return
            }
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, menuOne.removeFirst(), 2, 3, mc.thePlayer)
        }
        if (phase == 1) {
            if (menuTwo.isEmpty()) {
                shouldClick = false
                return
            }
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, menuTwo.removeFirst(), 2, 3, mc.thePlayer)
        }
    }

    override fun onKeyBind() {
        rightClick()
    }

    @SubscribeEvent
    fun onCloseServer(event: PacketReceiveEvent) {
        if (event.packet !is S2EPacketCloseWindow) return
        phase = 1
    }

    @SubscribeEvent
    fun onCloseClient(event: PacketReceiveEvent) {
        if (event.packet !is C0DPacketCloseWindow) return
        phase = 1
    }
}
