package net.consensys.cava.config.toml

import com.winterbe.expekt.should
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.Arrays

class TomlReaderSpec : Spek({
  describe("a toml reader") {

    it("should read valid toml content and return its content") {
      val content = TomlReader.read("foo=\"bar\"".byteInputStream())
      content.get("foo").should.equal("bar")
      content.keys().should.equal(setOf("foo"))
    }

    it("should filter out comments") {
      val content = TomlReader.read("# Some comment here \nfoo=\"bar\"\n # Spaced-out comment".byteInputStream())
      content.get("foo").should.equal("bar")
      content.keys().should.equal(setOf("foo"))
    }

    it("should read multiline values") {
      val content = TomlReader.read("foo=\"\"\"bar\nfoobar\"\"\"".byteInputStream())
      content.get("foo").should.equal("bar\nfoobar")
      content.keys().should.equal(setOf("foo"))
    }

    it("should read multiline values with single quotes") {
      val content = TomlReader.read("foo='''bar\nfoobar'''".byteInputStream())
      content.get("foo").should.equal("bar\nfoobar")
      content.keys().should.equal(setOf("foo"))
    }

    it("should read multiline values with CRLF EOLs") {
      val content = TomlReader.read("foo=\"\"\"bar\r\nfoobar\"\"\"".byteInputStream())
      content.get("foo").should.equal("bar\r\nfoobar")
      content.keys().should.equal(setOf("foo"))
    }

    it("should read numbers") {
      val content = TomlReader.read("foo=13".byteInputStream())
      content.getLong("foo").should.equal(13)
      content.keys().should.equal(setOf("foo"))
    }

    it("should read floating numbers") {
      val content = TomlReader.read("foo=13.4".byteInputStream())
      content.getDouble("foo").should.equal(13.4)
      content.keys().should.equal(setOf("foo"))
    }

    it("should read booleans") {
      val content = TomlReader.read("foo=false".byteInputStream())
      content.getBoolean("foo").should.equal(false)
      content.getBoolean("blah").should.equal(null)
      content.keys().should.equal(setOf("foo"))
    }

    it("should read invalid numbers") {
      val content = TomlReader.read("foo=13.4ww".byteInputStream())
      content.getDouble("foo").should.equal(null)
      content.keys().should.equal(setOf("foo"))
    }

    it("should read string values with double quotes in them") {
      val content = TomlReader.read("foo=\"\\\"<--double quote\"".byteInputStream())
      content.get("foo").should.equal("\"<--double quote")
      content.keys().should.equal(setOf("foo"))
    }

    it("should read string values with double quotes in them, when the string is presented with single quotes") {
      val content = TomlReader.read("foo='\"<--double quote'".byteInputStream())
      content.get("foo").should.equal("\"<--double quote")
      content.keys().should.equal(setOf("foo"))
    }

    it("should read arrays") {
      val content = TomlReader.read("foo = [\"yo\",\"bar\"]".byteInputStream())
      Arrays.equals(content.getArray("foo"), arrayOf("yo", "bar")).should.equal(true)
      content.keys().should.equal(setOf("foo"))
    }

    it("should be ok with comments inline") {
      val content = TomlReader.read("foo=\"bar\" # Some comment".byteInputStream())
      content.get("foo").should.equal("bar")
      content.keys().should.equal(setOf("foo"))
    }
  }
})
