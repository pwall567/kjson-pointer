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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect

import java.io.File

import io.kjson.JSON
import io.kjson.JSONArray
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONTypeException
import io.kjson.JSONValue
import io.kjson.pointer.test.SampleJSON.testArray
import io.kjson.pointer.test.SampleJSON.testNestedObject
import io.kjson.pointer.test.SampleJSON.testObject

class FindTest {

    private val document: JSONValue = JSON.parseObject(File("src/test/resources/json-pointer-example.json").readText())
    private val string1 = JSONString("bar")
    private val string2 = JSONString("baz")
    private val array1 = JSONArray.of(string1, string2)

    @Test fun `should give results shown in example in specification`() {
        assertSame(document, JSONPointer("").find(document))
        expect(array1) { JSONPointer("/foo").find(document) }
        expect(string1) { JSONPointer("/foo/0").find(document) }
        expect(JSONInt.ZERO) { JSONPointer("/").find(document) }
        expect(JSONInt(1)) { JSONPointer("/a~1b").find(document) }
        expect(JSONInt(2)) { JSONPointer("/c%d").find(document) }
        expect(JSONInt(3)) { JSONPointer("/e^f").find(document) }
        expect(JSONInt(4)) { JSONPointer("/g|h").find(document) }
        expect(JSONInt(5)) { JSONPointer("/i\\j").find(document) }
        expect(JSONInt(6)) { JSONPointer("/k\"l").find(document) }
        expect(JSONInt(7)) { JSONPointer("/ ").find(document) }
        expect(JSONInt(8)) { JSONPointer("/m~0n").find(document) }
    }

    @Test fun `should test whether pointer exists or not`() {
        assertTrue(JSONPointer("/foo") existsIn  document)
        assertTrue(JSONPointer("/foo/0") existsIn  document)
        assertTrue(JSONPointer("/foo/1") existsIn  document)
        assertFalse(JSONPointer("/foo/2") existsIn  document)
        assertFalse(JSONPointer("/fool") existsIn  document)
    }

    @Test fun `should handle null object properties correctly`() {
        val obj = JSONObject.Builder {
            add("nonNullValue", JSONString("OK"))
            add("nullValue", null)
        }.build()
        expect(JSONString("OK")) { JSONPointer("/nonNullValue").find(obj) }
        assertTrue { JSONPointer("/nonNullValue") existsIn obj }
        assertNull(JSONPointer("/nullValue").find(obj))
        assertTrue { JSONPointer("/nullValue") existsIn obj }
    }

    @Test fun `should handle null array items correctly`() {
        val array = JSONArray.Builder {
            add(JSONString("OK"))
            add(null)
        }.build()
        expect(JSONString("OK")) { JSONPointer("/0").find(array) }
        assertTrue(JSONPointer("/0") existsIn array)
        assertNull(JSONPointer("/1").find(array))
        assertTrue(JSONPointer("/1") existsIn array)
    }

    @Test fun `should give correct error message on bad reference`() {
        assertFailsWith<JSONPointerException> { JSONPointer("/wrong/0").find(document) }.let {
            expect("Can't locate JSON property \"wrong\"") { it.message }
            expect("Can't locate JSON property \"wrong\"") { it.text }
            assertSame(JSONPointer.root, it.pointer)
            assertNull(it.cause)
        }
        val innerDocument = JSONObject.build {
            add("data", document)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/data/wrong/0").find(innerDocument) }.let {
            expect("Can't locate JSON property \"wrong\", at /data") { it.message }
            expect("Can't locate JSON property \"wrong\"") { it.text }
            expect(JSONPointer("/data")) { it.pointer }
            assertNull(it.cause)
        }
    }

    @Test fun `should navigate numeric index`() {
        expect(JSONString("A")) { JSONPointer("/0").find(testArray) }
        expect(JSONString("B")) { JSONPointer("/1").find(testArray) }
        expect(JSONString("K")) { JSONPointer("/10").find(testArray) }
        expect(JSONString("P")) { JSONPointer("/15").find(testArray) }
    }

