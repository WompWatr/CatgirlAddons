package catgirlroutes

import catgirlroutes.commands.impl.*
import catgirlroutes.commands.CommandRegistry
import catgirlroutes.config.InventoryButtonsConfig
import catgirlroutes.config.ModuleConfig
import catgirlroutes.events.EventDispatcher
import catgirlroutes.module.ModuleManager
import catgirlroutes.ui.clickgui.ClickGUI
import catgirlroutes.ui.clickguinew.ClickGUI as ClickGUINew
import catgirlroutes.utils.*
import catgirlroutes.utils.clock.Executor
import catgirlroutes.utils.dungeon.DungeonUtils
import catgirlroutes.utils.dungeon.LeapUtils
import catgirlroutes.utils.dungeon.ScanUtils
import catgirlroutes.utils.rotation.FakeRotater
import catgirlroutes.utils.rotation.Rotater
import catgirlroutes.utils.rotation.RotationUtils
import catgirlroutes.utils.rotation.rotationDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

@Mod(
    modid = CatgirlRoutes.MOD_ID,
    name = CatgirlRoutes.MOD_NAME,
    version = CatgirlRoutes.MOD_VERSION,
    clientSideOnly = true
)
class CatgirlRoutes {
    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        ModuleManager.loadModules()

        listOf(
            this,
            ModuleManager,
            LocationManager,
            Executor,
            DungeonUtils,
            ScanUtils,
            ClientListener,
            FakeRotater,
            Rotater,
            MovementUtils,
            RotationUtils,
            EventDispatcher,
            VecUtils,
            Notifications,
            PlayerUtils,
            EntityAura,
            BlockAura,
            LeapUtils,
            CgaUsers,
            NeuRepo,
            SkyblockPlayer
        ).forEach(MinecraftForge.EVENT_BUS::register)

        CommandRegistry.register()
    }
    @Mod.EventHandler
    fun postInit(event: FMLLoadCompleteEvent) = runBlocking {
        //Load in the module config post init so that all the minecraft classes are already present.
        runBlocking {
            launch(Dispatchers.IO) {
                moduleConfig.loadConfig()
            }
        }
        InventoryButtonsConfig.load()
        ModuleManager.initializeModules()

        clickGUI = ClickGUI()
        clickGUINew = ClickGUINew()
    }
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        tickRamp++
        totalTicks++
        if (display != null) {
            mc.displayGuiScreen(display)
            display = null
        }
        if (tickRamp % 20 == 0) {
            tickRamp = 0
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        moduleConfig.saveConfig()
    }

    @SubscribeEvent
    fun onWorldChange(@Suppress("UNUSED_PARAMETER") event: WorldEvent.Load) {
        tickRamp = 18
    }
    companion object {
        const val MOD_ID = "cga"
        const val MOD_NAME = "CatgirlRoutes"
        const val MOD_VERSION = "@VER@"

        const val CHAT_PREFIX = "§5[§dCatgirlAddons§5]§r"
        const val SHORT_PREFIX = "§5[§dCga§5]§r"
        const val RESOURCE_DOMAIN = "catgirlroutes"
        const val CONFIG_DOMAIN = RESOURCE_DOMAIN
        val mc: Minecraft = Minecraft.getMinecraft()
        val scope = CoroutineScope(EmptyCoroutineContext)

        lateinit var clickGUI: ClickGUI
        lateinit var clickGUINew: ClickGUINew
        val configPath = File(mc.mcDataDir, "config/$CONFIG_DOMAIN")
        val moduleConfig = ModuleConfig(configPath)
        val onHypixel: Boolean  by LocationManager::onHypixel
        var display: GuiScreen? = null
        var tickRamp = 0
        var totalTicks: Long = 0
    }
}