//package catgirlroutes.ui.misc.configoverlay
//
//import catgirlroutes.CatgirlRoutes
//import catgirlroutes.commands.impl.Ring
//import catgirlroutes.commands.impl.RingManager.allRings
//import catgirlroutes.commands.impl.RingManager.loadRings
//import catgirlroutes.commands.impl.RingManager.saveRings
//import catgirlroutes.commands.impl.route
//import catgirlroutes.module.impl.dungeons.AutoP3.selectedRoute
//import catgirlroutes.ui.clickgui.util.ColorUtil
//import catgirlroutes.ui.misc.elements.impl.MiscElementButton
//import catgirlroutes.utils.ChatUtils.debugMessage
//import catgirlroutes.utils.ChatUtils.modMessage
//import catgirlroutes.utils.Utils.distanceToPlayer
//import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
//import com.google.gson.Gson
//import com.google.gson.GsonBuilder
//
//import net.minecraft.client.gui.GuiScreen
//import net.minecraft.client.gui.ScaledResolution
//import net.minecraft.client.renderer.GlStateManager
//import java.awt.Color
//import java.io.File
//
//class ConfigOverlay: GuiScreen() {
//
//    private var x = 0.0
//    private var y = 0.0
//
//    private var editingRing = false
//
//    private var display = "AutoP3"
//
//    private var routes = MiscElementButton("", width = 80.0,height = 20.0, thickness =  2.0, radius =  5.0) {
//        display = "AUTOP3"
//    }
//
//    private var nearby = MiscElementButton("", width = 80.0,height = 20.0, thickness =  2.0, radius =  5.0) {
//        display = "NEARBY"
//        editingRing = false
//    }
//
//    private var autoroutes = MiscElementButton("", width = 80.0,height = 20.0, thickness =  2.0, radius =  5.0) {
//        display = "ROUTES"
//    }
//
//    private var delete = MiscElementButton("", width = 20.0, height = 20.0, thickness = 2.0, radius = 5.0) {
//        debugMessage("DELETE")
//        when (display) {
//            "AUTOP3" -> {}
//            "AUTOROUTES" -> {}
//        }
//    }
//
//    private var copy = MiscElementButton("", width = 20.0, height = 20.0, thickness = 2.0, radius = 5.0) {
//        debugMessage("COPY")
//        when (display) {
//            "AUTOP3" -> {}
//            "AUTOROUTES" -> {}
//        }
//    }
//
//    private var export = MiscElementButton("", width = 20.0, height = 20.0, thickness = 2.0, radius = 5.0) {
//        debugMessage("EXPORT")
//        when (display) {
//            "AUTOP3" -> {}
//            "AUTOROUTES" -> {}
//        }
//    }
//
//    private val overlayWidth = 305.0
//    private val overlayHeight = 255.0
//
//    private var visibleRange = 0.0 .. 0.0
//
//    override fun initGui() {
//        val sr = ScaledResolution(mc)
//
//        y = sr.scaledHeight / 2.0 - overlayHeight / 2.0
//        x = sr.scaledWidth / 2.0 - overlayWidth / 2.0
//
//        visibleRange = (y + 25.0)..(y + 25.0 * 10)
//
//        super.initGui()
//    }
//
//    fun renderBox(text: String, value: String, offset: Double) {
//        debugMessage("RENDER")
//        drawRoundedBorderedRect(x + 30, y + offset, 20.0, 20.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//        mc.fontRendererObj.drawStringWithShadow(text + " :" + value, x.toFloat() + 35f, y.toFloat() + offset.toFloat() + 5f, Color.PINK.rgb)
//    }
//
//    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
//
//        GlStateManager.pushMatrix()
//
//        //Top Left Corner
//        var configX = x
//        var configY = y
//
//        //Main Box
//        drawRoundedBorderedRect(x - 5.0, y - 5.0, overlayWidth + 10, overlayHeight + 10, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//        drawRoundedBorderedRect(x, y + 25.0, overlayWidth, overlayHeight - 25.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//
//        //Routes
//        var allRoutes = arrayListOf<String>()
//        var offset = 30.0
//        configs.clear()
//        allRings.forEach{con ->
//            if (display != "AUTOP3") return@forEach
//            if (allRoutes.contains(con.route)) return@forEach
//            allRoutes.add(con.route)
//            val configButton = MiscElementButton(
//                name = con.route,
//                x = configX + 5.0,
//                y = configY + offset,
//                width = overlayWidth - 10.0,
//                height = 20.0,
//                thickness = 1.0,
//                radius = 3.0
//            ) {
//                if (display == "AUTOP3") {
//                    modMessage("Selected route: " + con.route)
//                    route = con.route
//                    selectedRoute.text = con.route
//                    saveRings()
//                    loadRings()
//                }
//            }
//            configs.add(configButton)
//            offset += 25.0
//        }
//
//        //Nearby Rings
//        rings.clear() //Do I even need this?
//        selected.clear()
//        allRings.forEach{ring ->
//            if (display != "NEARBY") return@forEach
//            if (ring.route == route && distanceToPlayer(ring.location.xCoord, ring.location.yCoord, ring.location.zCoord) <= 2.5) {
//                val ringButton = MiscElementButton(
//                    name = ring.type,
//                    x = configX + 5.0,
//                    y = configY + offset,
//                    width = overlayWidth - 10.0,
//                    height = 20.0,
//                    thickness = 1.0,
//                    radius = 3.0
//                ) {
//                    display = "RING"
//                    //TODO: Somehow show all the attributes of the clicked ring and make them changeable
//                }
//                rings.add(ringButton)
//                offset += 25.0
//            }
//        }
//
//        //Render Routes
//        configs.forEach{con ->
//            if (display != "AUTOP3") return@forEach
//            if (configs.isEmpty()) return
//            drawRoundedBorderedRect(con.x, con.y, con.width, con.height, con.radius, con.thickness, if (route == con.name) ColorUtil.clickGUIColor else Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//            mc.fontRendererObj.drawStringWithShadow(con.name, con.x.toFloat() + 5f, con.y.toFloat() + 6.5f, Color.PINK.rgb)
//        }
//
//        //Render Rings
//        rings.forEach{ring ->
//            if (display != "NEARBY") return@forEach
//            if (rings.isEmpty()) return
//            drawRoundedBorderedRect(ring.x, ring.y, ring.width, ring.height, ring.radius, ring.thickness, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//            mc.fontRendererObj.drawStringWithShadow(ring.name, ring.x.toFloat() + 5f, ring.y.toFloat() + 6.5f, Color.PINK.rgb)
//
//        }
//
//        //Render Tabs
//        drawRoundedBorderedRect(configX, configY, 80.0, 20.0, 3.0, 1.0, if (display == "AUTOP3") ColorUtil.clickGUIColor else Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//        mc.fontRendererObj.drawStringWithShadow("AutoP3", configX.toFloat() + 5f, configY.toFloat() + 6.5f, Color.PINK.rgb)
//
//        drawRoundedBorderedRect(configX + 85, configY, 80.0, 20.0, 3.0, 1.0, if (display == "NEARBY") ColorUtil.clickGUIColor else Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//        mc.fontRendererObj.drawStringWithShadow("Rings", configX.toFloat() + 90f, configY.toFloat() + 6.5f, Color.PINK.rgb)
//
//        drawRoundedBorderedRect(configX + 170, configY, 80.0, 20.0, 3.0, 1.0, if (display == "ROUTES") ColorUtil.clickGUIColor else Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//        mc.fontRendererObj.drawStringWithShadow("Autoroutes", configX.toFloat() + 175f, configY.toFloat() + 6.5f, Color.PINK.rgb)
//
//        //Render Buttons
//        drawRoundedBorderedRect(configX - 30, configY, 20.0, 20.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//        mc.fontRendererObj.drawStringWithShadow("\uD83D\uDDD1"/*Delete*/, configX.toFloat() - 30f, configY.toFloat(), Color.PINK.rgb)
//
//        drawRoundedBorderedRect(configX - 30, configY + 25.0, 20.0, 20.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//        mc.fontRendererObj.drawStringWithShadow("\uD83D\uDCCB"/*Copy*/, configX.toFloat() - 30f, configY.toFloat() + 25f, Color.PINK.rgb)
//
//        drawRoundedBorderedRect(configX - 30, configY + 50.0, 20.0, 20.0, 3.0, 2.0, Color(ColorUtil.bgColor), ColorUtil.clickGUIColor)
//        mc.fontRendererObj.drawStringWithShadow("\uDB81\uDC97"/*Export*/, configX.toFloat() - 30f, configY.toFloat() + 50, Color.PINK.rgb)
//
//        GlStateManager.translate(0.0, 0.0, -10.0)
//
//        //Buttons
//        delete.apply {
//            x = configX - 30.0
//            y = configY
//            render(mouseX, mouseY)
//        }
//
//        copy.apply {
//            x = configX - 30.0
//            y = configY + 25.0
//            render(mouseX, mouseY)
//        }
//
//        export.apply {
//            x = configX - 30.0
//            y = configY + 50.0
//            render(mouseX, mouseY)
//        }
//
//        //Tabs
//        routes.apply {
//            x = configX
//            y = configY
//            render(mouseX, mouseY)
//        }
//
//        nearby.apply {
//            x = configX + 85
//            y = configY
//            render(mouseX, mouseY)
//        }
//
//        autoroutes.apply {
//            x = configX + 170
//            y = configY
//            render(mouseX, mouseY)
//        }
//
//        GlStateManager.popMatrix()
//        super.drawScreen(mouseX, mouseY, partialTicks)
//    }
//
//    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
//        autoroutes.mouseClicked(mouseX, mouseY, mouseButton)
//        routes.mouseClicked(mouseX, mouseY, mouseButton)
//        nearby.mouseClicked(mouseX, mouseY, mouseButton)
//        configs.forEach{it.mouseClicked(mouseX, mouseY, mouseButton)}
//        rings.forEach{it.mouseClicked(mouseX, mouseY, mouseButton)}
//        super.mouseClicked(mouseX, mouseY, mouseButton)
//    }
//
//    override fun doesGuiPauseGame(): Boolean {
//        return false
//    }
//
//    companion object {
//        val configs: ArrayList<MiscElementButton> = arrayListOf()
//        val rings: ArrayList<MiscElementButton> = arrayListOf()
//        val selected: ArrayList<MiscElementButton> = arrayListOf()
//    }
//}
//
