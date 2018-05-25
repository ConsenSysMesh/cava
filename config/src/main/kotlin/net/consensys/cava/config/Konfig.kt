package net.consensys.cava.config

import net.consensys.cava.config.schema.Schema
import net.consensys.cava.config.schema.ValidationResults
import net.consensys.cava.config.toml.TomlReader
import net.consensys.cava.config.toml.TomlWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.Optional
import java.util.function.Supplier
import java.util.function.Consumer

class Konfig(content: InputStream, schema: Schema? = null, private val validator: Consumer<ValidationResults>? = null) :
  Configuration {
  private val toml: TomlReader.TomlContent = TomlReader.read(content)

  private val schema: KonfigSchema? = schema as KonfigSchema?

  init {

    val results = DefaultValidationResults()

    validator?.let {
      this.schema?.elements?.entries?.forEach {
        it.value.validator?.validate(it.key, toml._get(it.key))?.let {
          results.errorMessages.addAll(it)
        }
      }
      this.schema?.configurationValidator?.validate(this)?.let {
        results.errorMessages.addAll(it)
      }
      validator.accept(results)
    }
  }

  override fun getArray(key: String): Optional<Array<Any>> {
    val result = toml.getArray(key) ?: schema?.getArray(key)
    return Optional.ofNullable(result)
  }

  override fun key(key: String): Boolean = toml.key(key)

  override fun get(key: String): Optional<String> = Optional.ofNullable(toml.get(key) ?: schema?.get(key))

  override fun getDouble(key: String): Optional<Double> =
    Optional.ofNullable(toml.getDouble(key) ?: schema?.getDouble(key))

  override fun getBoolean(key: String): Optional<Boolean> =
    Optional.ofNullable(toml.getBoolean(key) ?: schema?.getBoolean(key))

  override fun getLong(key: String): Optional<Long> = Optional.ofNullable(toml.getLong(key) ?: schema?.getLong(key))

  override fun keys(): Set<String> = toml.keys().union(schema?.keys().orEmpty())

  fun export(output: OutputStream, timeSupplier: Supplier<LocalDateTime>) {
    TomlWriter.write(output, this, schema, timeSupplier)
  }

  fun type(it: String): Class<Any>? = schema?.type(it) ?: toml._get(it)?.javaClass

  @Throws(IOException::class)
  override fun save(path: Path) {
    save(path, Supplier { LocalDateTime.now() })
  }

  @Throws(IOException::class)
  fun save(path: Path, timeSupplier: Supplier<LocalDateTime>) {
    Files.createDirectories(path.parent)
    Files.newOutputStream(path).use { output -> this.export(output, timeSupplier) }
  }

  override fun toString(): String {
    return toString(Supplier { LocalDateTime.now() })
  }

  fun toString(timeSupplier: Supplier<LocalDateTime>): String {
    val output = ByteArrayOutputStream()
    this.export(output, timeSupplier)
    return String(output.toByteArray(), UTF_8)
  }
}
