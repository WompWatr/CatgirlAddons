package catgirlroutes.module.settings.impl

import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.Visibility

class ListSetting<E, T : MutableCollection<E>>(
    name: String,
    override val default: T,
    description: String = ""
): Setting<T>(name, description, Visibility.HIDDEN) {
    override var value: T = default
}