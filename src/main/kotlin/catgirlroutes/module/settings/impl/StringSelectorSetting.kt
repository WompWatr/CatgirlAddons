package catgirlroutes.module.settings.impl

import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.Visibility
import net.minecraft.util.MathHelper

class StringSelectorSetting(
    name: String,
    private val defaultSelected: String,
    var options: ArrayList<String>,
    description: String? = null,
    visibility: Visibility = Visibility.VISIBLE,
) : Setting<Int>(name, description, visibility){

    override val default: Int = optionIndex(defaultSelected)
    override var value: Int
        get() = index
        set(value) {index = value}

    var index: Int = optionIndex(defaultSelected)
     set(value) {
         /** guarantees that index is in bounds and enables cycling behaviour */
         val newVal = processInput(value)
         field = if (newVal > options.size - 1)  0 else if ( newVal < 0) options.size - 1 else newVal
     }

    var selected: String
     set(value) {
        index = optionIndex(value)
    }
    get() {
        return options[index]
    }

    /**
     * Finds the index of given option in the option list.
     * Ignores the case of the strings and returns 0 if not found.
     */
    private fun optionIndex(string: String): Int {
        return MathHelper.clamp_int(this.options.map { it.lowercase() }.indexOf(string.lowercase()), 0, options.size - 1)
    }

    fun isSelected(string: String): Boolean {
        return  this.selected.equals(string, ignoreCase = true)
    }
}