    @Test fun `should reject invalid numeric index`() {
        assertFailsWith<JSONPointerException> { JSONPointer("/01").find(testArray) }.let {
            expect("Illegal array index \"01\" in JSON Pointer") { it.message }
            expect("Illegal array index \"01\" in JSON Pointer") { it.text }
            assertSame(JSONPointer.root, it.pointer)
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/").find(testArray) }.let {
            expect("Illegal array index \"\" in JSON Pointer") { it.message }
            expect("Illegal array index \"\" in JSON Pointer") { it.text }
            assertSame(JSONPointer.root, it.pointer)
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/A").find(testArray) }.let {
            expect("Illegal array index \"A\" in JSON Pointer") { it.message }
            expect("Illegal array index \"A\" in JSON Pointer") { it.text }
            assertSame(JSONPointer.root, it.pointer)
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/999999999").find(testArray) }.let {
            expect("Illegal array index \"999999999\" in JSON Pointer") { it.message }
            expect("Illegal array index \"999999999\" in JSON Pointer") { it.text }
            assertSame(JSONPointer.root, it.pointer)
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/-1").find(testArray) }.let {
            expect("Illegal array index \"-1\" in JSON Pointer") { it.message }
            expect("Illegal array index \"-1\" in JSON Pointer") { it.text }
            assertSame(JSONPointer.root, it.pointer)
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/99").find(testArray) }.let {
            expect("Array index 99 out of range in JSON Pointer") { it.message }
            expect("Array index 99 out of range in JSON Pointer") { it.text }
            assertSame(JSONPointer.root, it.pointer)
            assertNull(it.cause)
        }
        val innerArray = JSONObject.build {
            add("data", testArray)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/data/01").find(innerArray) }.let {
            expect("Illegal array index \"01\" in JSON Pointer, at /data") { it.message }
            expect("Illegal array index \"01\" in JSON Pointer") { it.text }
            expect(JSONPointer("/data")) { it.pointer }
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/data/").find(innerArray) }.let {
            expect("Illegal array index \"\" in JSON Pointer, at /data") { it.message }
            expect("Illegal array index \"\" in JSON Pointer") { it.text }
            expect(JSONPointer("/data")) { it.pointer }
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/data/A").find(innerArray) }.let {
            expect("Illegal array index \"A\" in JSON Pointer, at /data") { it.message }
            expect("Illegal array index \"A\" in JSON Pointer") { it.text }
            expect(JSONPointer("/data")) { it.pointer }
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/data/999999999").find(innerArray) }.let {
            expect("Illegal array index \"999999999\" in JSON Pointer, at /data") { it.message }
            expect("Illegal array index \"999999999\" in JSON Pointer") { it.text }
            expect(JSONPointer("/data")) { it.pointer }
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/data/-1").find(innerArray) }.let {
            expect("Illegal array index \"-1\" in JSON Pointer, at /data") { it.message }
            expect("Illegal array index \"-1\" in JSON Pointer") { it.text }
            expect(JSONPointer("/data")) { it.pointer }
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/data/99").find(innerArray) }.let {
            expect("Array index 99 out of range in JSON Pointer, at /data") { it.message }
            expect("Array index 99 out of range in JSON Pointer") { it.text }
            expect(JSONPointer("/data")) { it.pointer }
            assertNull(it.cause)
        }
    }

    @Test fun `should return false for exists with null base`() {
        assertFalse(JSONPointer.root existsIn null)
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
        expect(JSONPointer("/aaa")) { JSONPointer.root.locateChild(obj1, obj2) }
        expect(JSONPointer("/aaa/bbb")) { JSONPointer.root.locateChild(obj1, str1) }
        expect(JSONPointer("/bbb")) { JSONPointer.root.locateChild(obj2, str1) }
        expect(JSONPointer("/aaa/ccc/1")) { JSONPointer.root.locateChild(obj1, int2) }
    }

    @Test fun `should find object using findObject`() {
        val obj = JSONPointer("/field2").findObject(testNestedObject)
        expect(JSONInt(99)) { obj["aaa"] }
    }

    @Test fun `should throw exception when findObject target is not an object`() {
        assertFailsWith<JSONTypeException> { JSONPointer("/field1").findObject(testNestedObject) }.let {
            expect("Node not correct type (JSONObject), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should find array using findArray`() {
        val obj = JSONPointer("/field2").findArray(testObject)
        expect(JSONString("def")) { obj[1] }
    }

    @Test fun `should throw exception when findArray target is not an array`() {
        assertFailsWith<JSONTypeException> { JSONPointer("/field1").findArray(testObject) }.let {
            expect("Node not correct type (JSONArray), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should find object using findOrNull`() {
        val obj = JSONPointer("/field2").findOrNull(testObject)
        assertTrue(obj is JSONArray)
        expect(JSONString("def")) { obj[1] }
        assertNull(JSONPointer("/nothing").findOrNull(testObject))
    }

}
