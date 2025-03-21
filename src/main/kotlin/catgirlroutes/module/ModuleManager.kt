package catgirlroutes.module

import catgirlroutes.events.impl.PreKeyInputEvent
import catgirlroutes.events.impl.PreMouseInputEvent
import catgirlroutes.module.ModuleManager.modules
import catgirlroutes.module.impl.dungeons.*
import catgirlroutes.module.impl.dungeons.puzzlesolvers.Puzzles
import catgirlroutes.module.impl.misc.*
import catgirlroutes.module.impl.player.*
import catgirlroutes.module.impl.render.*
import catgirlroutes.module.settings.NoShowInList
import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.KeyBindSetting
import catgirlroutes.ui.hud.EditHudGUI
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


/**
 * # This object handles all the modules of the mod.
 *
 * After making a [Module] it just has to be added to the [modules] list and
 * everything else will be taken care of automatically. This entails:
 *
 * + It will be added to the click gui in the order it is put in here. But keep in mind that the category is set within
 * the module. The comments here are only for readability.
 *
 * + All settings that are registered within the module will be saved to and loaded from the module config.
 * For this to properly work remember to register the settings to the module.
 *
 * + The module will be registered and unregistered to the forge eventbus when it is enabled / disabled.
 *
 * + The module will be informed of its keybind presses.
 *
 *
 * @author Aton
 * @see Module
 * @see Setting
 */
object ModuleManager {
    /**
     * All modules have to be added to this list to function!
     */
    val modules: java.util.ArrayList<Module> = arrayListOf(
        // Dungeons
        AutoP3,
        AutoP3ConfigOverlay,
        AutoRoutes,
        GhostBlocks,
        InstaMid,
        LavaClip,
        Secrets,
        SecretAura,
        Test,
        StormClip,
        AutoLeap,
        Zpew,
        AutoPot,
        Puzzles,
        Blink,
        AutoExcavator,
        Relics,
        Doorless,
        CoreClip,
        LeapOrganiser,
        AutoSS,
        Auto4,

        // Misc
        F7sim,
        CatMode,
        InventoryButtons,
        InstantSprint,
        NickHider,
        InvincibilityTimer,
        AutoDialogue,
        AutoClicker,
//        DojoHelper,
        AutoKick,
        AutoWardrobe,
        WitherCloak,
        Inventory,
        PhoenixAuth,
        RendHelper,

        // Player
        AutoSprint,
        HClip,
        PearlClip,
        VerticalJerry,
        NoDebuff,
        BlockClip,
        BarPhase,
        Animations,
        PlayerDisplay,

        // Render
        ClickGui,
        CgaUser,
        BossESP,
        TerminalEsp,
        ModuleList,
        EditHud,
        Trail,
        DungeonESP,
        ScreenImage,
    )

    /**
     * Adds "Show in List" [BooleanSetting] unless [Module] class has annotation [NoShowInList]
     * Also adds "Key Bind" [KeyBindSetting] that adds a key bind to toggle the module
     */
    init {
        for (module in modules) {
//            if (!module::class.hasAnnotation<NoShowInList>()) {
//                module.addSettings(BooleanSetting("Show in List", true, null, Visibility.ADVANCED_ONLY))
//            }
            module.keybinding.let {
                module.register(KeyBindSetting("Key Bind", it, description = "Toggles the module"))
            }
        }
    }

    /**
     * Loads in all modules and their elements.
     *
     * Self registering modules are loaded by this.
     * Self registering Hud elements will also be loaded.
     *
     * This method also accesses instances of all modules and their hud elements.
     * That way all module instances are created and loaded into memory.
     *
     * This step is required before the config is loaded.
     */
    fun loadModules() {
        modules.forEach {
            it.loadModule()
        }
    }

    /**
     * Initialize the Modules.
     * This is run on game startup during the FMLInitializationEvent.
     */
    fun initializeModules() {
        modules.forEach {
            it.initializeModule()
            EditHudGUI.addHUDElements(it.hudElements)
        }
    }
    /**
     * Handles the key binds for the modules.
     * Note that the custom event fired in the minecraft mixin is used here and not the forge event.
     * That is done to run this code before the vanilla minecraft code.
     */
    @SubscribeEvent
    fun activateModuleKeyBinds(event: PreKeyInputEvent) {
//        modules.stream().filter { module -> module.keyCode == event.key }.forEach { module -> module.onKeyBind() }
        for (module in modules) {
            for (setting in module.settings) {
                if (setting is KeyBindSetting && setting.value.key == event.key) {
                    setting.value.onPress?.invoke();
                }
            }
        }
    }

    /**
     * Handles the key binds for the modules.
     * Note that the custom event fired in the minecraft mixin is used here and not the forge event.
     * That is done to run this code before the vanilla minecraft code.
     */
    @SubscribeEvent
    fun activateModuleMouseBinds(event: PreMouseInputEvent) {
//        modules.stream().filter { module -> module.keyCode + 100 == event.button }.forEach { module -> module.onKeyBind() }
        for (module in modules) {
            for (setting in module.settings) {
                if (setting is KeyBindSetting && setting.value.key + 100 == event.button) {
                    setting.value.onPress?.invoke();
                }
            }
        }
    }

    fun getModuleByName(name: String): Module? {
        return modules.find{ it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Loads all classes from all jars in [MODULES_PATH].
     *
     * From those classes all valid Module Instances get extracted and returned as a list for registering.
     *
     * Valid Modules Instances must either be kotlin objects annotated with [SelfRegisterModule].
     *
     *     @SelfRegisterModule
     *     object ExternalModule : Module("External Module", category = Category.MISC) {
     *          // Module code
     *     }
     * Or java classes with that annotation which have a public static field called **INSTANCE** which is initialized as
     * an instance of the module class.
     *
     *     @SelfRegisterModule
     *     public class ExternalJavaModule extends Module {
     *         @NotNull
     *         public static final ExternalJavaModule INSTANCE = new ExternalJavaModule();
     *
     *         private ExternalJavaModule() {
     *             super("External Java Module", Category.MISC, "An external Java Module");
     *         }
     *         // Module code
     *     }
     */
}
