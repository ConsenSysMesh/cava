package net.consensys.cava.config

import net.consensys.cava.config.schema.ConfigurationValidator
import net.consensys.cava.config.schema.Schema
import net.consensys.cava.config.schema.Validator

class KonfigSchemaElement(
  val type: Class<Any>?,
  val defaultValue: Any?,
  val description: String?,
  val validator: Validator?
)

class KonfigSchema private constructor(
  val elements: Map<String, KonfigSchemaElement>,
  val configurationValidator: ConfigurationValidator? = null
) : Schema {

  companion object {
    fun create(): KonfigSchema = KonfigSchema(mapOf())
  }

  private fun internalAdd(key: String, defaultValue: Any?, description: String?, validator: Validator?): Schema {
    val newElements = HashMap(elements)
    newElements.put(key, KonfigSchemaElement(defaultValue?.javaClass, defaultValue, description, validator))
    return KonfigSchema(newElements, configurationValidator)
  }

  override fun add(key: String, defaultValue: Boolean?, description: String?, validator: Validator?): Schema {
    return internalAdd(key, defaultValue, description, validator)
  }

  override fun add(key: String, defaultValue: Double?, description: String?, validator: Validator?): Schema {
    return internalAdd(key, defaultValue, description, validator)
  }

  override fun add(key: String, defaultValue: Long?, description: String?, validator: Validator?): Schema {
    return internalAdd(key, defaultValue, description, validator)
  }

  override fun add(key: String, defaultValue: String?, description: String?, validator: Validator?): Schema {
    return internalAdd(key, defaultValue, description, validator)
  }

  override fun validate(newConfigurationValidator: ConfigurationValidator): Schema {
    return KonfigSchema(HashMap(elements), newConfigurationValidator)
  }

  @Suppress("UNCHECKED_CAST")
  fun getArray(key: String): Array<Any>? {
    val defaultValue = elements[key]?.defaultValue
    if (defaultValue is Array<*>) {
      return defaultValue as Array<Any>?
    }
    return null
  }

  fun get(key: String): String? {
    val defaultValue = elements.get(key)?.defaultValue
    if (defaultValue is String) {
      return defaultValue
    }
    return null
  }

  fun getBoolean(key: String): Boolean? {
    val defaultValue = elements[key]?.defaultValue
    if (defaultValue is Boolean) {
      return defaultValue
    }
    return null
  }

  fun getLong(key: String): Long? {
    val defaultValue = elements[key]?.defaultValue
    if (defaultValue is Long) {
      return defaultValue
    }
    return null
  }

  fun getDouble(key: String): Double? {
    val defaultValue = elements[key]?.defaultValue
    if (defaultValue is Double) {
      return defaultValue
    }
    return null
  }

  fun keys(): Set<String> = elements.keys

  fun description(it: String): String? = elements[it]?.description
  fun defaultValue(it: String): Any? = elements[it]?.defaultValue
  fun type(it: String): Class<Any>? = elements[it]?.type
}
