package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.StringSelectorSetting
import catgirlroutes.module.settings.impl.StringSetting
import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.Utils.noControlCodes
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

object PhoenixAuth: Module(
    "Phoenix Auth",
    Category.MISC,
    "Automatically authenticate and toggle modules when connecting to your phoenix proxy"
) {
    private val phoenixProxy = StringSetting("Proxy IP", "hypixelcgaproxy.duckdns.org", 9999, "Your phoenix proxy IP")
    private val phoenixModule = StringSelectorSetting("Module", "autoterms", arrayListOf("autoterms", "zeropingterminals"))
    private var onPenix = false

    init {
        addSettings(this.phoenixProxy, this.phoenixModule)
    }

    @SubscribeEvent
    fun onConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        this.onPenix = mc.runCatching {
            !event.isLocal && ((thePlayer?.clientBrand?.lowercase()?.equals(phoenixProxy.text)
                ?: currentServerData?.serverIP?.lowercase()?.equals(phoenixProxy.text)) == true)
        }.getOrDefault(false)
        if (this.onPenix) scheduleTask(1) { ChatUtils.commandAny("phoenixauth") }
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        this.onPenix = false
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2 || !this.onPenix) return
        if (event.message.unformattedText.noControlCodes == "[Phoenix] Successfully transferred!") {
            scheduleTask(1) { ChatUtils.sendChat("p.toggle ${this.phoenixModule.selected}") }
        }
    }
}