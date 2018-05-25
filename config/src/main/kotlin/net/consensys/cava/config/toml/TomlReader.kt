package net.consensys.cava.config.toml

import java.io.InputStream

class TomlReader {
  class TomlContent(private val content: Map<String, Any>) {
    fun keys(): Set<String> = content.keys
    fun key(key: String): Boolean = content.containsKey(key)
    fun get(key: String): String? = content.get(key)?.toString()
    fun getDouble(key: String): Double? {
      val value = content.get(key)
      if (value is Double?) {
        return value
      }
      return null
    }

    fun getBoolean(key: String): Boolean? {
      val value = content.get(key)
      if (value is Boolean?) {
        return value
      }
      return null
    }

    fun getLong(key: String): Long? {
      val value = content.get(key)
      if (value is Long?) {
        return value
      }
      return null
    }

    @Suppress("UNCHECKED_CAST")
    fun getArray(key: String): Array<Any>? {
      val value = content.get(key)
      if (value is Array<*>?) {
        return value as Array<Any>?
      }
      return null
    }

    fun _get(key: String): Any? = content.get(key)
  }

  companion object {
    fun read(input: InputStream): TomlContent {
      val map = HashMap<String, Any>()
      var content = input.bufferedReader().use { it.readText() }
      "(.*)\\s*=\\s*\"\"\"(.*)\"\"\"".toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        .findAll(content).forEach {
          map.put(trimAndDequote(it.groups[1]!!.value), it.groups[2]!!.value)
          content = content.removeRange(it.range)
        }
      "(.*)\\s*=\\s*'''(.*)'''".toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        .findAll(content).forEach {
          map.put(trimAndDequote(it.groups[1]!!.value), it.groups[2]!!.value)
          content = content.removeRange(it.range)
        }
      if (!content.isBlank()) {
        content.split("\r\n", "\n").forEach({
          "^\"?(.*?)\"?\\s*=\\s*(.*?)\\s*(#.*)?$".toRegex().findAll(it).forEach {
            map.put(it.groups[1]!!.value, castValue(it.groups[2]!!.value))
          }
        })
      }
      return TomlContent(map)
    }

    private fun castValue(value: String): Any {
      if (value == "true" || value == "false") {
        return value.toBoolean()
      }
      value.toLongOrNull()?.let {
        return it
      }
      value.toDoubleOrNull()?.let {
        return it
      }
      "^\\s*\\[(.*)\\]\\s*$".toRegex().find(value.trim())?.groups?.get(1)?.value?.let {
        return it.split(",").map(::castValue).toTypedArray()
      }
      return trimAndDequote(value)
    }

    private fun trimAndDequote(value: String): String {
      val trimmed = value.trim()
      if (trimmed.matches(Regex("^'.*'$"))) {
        return trimmed.drop(1).dropLast(1)
      } else if (trimmed.matches(Regex("^\".*\"$"))) {
        return trimmed.drop(1).dropLast(1).replace("\\\"", "\"")
        // TODO .replace("\b", "\u0008")
      }
      return trimmed
    }
  }
}
