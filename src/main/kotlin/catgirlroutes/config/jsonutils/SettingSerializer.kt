package catgirlroutes.config.jsonutils

import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.impl.*
import com.google.gson.*
import java.lang.reflect.Type

class SettingSerializer : JsonSerializer<Setting<*>> {
    override fun serialize(src: Setting<*>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonObject().apply {
            when (src) {
                is BooleanSetting -> this.addProperty(src.name, src.enabled)
                is NumberSetting -> this.addProperty(src.name, src.value)
                is StringSelectorSetting -> this.addProperty(src.name, src.selected)
                is SelectorSetting -> this.addProperty(src.name, src.selected)
                is StringSetting -> this.addProperty(src.name, src.text)
                is ColorSetting -> this.addProperty(src.name, src.value.rgb)
                is ActionSetting -> this.addProperty(src.name, "Action Setting")
                is KeyBindSetting -> this.addProperty(src.name, src.value.key)
                is ListSetting<*, *> -> {
                    val array = JsonArray()
                    src.value.forEach { element ->
                        when (element) {
                            is Boolean -> array.add(JsonPrimitive(element))
                            is Number -> array.add(JsonPrimitive(element))
                            is String -> array.add(JsonPrimitive(element))
                            else -> throw IllegalArgumentException("unsupported type: $element")
                        }
                    }
                    this.add(src.name, array)
                }
            }
        }
    }
}