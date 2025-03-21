package catgirlroutes.config.jsonutils


import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.impl.*
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.lang.reflect.Type

class SettingDeserializer: JsonDeserializer<Setting<*>> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Setting<*> {
        if (json?.isJsonObject == true) {
            if (json.asJsonObject.entrySet().isEmpty()) return  DummySetting("Undefined")

            /**
             * The JsonObject for a Setting should only have one property. If more properties will be needed, this
             * deserializer has to be updated.
             * For now only the first element is used.
             */
            val name = json.asJsonObject.entrySet().first().key
            val value = json.asJsonObject.entrySet().first().value

            return when {
                value.isJsonPrimitive -> when {
                    (value as JsonPrimitive).isBoolean -> BooleanSetting(name, value.asBoolean)
                    value.isNumber -> NumberSetting(name, value.asDouble)
                    value.isString -> StringSetting(name, value.asString)
                    else -> DummySetting("Undefined")
                }
                value.isJsonArray -> {
                    val list = value.asJsonArray.mapNotNull { element ->
                        when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isBoolean -> element.asBoolean
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asDouble
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString
                            else -> null
                        }
                    }.toMutableList()
                    ListSetting(name, list, "")
                }
                else -> DummySetting("Undefined")
            }
        }
        return DummySetting("Undefined")
    }
}