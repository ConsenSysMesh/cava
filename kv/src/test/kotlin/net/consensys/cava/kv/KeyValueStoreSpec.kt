/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.kv

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.winterbe.expekt.should
import kotlinx.coroutines.experimental.runBlocking
import net.consensys.cava.bytes.Bytes
import net.consensys.cava.kv.Vars.foo
import net.consensys.cava.kv.Vars.foobar
import org.iq80.leveldb.DBException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.nio.file.Files

object Vars {
  val foo = Bytes.wrap("foo".toByteArray())!!
  val foobar = Bytes.wrap("foobar".toByteArray())!!
}

object KeyValueStoreSpec : Spek({
  val backingMap = mutableMapOf<Bytes, Bytes>()
  val kv = MapKeyValueStore(backingMap)

  describe("a map-backed key value store") {

    it("should allow to store values") {
      runBlocking {
        kv.put(foo, foo)
        backingMap.get(foo).should.equal(foo)
      }
    }

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should return an empty optional when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }
  }
})

object MapDBKeyValueStoreSpec : Spek({
  val testDir = Files.createTempDirectory("data")
  val kv = MapDBKeyValueStore(testDir.resolve("data.db"))

  describe("a MapDB-backed key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should return an empty optional when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = MapDBKeyValueStore(testDir.resolve("data2.db"))
      kv2.close()
      runBlocking {
        var caught = false
        try {
          kv2.put(foobar, foo)
        } catch (e: IllegalAccessError) {
          caught = true
        }
        caught.should.be.`true`
      }
    }

    afterGroup {
      kv.close()
      MoreFiles.deleteRecursively(testDir, RecursiveDeleteOption.ALLOW_INSECURE)
    }
  }
})

object LevelDBKeyValueStoreSpec : Spek({
  val path = Files.createTempDirectory("leveldb")
  val kv = LevelDBKeyValueStore(path)
  afterGroup {
    kv.close()
    MoreFiles.deleteRecursively(path, RecursiveDeleteOption.ALLOW_INSECURE)
  }
  describe("a levelDB-backed key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should return an empty optional when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = LevelDBKeyValueStore(path.resolve("subdb"))
      kv2.close()
      runBlocking {
        var caught = false
        try {
          kv2.put(foobar, foo)
        } catch (e: DBException) {
          caught = true
        }
        caught.should.be.`true`
      }
    }
  }
})
