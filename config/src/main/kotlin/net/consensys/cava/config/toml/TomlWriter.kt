package net.consensys.cava.config.toml

import net.consensys.cava.config.Konfig
import net.consensys.cava.config.KonfigSchema
import java.io.OutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.function.Supplier

class TomlWriter {
  companion object {
    fun write(
      output: OutputStream,
      config: Konfig,
      schema: KonfigSchema? = null,
      timeSupplier: Supplier<LocalDateTime> = Supplier { LocalDateTime.now() }
    ) {
      val writer = output.bufferedWriter(UTF_8)
      writer.write("#####\n## Generated on " + timeSupplier.get() + "\n## ---------------------------------")
      writer.newLine()
      writer.newLine()
      config.keys().sortedWith(compareBy({ it.toLowerCase() })).forEach({
        schema?.description(it)?.apply {
          writer.write("## " + it)
          writer.newLine()
        }
        schema?.defaultValue(it)?.apply {
          writer.write("## Default: " + it)
          writer.newLine()
        }
        val value = when (config.type(it)?.simpleName) {
          "String" -> config.get(it).map(::encodeString)
          "Boolean" -> config.getBoolean(it)
          "Double" -> config.getDouble(it)
          "Long" -> config.getLong(it)
          "Object[]" -> config.getArray(it).map({
            "[" + it.map { encodeString(it.toString()) }.joinToString(",") + "]"
          })
          else -> config.get(it).map(::encodeString)
        }
        var key = it
        if (key.contains("\\s".toRegex()) || key.contains("\"")) {
          key = key.replace("\"", "\\\"")
          key = "\"$key\""
        }
        value.ifPresent {
          writer.write(key)
          writer.write(" = ")
          writer.write(value.get().toString())
          writer.newLine()
          writer.newLine()
        }
      })
      writer.flush()
    }

    private fun encodeString(value: String): String {
      if (value.contains('\n')) {
        return "\"\"\"$value\"\"\""
      }
      return "\"${value.replace("\"", "\\\"")}\""
    }
  }
}
