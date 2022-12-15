/*
 * @(#) IndexOpTest.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2022 Peter Wall
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
import kotlin.test.assertTrue
import kotlin.test.expect

import java.math.BigDecimal

import io.kjson.JSON.asInt
import io.kjson.JSON.asString
import io.kjson.JSONArray
import io.kjson.JSONIncorrectTypeException
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.pointer.test.SampleJSON.testObject

class IndexOpTest {

    @Test fun `should perform indexing operation`() {
        expect(123) { testObject[JSONPointer("/field1")].asInt }
        expect("abc") { testObject[JSONPointer("/field2/0")].asString }
    }

    @Test fun `should return null on indexing operation not found`() {
        assertNull(testObject[JSONPointer("/bad")])
    }

    @Test fun `should perform -in- operation`() {
        assertTrue(JSONPointer("/field1") in testObject)
        assertTrue(JSONPointer("/field2/0") in testObject)
        assertFalse(JSONPointer("/field99") in testObject)
        assertFalse(JSONPointer("/field2/2") in testObject)
    }

    @Test fun `should get string from object using pointer`() {
        expect("abc") { testObject.getString(JSONPointer("/field2/0")) }
        expect("def") { testObject.getString(JSONPointer("/field2/1")) }
    }

    @Test fun `should fail on getString from object using pointer when not string`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getString(JSONPointer("/field1")) }.let {
            expect("Node") { it.nodeName }
            expect("String") { it.target }
            expect(JSONPointer("/field1")) { it.key }
            expect(JSONInt(123)) { it.value }
            expect("Node not correct type (String), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should get long from object using pointer`() {
        expect(123) { testObject.getLong(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getLong from object using pointer when not long`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getLong(JSONPointer("/field2/0")) }.let {
            expect("Node") { it.nodeName }
            expect("Long") { it.target }
            expect(JSONPointer("/field2/0")) { it.key }
            expect(JSONString("abc")) { it.value }
            expect("Node not correct type (Long), was \"abc\", at /field2/0") { it.message }
        }
    }

    @Test fun `should get int from object using pointer`() {
        expect(123) { testObject.getInt(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getInt from object using pointer when not int`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getInt(JSONPointer("/field2/1")) }.let {
            expect("Node") { it.nodeName }
            expect("Int") { it.target }
            expect(JSONPointer("/field2/1")) { it.key }
            expect(JSONString("def")) { it.value }
            expect("Node not correct type (Int), was \"def\", at /field2/1") { it.message }
        }
    }

    @Test fun `should get short from object using pointer`() {
        expect(123) { testObject.getShort(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getShort from object using pointer when not short`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getShort(JSONPointer("/field2/0")) }.let {
            expect("Node") { it.nodeName }
            expect("Short") { it.target }
            expect(JSONPointer("/field2/0")) { it.key }
            expect(JSONString("abc")) { it.value }
            expect("Node not correct type (Short), was \"abc\", at /field2/0") { it.message }
        }
    }

    @Test fun `should get byte from object using pointer`() {
        expect(123) { testObject.getByte(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getByte from object using pointer when not byte`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getByte(JSONPointer("/field2/1")) }.let {
            expect("Node") { it.nodeName }
            expect("Byte") { it.target }
            expect(JSONPointer("/field2/1")) { it.key }
            expect(JSONString("def")) { it.value }
            expect("Node not correct type (Byte), was \"def\", at /field2/1") { it.message }
        }
    }

    @Test fun `should get unsigned long from object using pointer`() {
        expect(123U) { testObject.getULong(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getULong from object using pointer when not unsigned long`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getULong(JSONPointer("/field2/0")) }.let {
            expect("Node") { it.nodeName }
            expect("ULong") { it.target }
            expect(JSONPointer("/field2/0")) { it.key }
            expect(JSONString("abc")) { it.value }
            expect("Node not correct type (ULong), was \"abc\", at /field2/0") { it.message }
        }
    }

    @Test fun `should get unsigned int from object using pointer`() {
        expect(123U) { testObject.getUInt(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getUInt from object using pointer when not unsigned int`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getUInt(JSONPointer("/field2/1")) }.let {
            expect("Node") { it.nodeName }
            expect("UInt") { it.target }
            expect(JSONPointer("/field2/1")) { it.key }
            expect(JSONString("def")) { it.value }
            expect("Node not correct type (UInt), was \"def\", at /field2/1") { it.message }
        }
    }

    @Test fun `should get unsigned short from object using pointer`() {
        expect(123U) { testObject.getUShort(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getUShort from object using pointer when not unsigned short`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getUShort(JSONPointer("/field2/0")) }.let {
            expect("Node") { it.nodeName }
            expect("UShort") { it.target }
            expect(JSONPointer("/field2/0")) { it.key }
            expect(JSONString("abc")) { it.value }
            expect("Node not correct type (UShort), was \"abc\", at /field2/0") { it.message }
        }
    }

    @Test fun `should get unsigned byte from object using pointer`() {
        expect(123U) { testObject.getUByte(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getUByte from object using pointer when not unsigned byte`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getUByte(JSONPointer("/field2/1")) }.let {
            expect("Node") { it.nodeName }
            expect("UByte") { it.target }
            expect(JSONPointer("/field2/1")) { it.key }
            expect(JSONString("def")) { it.value }
            expect("Node not correct type (UByte), was \"def\", at /field2/1") { it.message }
        }
    }

    @Test fun `should get decimal from object using pointer`() {
        expect(BigDecimal(123)) { testObject.getDecimal(JSONPointer("/field1")) }
    }

    @Test fun `should fail on getDecimal from object using pointer when not decimal`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getDecimal(JSONPointer("/field2/0")) }.let {
            expect("Node") { it.nodeName }
            expect("BigDecimal") { it.target }
            expect(JSONPointer("/field2/0")) { it.key }
            expect(JSONString("abc")) { it.value }
            expect("Node not correct type (BigDecimal), was \"abc\", at /field2/0") { it.message }
        }
    }

    @Test fun `should get boolean from object using pointer`() {
        expect(true) { testObject.getBoolean(JSONPointer("/field3")) }
    }

    @Test fun `should fail on getBoolean from object using pointer when not boolean`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getBoolean(JSONPointer("/field1")) }.let {
            expect("Node") { it.nodeName }
            expect("Boolean") { it.target }
            expect(JSONPointer("/field1")) { it.key }
            expect(JSONInt(123)) { it.value }
            expect("Node not correct type (Boolean), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should get array from object using pointer`() {
        expect(JSONArray.of(JSONString("abc"), JSONString("def"))) { testObject.getArray(JSONPointer("/field2")) }
    }

    @Test fun `should fail on getArray from object using pointer when not array`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getArray(JSONPointer("/field1")) }.let {
            expect("Node") { it.nodeName }
            expect("JSONArray") { it.target }
            expect(JSONPointer("/field1")) { it.key }
            expect(JSONInt(123)) { it.value }
            expect("Node not correct type (JSONArray), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should get object from object using pointer`() {
        expect(testObject) { testObject.getObject(JSONPointer.root) }
    }

    @Test fun `should fail on getObject from object using pointer when not object`() {
        assertFailsWith<JSONIncorrectTypeException> { testObject.getObject(JSONPointer("/field2")) }.let {
            expect("Node") { it.nodeName }
            expect("JSONObject") { it.target }
            expect(JSONPointer("/field2")) { it.key }
            expect(JSONArray.of(JSONString("abc"), JSONString("def"))) { it.value }
            expect("Node not correct type (JSONObject), was [ ... ], at /field2") { it.message }
        }
    }

}
