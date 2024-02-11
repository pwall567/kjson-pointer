/*
 * @(#) JSONRefTest.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2022, 2024 Peter Wall
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
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSON
import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONInt
import io.kjson.JSONNumber
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONTypeException
import io.kjson.JSONValue
import io.kjson.pointer.test.SampleJSON.testObject
import io.kjson.pointer.test.SampleJSON.testString

class JSONRefTest {

    @Test fun `should create JSONRef`() {
        val ref1 = JSONRef(testString)
        assertSame(testString, ref1.base)
        expect(JSONPointer.root) { ref1.pointer }
        assertSame(testString, ref1.node)
    }

    @Test fun `should navigate back to parent`() {
        val refObject = JSONRef.of<JSONInt>(testObject, JSONPointer("/field1"))
        val parent = refObject.parent<JSONObject>()
        expect(JSONBoolean.TRUE) { parent.node["field3"] }
        expect(JSONPointer.root) { parent.pointer }
    }

    @Test fun `should throw exception when trying to navigate to parent of root pointer`() {
        val refObject = JSONRef(testObject)
        assertFailsWith<JSONPointerException> { refObject.parent<JSONObject>() }.let {
            expect("Can't get parent of root JSON Pointer") { it.message }
        }
    }

    @Test fun `should throw exception when parent is not correct type`() {
        val refChild = JSONRef(testObject).child<JSONInt>("field1")
        assertFailsWith<JSONTypeException> { refChild.parent<JSONArray>() }.let {
            expect("Parent") { it.nodeName }
            expect("JSONArray") { it.target }
            expect(testObject) { it.value }
            expect(JSONPointer.root) { it.key }
            expect("Parent not correct type (JSONArray), was { ... }") { it.message }
        }
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
        expect(JSONPointer("/aaa")) { JSONRef(obj1).locateChild(obj2)?.pointer }
        expect(JSONPointer("/aaa/bbb")) { JSONRef(obj1).locateChild(str1)?.pointer }
        expect(JSONPointer("/bbb")) { JSONRef(obj2).locateChild(str1)?.pointer }
        expect(JSONPointer("/aaa/ccc/1")) { JSONRef(obj1).locateChild(int2)?.pointer }
    }

    @Test fun `should convert to ref of correct type`() {
        val str = JSONString("mango")
        val ref = JSONRef<JSONValue>(str)
        expect("mango") { ref.asRef<JSONString>().node.value }
    }

    @Test fun `should throw exception converting to ref of incorrect type`() {
        val str = JSONString("mango")
        val ref = JSONRef<JSONValue>(str)
        assertFailsWith<JSONTypeException> { ref.asRef<JSONObject>() }.let {
            expect("Node") { it.nodeName }
            expect("JSONObject") { it.target }
            expect(JSONString("mango")) { it.value }
            expect(JSONPointer.root) { it.key }
            expect("Node not correct type (JSONObject), was \"mango\"") { it.message }
        }
    }

    @Test fun `should convert to ref of nullable type`() {
        val str = JSONString("mango")
        val ref = JSONRef<JSONValue>(str)
        expect("mango") { ref.asRef<JSONString?>().node?.value }
    }

    @Test fun `should test for specified type`() {
        val number = JSONInt(42)
        val ref = JSONRef<JSONValue>(number)
        assertTrue(ref.isRef<JSONNumber>())
        assertTrue(ref.isRef<JSONInt>())
        assertTrue(ref.isRef<JSONValue>())
        assertFalse(ref.isRef<JSONString>())
    }

    @Test fun `should test for specified type including nullable`() {
        val number = JSONInt(42)
        val ref = JSONRef<JSONValue>(number)
        assertTrue(ref.isRef<JSONNumber?>())
        assertTrue(ref.isRef<JSONInt?>())
        assertTrue(ref.isRef<JSONValue?>())
        assertFalse(ref.isRef<JSONString?>())
    }

    @Test fun `should test for specified type with null value`() {
        val ref = JSONRef<JSONValue?>(null)
        assertTrue(ref.isRef<JSONNumber?>())
        assertTrue(ref.isRef<JSONInt?>())
        assertTrue(ref.isRef<JSONValue?>())
        assertTrue(ref.isRef<JSONString?>())
    }

    @Test fun `should throw exception building JSONRef using of with wrong type`() {
        assertFailsWith<JSONTypeException> { JSONRef.of<JSONString>(testObject, JSONPointer("/field1")) }.let {
            expect("Node") { it.nodeName }
            expect("JSONString") { it.target }
            expect(JSONInt(123)) { it.value }
            expect(JSONPointer("/field1")) { it.key }
            expect("Node not correct type (JSONString), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should build JSONRef using of`() {
        val ref1 = JSONRef.of<JSONInt>(testObject, JSONPointer("/field1"))
        expect(123) { ref1.node.value }
        val ref2 = JSONRef.of<JSONString>(testObject, JSONPointer("/field2/0"))
        expect("abc") { ref2.node.value }
        val ref3 = ref2.parent<JSONArray>()
        assertSame(testObject["field2"], ref3.node)
        val ref4 = ref3.parent<JSONObject>()
        assertSame(testObject, ref4.node)
    }

    @Test fun `should throw exception on null node when building JSONRef using of`() {
        val json = JSON.parseObject("""{"a":{"b":null}}""")
        assertFailsWith<JSONPointerException> { JSONRef.of<JSONInt>(json, JSONPointer("/a/b/c")) }.let {
            expect("Node is null, at /a/b") { it.message }
        }
    }

}
