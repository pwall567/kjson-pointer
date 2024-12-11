/*
 * @(#) FindTest.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2024 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kjson.pointer

import kotlin.test.Test

import java.io.File

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

import io.kjson.JSON
import io.kjson.JSONArray
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONTypeException
import io.kjson.JSONValue
import io.kjson.pointer.test.SampleJSON.testArray
import io.kjson.pointer.test.SampleJSON.testMixedArray
import io.kjson.pointer.test.SampleJSON.testNestedObject
import io.kjson.pointer.test.SampleJSON.testObject

class FindTest {

    private val document: JSONValue = JSON.parseObject(File("src/test/resources/json-pointer-example.json").readText())
    private val string1 = JSONString("bar")
    private val string2 = JSONString("baz")
    private val array1 = JSONArray.of(string1, string2)

    @Test fun `should give results shown in example in specification`() {
        JSONPointer("").find(document) shouldBeSameInstance document
        JSONPointer("/foo").find(document) shouldBe array1
        JSONPointer("/foo/0").find(document) shouldBe string1
        JSONPointer("/").find(document) shouldBe JSONInt.ZERO
        JSONPointer("/a~1b").find(document) shouldBe JSONInt(1)
        JSONPointer("/c%d").find(document) shouldBe JSONInt(2)
        JSONPointer("/e^f").find(document) shouldBe JSONInt(3)
        JSONPointer("/g|h").find(document) shouldBe JSONInt(4)
        JSONPointer("/i\\j").find(document) shouldBe JSONInt(5)
        JSONPointer("/k\"l").find(document) shouldBe JSONInt(6)
        JSONPointer("/ ").find(document) shouldBe JSONInt(7)
        JSONPointer("/m~0n").find(document) shouldBe JSONInt(8)
    }

    @Test fun `should test whether pointer exists or not`() {
        JSONPointer("/foo") existsIn  document shouldBe true
        JSONPointer("/foo/0") existsIn  document shouldBe true
        JSONPointer("/foo/1") existsIn  document shouldBe true
        JSONPointer("/foo/2") existsIn  document shouldBe false
        JSONPointer("/fool") existsIn  document shouldBe false
    }

    @Test fun `should handle null object properties correctly`() {
        val obj = JSONObject.Builder {
            add("nonNullValue", JSONString("OK"))
            add("nullValue", null)
        }.build()
        JSONPointer("/nonNullValue").find(obj) shouldBe JSONString("OK")
        JSONPointer("/nonNullValue") existsIn obj shouldBe true
        JSONPointer("/nullValue").find(obj) shouldBe null
        JSONPointer("/nullValue") existsIn obj shouldBe true
    }

    @Test fun `should handle null array items correctly`() {
        val array = JSONArray.Builder {
            add(JSONString("OK"))
            add(null)
        }.build()
        JSONPointer("/0").find(array) shouldBe JSONString("OK")
        JSONPointer("/0") existsIn array shouldBe true
        JSONPointer("/1").find(array) shouldBe null
        JSONPointer("/1") existsIn array shouldBe true
    }

    @Test fun `should give correct error message on bad reference`() {
        shouldThrow<JSONPointerException>("Can't locate JSON property \"wrong\"") {
            JSONPointer("/wrong/0").find(document)
        }.let {
            it.text shouldBe "Can't locate JSON property \"wrong\""
            it.pointer shouldBeSameInstance JSONPointer.root
            it.cause shouldBe null
        }
        val innerDocument = JSONObject.build {
            add("data", document)
        }
        shouldThrow<JSONPointerException>("Can't locate JSON property \"wrong\", at /data") {
            JSONPointer("/data/wrong/0").find(innerDocument)
        }.let {
            it.text shouldBe "Can't locate JSON property \"wrong\""
            it.pointer shouldBe JSONPointer("/data")
            it.cause shouldBe null
        }
    }

    @Test fun `should navigate numeric index`() {
        JSONPointer("/0").find(testArray) shouldBe JSONString("A")
        JSONPointer("/1").find(testArray) shouldBe JSONString("B")
        JSONPointer("/10").find(testArray) shouldBe JSONString("K")
        JSONPointer("/15").find(testArray) shouldBe JSONString("P")
    }

    @Test fun `should reject invalid numeric index`() {
        shouldThrow<JSONPointerException>("Illegal array index \"01\" in JSON Pointer") {
            JSONPointer("/01").find(testArray)
        }.let {
            it.text shouldBe "Illegal array index \"01\" in JSON Pointer"
            it.pointer shouldBeSameInstance JSONPointer.root
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal array index \"\" in JSON Pointer") {
            JSONPointer("/").find(testArray)
        }.let {
            it.text shouldBe "Illegal array index \"\" in JSON Pointer"
            it.pointer shouldBeSameInstance JSONPointer.root
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal array index \"A\" in JSON Pointer") {
            JSONPointer("/A").find(testArray)
        }.let {
            it.text shouldBe "Illegal array index \"A\" in JSON Pointer"
            it.pointer shouldBeSameInstance JSONPointer.root
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal array index \"999999999\" in JSON Pointer") {
            JSONPointer("/999999999").find(testArray)
        }.let {
            it.text shouldBe "Illegal array index \"999999999\" in JSON Pointer"
            it.pointer shouldBeSameInstance JSONPointer.root
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal array index \"-1\" in JSON Pointer") {
            JSONPointer("/-1").find(testArray)
        }.let {
            it.text shouldBe "Illegal array index \"-1\" in JSON Pointer"
            it.pointer shouldBeSameInstance JSONPointer.root
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Array index 99 out of range in JSON Pointer") {
            JSONPointer("/99").find(testArray)
        }.let {
            it.text shouldBe "Array index 99 out of range in JSON Pointer"
            it.pointer shouldBeSameInstance JSONPointer.root
            it.cause shouldBe null
        }
        val innerArray = JSONObject.build {
            add("data", testArray)
        }
        shouldThrow<JSONPointerException>("Illegal array index \"01\" in JSON Pointer, at /data") {
            JSONPointer("/data/01").find(innerArray)
        }.let {
            it.text shouldBe "Illegal array index \"01\" in JSON Pointer"
            it.pointer shouldBe JSONPointer("/data")
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal array index \"\" in JSON Pointer, at /data") {
            JSONPointer("/data/").find(innerArray)
        }.let {
            it.text shouldBe "Illegal array index \"\" in JSON Pointer"
            it.pointer shouldBe JSONPointer("/data")
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal array index \"A\" in JSON Pointer, at /data") {
            JSONPointer("/data/A").find(innerArray)
        }.let {
            it.text shouldBe "Illegal array index \"A\" in JSON Pointer"
            it.pointer shouldBe JSONPointer("/data")
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal array index \"999999999\" in JSON Pointer, at /data") {
            JSONPointer("/data/999999999").find(innerArray)
        }.let {
            it.text shouldBe "Illegal array index \"999999999\" in JSON Pointer"
            it.pointer shouldBe JSONPointer("/data")
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal array index \"-1\" in JSON Pointer, at /data") {
            JSONPointer("/data/-1").find(innerArray)
        }.let {
            it.text shouldBe "Illegal array index \"-1\" in JSON Pointer"
            it.pointer shouldBe JSONPointer("/data")
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Array index 99 out of range in JSON Pointer, at /data") {
            JSONPointer("/data/99").find(innerArray)
        }.let {
            it.text shouldBe "Array index 99 out of range in JSON Pointer"
            it.pointer shouldBe JSONPointer("/data")
            it.cause shouldBe null
        }
    }

    @Test fun `should return false for exists with null base`() {
        JSONPointer.root existsIn null shouldBe false
    }

    @Test fun `should locate child in nested object`() {
        val str1 = JSONString("xyz")
        val int1 = JSONInt(123)
        val int2 = JSONInt(456)
        val array1 = JSONArray.of(int1, int2)
        val obj2 = JSONObject.Builder {
            add("bbb", str1)
            add("ccc", array1)
        }.build()
        val obj1 = JSONObject.Builder {
            add("aaa", obj2)
        }.build()
        JSONPointer.root.locateChild(obj1, obj2) shouldBe JSONPointer("/aaa")
        JSONPointer.root.locateChild(obj1, str1) shouldBe JSONPointer("/aaa/bbb")
        JSONPointer.root.locateChild(obj2, str1) shouldBe JSONPointer("/bbb")
        JSONPointer.root.locateChild(obj1, int2) shouldBe JSONPointer("/aaa/ccc/1")
    }

    @Test fun `should find object using findObject`() {
        val obj = JSONPointer("/field2").findObject(testNestedObject)
        obj["aaa"] shouldBe JSONInt(99)
    }

    @Test fun `should throw exception when findObject target is not an object`() {
        shouldThrow<JSONTypeException>("Node not correct type (JSONObject), was 123, at /field1") {
            JSONPointer("/field1").findObject(testNestedObject)
        }
    }

    @Test fun `should find array using findArray`() {
        val obj = JSONPointer("/field2").findArray(testObject)
        obj[1] shouldBe JSONString("def")
    }

    @Test fun `should throw exception when findArray target is not an array`() {
        shouldThrow<JSONTypeException>("Node not correct type (JSONArray), was 123, at /field1") {
            JSONPointer("/field1").findArray(testObject)
        }
    }

    @Test fun `should find object using findOrNull`() {
        val obj = JSONPointer("/field2").findOrNull(testObject)
        obj.shouldBeType<JSONArray>()
        obj[1] shouldBe JSONString("def")
        JSONPointer("/nothing").findOrNull(testObject) shouldBe null
    }

    @Test fun `should throw exception when intermediate node is null on find`() {
        shouldThrow<JSONPointerException>("Intermediate node is null, at /4") {
            JSONPointer("/4/name").find(testMixedArray)
        }.let {
            it.text shouldBe "Intermediate node is null"
            it.pointer shouldBe JSONPointer("/4")
        }
    }

}
