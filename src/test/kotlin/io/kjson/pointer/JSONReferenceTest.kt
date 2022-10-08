/*
 * @(#) JSONReferenceTest.kt
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSONArray
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.pointer.test.SampleJSON.testObject
import io.kjson.pointer.test.SampleJSON.testString

class JSONReferenceTest {

    @Test fun `should create JSONReference with given pointer`() {
        val testReference = JSONPointer.root ref testString
        assertSame(testString, testReference.base)
        expect(JSONPointer.root) { testReference.pointer }
        assertTrue { testReference.valid }
        assertSame(testString, testReference.value)
        expect("\"test1\"") { testReference.toString() }
    }

    @Test fun `should create JSONReference with default pointer`() {
        val testReference = JSONReference(testString)
        assertSame(testString, testReference.base)
        expect(JSONPointer.root) { testReference.pointer }
        assertTrue { testReference.valid }
        assertSame(testString, testReference.value)
        expect("\"test1\"") { testReference.toString() }
    }

    @Test fun `should create JSONReference with non-root pointer`() {
        val testReference = JSONPointer("/field1") ref testObject
        assertSame(testObject, testReference.base)
        expect(JSONPointer.root.child("field1")) { testReference.pointer }
        assertTrue { testReference.valid }
        expect(JSONInt(123)) { testReference.value }
        expect("123") { testReference.toString() }
    }

    @Test fun `should create JSONReference with invalid pointer`() {
        val testReference = JSONPointer("/field99") ref testObject
        assertSame(testObject, testReference.base)
        expect(JSONPointer.root.child("field99")) { testReference.pointer }
        assertFalse { testReference.valid }
        assertNull(testReference.value)
        expect("invalid") { testReference.toString() }
    }

    @Test fun `should navigate to child by name`() {
        val testReference1 = JSONReference(testObject)
        assertSame(testObject, testReference1.base)
        expect(JSONPointer.root) { testReference1.pointer }
        assertTrue { testReference1.valid }
        assertTrue { testReference1.value is JSONObject }
        expect("""{"field1":123,"field2":["abc","def"]}""") { testReference1.toString() }
        val testReference2 = testReference1.child("field1")
        assertSame(testObject, testReference2.base)
        expect(JSONPointer.root.child("field1")) { testReference2.pointer }
        assertTrue { testReference2.valid }
        expect(JSONInt(123)) { testReference2.value }
        expect("123") { testReference2.toString() }
    }

    @Test fun `should navigate to child by index`() {
        val testReference1 = JSONReference(testObject)
        assertSame(testObject, testReference1.base)
        expect(JSONPointer.root) { testReference1.pointer }
        assertTrue { testReference1.valid }
        assertTrue { testReference1.value is JSONObject }
        expect("""{"field1":123,"field2":["abc","def"]}""") { testReference1.toString() }
        val testReference2 = testReference1.child("field2")
        assertSame(testObject, testReference2.base)
        expect(JSONPointer.root.child("field2")) { testReference2.pointer }
        assertTrue { testReference2.valid }
        assertTrue { testReference2.value is JSONArray }
        val testReference3 = testReference2.child(1)
        assertSame(testObject, testReference3.base)
        expect(JSONPointer.root.child("field2").child(1)) { testReference3.pointer }
        assertTrue { testReference3.valid }
        expect(JSONString("def")) { testReference3.value }
        expect("\"def\"") { testReference3.toString() }
    }

    @Test fun `should navigate back to parent`() {
        val testReference1 = JSONReference(testObject)
        assertSame(testObject, testReference1.base)
        expect(JSONPointer.root) { testReference1.pointer }
        assertTrue { testReference1.valid }
        assertTrue { testReference1.value is JSONObject }
        expect("""{"field1":123,"field2":["abc","def"]}""") { testReference1.toString() }
        val testReference2 = testReference1.child("field1")
        assertSame(testObject, testReference2.base)
        expect(JSONPointer.root.child("field1")) { testReference2.pointer }
        assertTrue { testReference2.valid }
        expect(JSONInt(123)) { testReference2.value }
        expect("123") { testReference2.toString() }
        val testReference3 = testReference2.parent()
        assertSame(testObject, testReference3.base)
        expect(JSONPointer.root) { testReference3.pointer }
        assertTrue { testReference3.valid }
        assertTrue { testReference3.value is JSONObject }
        expect("""{"field1":123,"field2":["abc","def"]}""") { testReference3.toString() }
    }

    @Test fun `should respond correctly to hasChild`() {
        val testReference1 = JSONReference(testObject)
        assertFalse { testReference1.hasChild("field99") }
        assertTrue { testReference1.hasChild("field2") }
        val testReference2 = testReference1.child("field2")
        assertFalse { testReference2.hasChild("field99") }
        assertFalse { testReference2.hasChild(2) }
        assertTrue { testReference2.hasChild(0) }
    }

    @Test fun `should regard null base as not valid`() {
        val testReference1 = JSONReference(null)
        assertFalse { testReference1.valid }
        val testReference2 = JSONPointer.root ref null
        assertFalse { testReference2.valid }
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
        expect(JSONPointer("/aaa") ref obj1) { JSONReference(obj1).locateChild(obj2) }
        expect(JSONPointer("/aaa/bbb") ref obj1) { JSONReference(obj1).locateChild(str1) }
        expect(JSONPointer("/bbb") ref obj2) { JSONReference(obj2).locateChild(str1) }
        expect(JSONPointer("/aaa/ccc/1") ref obj1) { JSONReference(obj1).locateChild(int2) }
    }

    @Test fun `should regard equal references as equal`() {
        val testReference1 = JSONPointer("/field2/1") ref testObject
        val testReference2 = JSONReference(testObject).child("field2").child(1)
        assertEquals(testReference1, testReference2)
    }

    @Test fun `should regard references with different paths as not equal`() {
        val testReference1 = JSONPointer("/field2/1") ref testObject
        val testReference2 = JSONReference(testObject).child("field2")
        assertNotEquals(testReference1, testReference2)
    }

    @Test fun `should regard references with different bases as not equal`() {
        val testReference1 = JSONPointer("/field2") ref testObject
        val testReference2 = JSONPointer("/field2") ref JSONObject.from(testObject)
        assertNotEquals(testReference1, testReference2)
    }

}
