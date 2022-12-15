/*
 * @(#) JSONPointerTest.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2021, 2022 Peter Wall
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
import io.kjson.JSONIncorrectTypeException
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer.Companion.encodeJSONPointer
import io.kjson.pointer.JSONPointer.Companion.decodeJSONPointer
import io.kjson.pointer.test.SampleJSON.testArray
import io.kjson.pointer.test.SampleJSON.testNestedObject
import io.kjson.pointer.test.SampleJSON.testObject

class JSONPointerTest {

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

    @Test fun `should give results shown in example in specification using companion function`() {
        assertSame(document, JSONPointer.find("", document))
        expect(array1) { JSONPointer.find("/foo", document) }
        expect(string1) { JSONPointer.find("/foo/0", document) }
        expect(JSONInt.ZERO) { JSONPointer.find("/", document) }
        expect(JSONInt(1)) { JSONPointer.find("/a~1b", document) }
        expect(JSONInt(2)) { JSONPointer.find("/c%d", document) }
        expect(JSONInt(3)) { JSONPointer.find("/e^f", document) }
        expect(JSONInt(4)) { JSONPointer.find("/g|h", document) }
        expect(JSONInt(5)) { JSONPointer.find("/i\\j", document) }
        expect(JSONInt(6)) { JSONPointer.find("/k\"l", document) }
        expect(JSONInt(7)) { JSONPointer.find("/ ", document) }
        expect(JSONInt(8)) { JSONPointer.find("/m~0n", document) }
    }

    @Test fun `should escape correctly on toString`() {
        expect("") { JSONPointer("").toString() }
        expect("/foo") { JSONPointer("/foo").toString() }
        expect("/foo/0") { JSONPointer("/foo/0").toString() }
        expect("/") { JSONPointer("/").toString() }
        expect("/a~1b") { JSONPointer("/a~1b").toString() }
        expect("/c%d") { JSONPointer("/c%d").toString() }
        expect("/e^f") { JSONPointer("/e^f").toString() }
        expect("/g|h") { JSONPointer("/g|h").toString() }
        expect("/i\\j") { JSONPointer("/i\\j").toString() }
        expect("/ ") { JSONPointer("/ ").toString() }
        expect("/m~0n") { JSONPointer("/m~0n").toString() }
    }

    @Test fun `should create correct URI fragment`() {
        expect("") { JSONPointer("").toURIFragment() }
        expect("/foo") { JSONPointer("/foo").toURIFragment() }
        expect("/foo/0") { JSONPointer("/foo/0").toURIFragment() }
        expect("/") { JSONPointer("/").toURIFragment() }
        expect("/a~1b") { JSONPointer("/a~1b").toURIFragment() }
        expect("/c%25d") { JSONPointer("/c%d").toURIFragment() }
        expect("/e%5Ef") { JSONPointer("/e^f").toURIFragment() }
        expect("/g%7Ch") { JSONPointer("/g|h").toURIFragment() }
        expect("/i%5Cj") { JSONPointer("/i\\j").toURIFragment() }
        expect("/k%22l") { JSONPointer("/k\"l").toURIFragment() }
        expect("/%20") { JSONPointer("/ ").toURIFragment() }
        expect("/m~0n") { JSONPointer("/m~0n").toURIFragment() }
        expect("/o%2Ap") { JSONPointer("/o*p").toURIFragment() }
        expect("/q%2Br") { JSONPointer("/q+r").toURIFragment() }
    }

    @Test fun `should correctly decode URI fragment`() {
        expect(JSONPointer("")) { JSONPointer.fromURIFragment("") }
        expect(JSONPointer("/foo")) { JSONPointer.fromURIFragment("/foo") }
        expect(JSONPointer("/foo/0")) { JSONPointer.fromURIFragment("/foo/0") }
        expect(JSONPointer("/")) { JSONPointer.fromURIFragment("/") }
        expect(JSONPointer("/a~1b")) { JSONPointer.fromURIFragment("/a~1b") }
        expect(JSONPointer("/c%d")) { JSONPointer.fromURIFragment("/c%25d") }
        expect(JSONPointer("/e^f")) { JSONPointer.fromURIFragment("/e%5Ef") }
        expect(JSONPointer("/g|h")) { JSONPointer.fromURIFragment("/g%7Ch") }
        expect(JSONPointer("/i\\j")) { JSONPointer.fromURIFragment("/i%5Cj") }
        expect(JSONPointer("/k\"l")) { JSONPointer.fromURIFragment("/k%22l") }
        expect(JSONPointer("/ ")) { JSONPointer.fromURIFragment("/%20") }
        expect(JSONPointer("/m~0n")) { JSONPointer.fromURIFragment("/m~0n") }
        expect(JSONPointer("/o*p")) { JSONPointer.fromURIFragment("/o%2Ap") }
        expect(JSONPointer("/q+r")) { JSONPointer.fromURIFragment("/q%2Br") }
    }

    @Test fun `should test whether pointer exists or not`() {
        assertTrue { JSONPointer("/foo") existsIn  document }
        assertTrue { JSONPointer("/foo/0") existsIn  document }
        assertTrue { JSONPointer("/foo/1") existsIn  document }
        assertFalse { JSONPointer("/foo/2") existsIn  document }
        assertFalse { JSONPointer("/fool") existsIn  document }
    }

    @Test fun `should test whether pointer exists using companion function`() {
        assertTrue { JSONPointer.existsIn("/foo", document) }
        assertTrue { JSONPointer.existsIn("/foo/0", document) }
        assertTrue { JSONPointer.existsIn("/foo/1", document) }
        assertFalse { JSONPointer.existsIn("/foo/2", document) }
        assertFalse { JSONPointer.existsIn("/fool", document) }
    }

    @Test fun `should handle null object properties correctly`() {
        val obj = JSONObject.Builder {
            add("nonNullValue", JSONString("OK"))
            add("nullValue", null)
        }.build()
        expect(JSONString("OK")) { JSONPointer("/nonNullValue").find(obj) }
        expect(JSONString("OK")) { JSONPointer.find("/nonNullValue", obj) }
        assertTrue { JSONPointer("/nonNullValue") existsIn obj }
        assertTrue { JSONPointer.existsIn("/nonNullValue", obj) }
        assertNull(JSONPointer("/nullValue").find(obj))
        assertNull(JSONPointer.find("/nullValue", obj))
        assertTrue { JSONPointer("/nullValue") existsIn obj }
        assertTrue { JSONPointer.existsIn("/nullValue", obj) }
    }

    @Test fun `should handle null array items correctly`() {
        val array = JSONArray.Builder {
            add(JSONString("OK"))
            add(null)
        }.build()
        expect(JSONString("OK")) { JSONPointer("/0").find(array) }
        expect(JSONString("OK")) { JSONPointer.find("/0", array) }
        assertTrue { JSONPointer("/0") existsIn array }
        assertTrue { JSONPointer.existsIn("/0", array) }
        assertNull(JSONPointer("/1").find(array))
        assertNull(JSONPointer.find("/1", array))
        assertTrue { JSONPointer("/1") existsIn array }
        assertTrue { JSONPointer.existsIn("/1", array) }
    }

    @Test fun `should navigate correctly to child`() {
        val basePointer = JSONPointer("")
        assertSame(document, basePointer.find(document))
        val childPointer1 = basePointer.child("foo")
        expect(array1) { childPointer1.find(document) }
        val childPointer2 = childPointer1.child(0)
        expect(string1) { childPointer2.find(document) }
        val childPointer3 = childPointer1.child(1)
        expect(string2) { childPointer3.find(document) }
    }

    @Test fun `should navigate correctly to parent`() {
        val startingPointer = JSONPointer("/foo/1")
        expect(string2) { startingPointer.find(document) }
        val parentPointer1 = startingPointer.parent()
        expect(array1) { parentPointer1.find(document) }
        val parentPointer2 = parentPointer1.parent()
        assertSame(document, parentPointer2.find(document))
    }

    @Test fun `should give correct error message on bad reference`() {
        assertFailsWith<JSONPointerException> { JSONPointer("/wrong/0").find(document) }.let {
            expect("Can't resolve JSON Pointer - \"/wrong\"") { it.message }
        }
    }

    @Test fun `should return valid root pointer`() {
        expect(JSONPointer("")) { JSONPointer.root }
    }

    @Test fun `should navigate numeric index`() {
        expect(JSONString("A")) { JSONPointer("/0").find(testArray) }
        expect(JSONString("B")) { JSONPointer("/1").find(testArray) }
        expect(JSONString("K")) { JSONPointer("/10").find(testArray) }
        expect(JSONString("P")) { JSONPointer("/15").find(testArray) }
    }

    @Test fun `should reject invalid numeric index`() {
        assertFailsWith<JSONPointerException> { JSONPointer("/01").find(testArray) }.let {
            expect("Illegal array index in JSON Pointer - \"/01\"") { it.message }
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/").find(testArray) }.let {
            expect("Illegal array index in JSON Pointer - \"/\"") { it.message }
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/A").find(testArray) }.let {
            expect("Illegal array index in JSON Pointer - \"/A\"") { it.message }
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/999999999").find(testArray) }.let {
            expect("Illegal array index in JSON Pointer - \"/999999999\"") { it.message }
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/-1").find(testArray) }.let {
            expect("Illegal array index in JSON Pointer - \"/-1\"") { it.message }
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/99").find(testArray) }.let {
            expect("Array index out of range in JSON Pointer - \"/99\"") { it.message }
        }
    }

    @Test fun `should get current token`() {
        expect("second") { JSONPointer("/first/second").current }
        expect("first") { JSONPointer("/first/second").parent().current }
        expect("2") { JSONPointer("/first/2").current }
        assertNull(JSONPointer.root.current)
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

    @Test fun `should correctly unescape pointer string`() {
        val array1 = JSONPointer.parseString("/abc/def")
        expect(2) { array1.size }
        expect("abc") { array1[0] }
        expect("def") { array1[1] }
        val array2 = JSONPointer.parseString("/ab~0")
        expect(1) { array2.size }
        expect("ab~") { array2[0] }
        val array3 = JSONPointer.parseString("/ab~1")
        expect(1) { array3.size }
        expect("ab/") { array3[0] }
        val array4 = JSONPointer.parseString("/ab~1~0cd")
        expect(1) { array4.size }
        expect("ab/~cd") { array4[0] }
    }

    @Test fun `should find object using findObject`() {
        val obj = JSONPointer("/field2").findObject(testNestedObject)
        expect(JSONInt(99)) { obj["aaa"] }
    }

    @Test fun `should throw exception when findObject target is not an object`() {
        assertFailsWith<JSONIncorrectTypeException> { JSONPointer("/field1").findObject(testNestedObject) }.let {
            expect("Node not correct type (JSONObject), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should find array using findArray`() {
        val obj = JSONPointer("/field2").findArray(testObject)
        expect(JSONString("def")) { obj[1] }
    }

    @Test fun `should throw exception when findArray target is not an array`() {
        assertFailsWith<JSONIncorrectTypeException> { JSONPointer("/field1").findArray(testObject) }.let {
            expect("Node not correct type (JSONArray), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should find object using findOrNull`() {
        val obj = JSONPointer("/field2").findOrNull(testObject)
        assertTrue(obj is JSONArray)
        expect(JSONString("def")) { obj[1] }
        assertNull(JSONPointer("/nothing").findOrNull(testObject))
    }

    @Test fun `should find object using findOrNull companion object methods`() {
        val obj = JSONPointer.findOrNull("/field2", testObject)
        assertTrue(obj is JSONArray)
        expect(JSONString("def")) { obj[1] }
        assertNull(JSONPointer("/nothing").findOrNull(testObject))
    }

    @Test fun `should map JSON Pointer characters correctly`() {
        val unchanged = "unchanged"
        assertSame(unchanged, unchanged.encodeJSONPointer())
        expect("a~1b") { "a/b".encodeJSONPointer() }
        expect("a~1~0b") { "a/~b".encodeJSONPointer() }
        expect("abc") { "abc".encodeJSONPointer() }
        expect("ab~0") { "ab~".encodeJSONPointer() }
        expect("ab~1") { "ab/".encodeJSONPointer() }
        expect("ab~1~0cd") { "ab/~cd".encodeJSONPointer() }
    }

    @Test fun `should unmap JSON Pointer characters correctly`() {
        val unchanged = "unchanged"
        assertSame(unchanged, unchanged.decodeJSONPointer())
        expect("a/b") { "a~1b".decodeJSONPointer() }
        expect("a/~b") { "a~1~0b".decodeJSONPointer() }
    }

    @Test fun `should fail on incorrect JSON Pointer string`() {
        assertFailsWith<IllegalArgumentException> { "~".decodeJSONPointer() }.let {
            expect("Incomplete escape sequence") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { "~9".decodeJSONPointer() }.let {
            expect("Invalid escape sequence") { it.message }
        }
    }

    @Test fun `should throw exception when trying to navigate to parent of root pointer`() {
        assertFailsWith<JSONPointerException> { JSONPointer.root.parent() }.let {
            expect("Can't get parent of root JSON Pointer") { it.message }
        }
    }

}
