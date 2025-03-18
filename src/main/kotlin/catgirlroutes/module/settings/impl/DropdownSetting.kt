package catgirlroutes.module.settings.impl

import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.Visibility

/**
 * A setting that shows or hides settings in the ClickGui only
 */
class DropdownSetting(
    name: String,
    override val default: Boolean = false,
) : Setting<Boolean>(name, null, Visibility.CLICK_GUI_ONLY) {
    override var value: Boolean = default
    val dependentModules: MutableList<Setting<*>> = mutableListOf()
    var enabled: Boolean by this::value
}