package net.consensys.cava.config.toml

import com.winterbe.expekt.should
import net.consensys.cava.config.Konfig
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.function.Supplier

class TomlWriterSpec : Spek({
  val now = LocalDateTime.now()
  val header = "#####\n" +
    "## Generated on " + now + "\n" +
    "## ---------------------------------\n\n"

  describe("a toml writer") {

    it("should take a configuration and write it out") {
      val baos = ByteArrayOutputStream()
      TomlWriter.write(baos, Konfig("foo=\"bar\"".byteInputStream()), null, Supplier { now })
      baos.toString(UTF_8.name()).should.equal("${header}foo = \"bar\"\n\n")
    }

    it("should be able to write keys with spaces in them") {
      val baos = ByteArrayOutputStream()
      TomlWriter.write(baos, Konfig("\"foo foo\"=\"bar\"".byteInputStream()), null, Supplier { now })
      baos.toString(UTF_8.name()).should.equal("$header\"foo foo\" = \"bar\"\n\n")
    }

    it("should write multiline values correctly") {
      val baos = ByteArrayOutputStream()
      TomlWriter.write(baos, Konfig("foo=\"\"\"bar\nnextline\"\"\"".byteInputStream()), null, Supplier { now })
      baos.toString(UTF_8.name()).should.equal("${header}foo = \"\"\"bar\nnextline\"\"\"\n\n")
    }

    it("should escape double quotes") {
      val baos = ByteArrayOutputStream()
      TomlWriter.write(baos, Konfig("foo=\"bar\"\"".byteInputStream()), null, Supplier { now })
      baos.toString(UTF_8.name()).should.equal("${header}foo = \"bar\\\"\"\n\n")
    }

    it("should write boolean values") {
      val baos = ByteArrayOutputStream()
      TomlWriter.write(baos, Konfig("foo=false".byteInputStream()), null, Supplier { now })
      baos.toString(UTF_8.name()).should.equal("${header}foo = false\n\n")
    }

    it("should write integer values") {
      val baos = ByteArrayOutputStream()
      TomlWriter.write(baos, Konfig("foo=134".byteInputStream()), null, Supplier { now })
      baos.toString(UTF_8.name()).should.equal("${header}foo = 134\n\n")
    }

    it("should write floating values") {
      val baos = ByteArrayOutputStream()
      TomlWriter.write(baos, Konfig("foo=13.45".byteInputStream()), null, Supplier { now })
      baos.toString(UTF_8.name()).should.equal("${header}foo = 13.45\n\n")
    }

    it("should write arrays") {
      val baos = ByteArrayOutputStream()
      TomlWriter.write(baos, Konfig("foo=[\"bar\", \"foobar\"]".byteInputStream()), null, Supplier { now })
      baos.toString(UTF_8.name()).should.equal("${header}foo = [\"bar\",\"foobar\"]\n\n")
    }
  }
})
