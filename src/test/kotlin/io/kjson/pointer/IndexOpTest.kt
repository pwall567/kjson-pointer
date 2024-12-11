/*
 * @(#) IndexOpTest.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2022, 2023, 2024 Peter Wall
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

import java.math.BigDecimal

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldThrow

import io.kjson.JSON.asInt
import io.kjson.JSON.asString
import io.kjson.JSONArray
import io.kjson.JSONInt
import io.kjson.JSONString
import io.kjson.JSONTypeException
import io.kjson.JSONValue
import io.kjson.pointer.test.SampleJSON.testObject

class IndexOpTest {

    @Test fun `should perform indexing operation`() {
        testObject[JSONPointer("/field1")].asInt shouldBe 123
        testObject[JSONPointer("/field2/0")].asString shouldBe "abc"
    }

    @Test fun `should return null on indexing operation not found`() {
        testObject[JSONPointer("/bad")] shouldBe null
    }

    @Test fun `should allow indexing operation on null and return not found`() {
        val nullValue: JSONValue? = null
        nullValue[JSONPointer("/bad")] shouldBe null
    }

    @Test fun `should perform -in- operation`() {
        (JSONPointer("/field1") in testObject) shouldBe true
        (JSONPointer("/field2/0") in testObject) shouldBe true
        (JSONPointer("/field99") in testObject) shouldBe false
        (JSONPointer("/field2/2") in testObject) shouldBe false
    }

    @Test fun `should allow -in- operation on null and return not found`() {
        val nullValue: JSONValue? = null
        (JSONPointer("/wrong") in nullValue) shouldBe false
    }

    @Test fun `should get string from object using pointer`() {
        testObject.getString(JSONPointer("/field2/0")) shouldBe "abc"
        testObject.getString(JSONPointer("/field2/1")) shouldBe "def"
    }

    @Test fun `should fail on getString from object using pointer when not string`() {
        shouldThrow<JSONTypeException>("Node not correct type (String), was 123, at /field1") {
            testObject.getString(JSONPointer("/field1"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "String"
            it.key shouldBe JSONPointer("/field1")
            it.value shouldBe JSONInt(123)
        }
    }

    @Test fun `should get long from object using pointer`() {
        testObject.getLong(JSONPointer("/field1")) shouldBe 123
    }

    @Test fun `should fail on getLong from object using pointer when not long`() {
        shouldThrow<JSONTypeException>("Node not correct type (Long), was \"abc\", at /field2/0") {
            testObject.getLong(JSONPointer("/field2/0"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Long"
            it.key shouldBe JSONPointer("/field2/0")
            it.value shouldBe JSONString("abc")
        }
    }

    @Test fun `should get int from object using pointer`() {
        testObject.getInt(JSONPointer("/field1")) shouldBe 123
    }

    @Test fun `should fail on getInt from object using pointer when not int`() {
        shouldThrow<JSONTypeException>("Node not correct type (Int), was \"def\", at /field2/1") {
            testObject.getInt(JSONPointer("/field2/1"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Int"
            it.key shouldBe JSONPointer("/field2/1")
            it.value shouldBe JSONString("def")
        }
    }

    @Test fun `should get short from object using pointer`() {
        testObject.getShort(JSONPointer("/field1")) shouldBe 123
    }

    @Test fun `should fail on getShort from object using pointer when not short`() {
        shouldThrow<JSONTypeException>("Node not correct type (Short), was \"abc\", at /field2/0") {
            testObject.getShort(JSONPointer("/field2/0"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Short"
            it.key shouldBe JSONPointer("/field2/0")
            it.value shouldBe JSONString("abc")
        }
    }

    @Test fun `should get byte from object using pointer`() {
        testObject.getByte(JSONPointer("/field1")) shouldBe 123
    }

    @Test fun `should fail on getByte from object using pointer when not byte`() {
        shouldThrow<JSONTypeException>("Node not correct type (Byte), was \"def\", at /field2/1") {
            testObject.getByte(JSONPointer("/field2/1"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Byte"
            it.key shouldBe JSONPointer("/field2/1")
            it.value shouldBe JSONString("def")
        }
    }

    @Test fun `should get unsigned long from object using pointer`() {
        testObject.getULong(JSONPointer("/field1")) shouldBe 123U
    }

    @Test fun `should fail on getULong from object using pointer when not unsigned long`() {
        shouldThrow<JSONTypeException>("Node not correct type (ULong), was \"abc\", at /field2/0") {
            testObject.getULong(JSONPointer("/field2/0"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "ULong"
            it.key shouldBe JSONPointer("/field2/0")
            it.value shouldBe JSONString("abc")
        }
    }

    @Test fun `should get unsigned int from object using pointer`() {
        testObject.getUInt(JSONPointer("/field1")) shouldBe 123U
    }

    @Test fun `should fail on getUInt from object using pointer when not unsigned int`() {
        shouldThrow<JSONTypeException>("Node not correct type (UInt), was \"def\", at /field2/1") {
            testObject.getUInt(JSONPointer("/field2/1"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "UInt"
            it.key shouldBe JSONPointer("/field2/1")
            it.value shouldBe JSONString("def")
        }
    }

    @Test fun `should get unsigned short from object using pointer`() {
        testObject.getUShort(JSONPointer("/field1")) shouldBe 123U
    }

    @Test fun `should fail on getUShort from object using pointer when not unsigned short`() {
        shouldThrow<JSONTypeException>("Node not correct type (UShort), was \"abc\", at /field2/0") {
            testObject.getUShort(JSONPointer("/field2/0"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "UShort"
            it.key shouldBe JSONPointer("/field2/0")
            it.value shouldBe JSONString("abc")
        }
    }

    @Test fun `should get unsigned byte from object using pointer`() {
        testObject.getUByte(JSONPointer("/field1")) shouldBe 123U
    }

    @Test fun `should fail on getUByte from object using pointer when not unsigned byte`() {
        shouldThrow<JSONTypeException>("Node not correct type (UByte), was \"def\", at /field2/1") {
            testObject.getUByte(JSONPointer("/field2/1"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "UByte"
            it.key shouldBe JSONPointer("/field2/1")
            it.value shouldBe JSONString("def")
        }
    }

    @Test fun `should get decimal from object using pointer`() {
        testObject.getDecimal(JSONPointer("/field1")) shouldBe BigDecimal(123)
    }

    @Test fun `should fail on getDecimal from object using pointer when not decimal`() {
        shouldThrow<JSONTypeException>("Node not correct type (BigDecimal), was \"abc\", at /field2/0") {
            testObject.getDecimal(JSONPointer("/field2/0"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "BigDecimal"
            it.key shouldBe JSONPointer("/field2/0")
            it.value shouldBe JSONString("abc")
        }
    }

    @Test fun `should get boolean from object using pointer`() {
        testObject.getBoolean(JSONPointer("/field3")) shouldBe true
    }

    @Test fun `should fail on getBoolean from object using pointer when not boolean`() {
        shouldThrow<JSONTypeException>("Node not correct type (Boolean), was 123, at /field1") {
            testObject.getBoolean(JSONPointer("/field1"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Boolean"
            it.key shouldBe JSONPointer("/field1")
            it.value shouldBe JSONInt(123)
        }
    }

    @Test fun `should get array from object using pointer`() {
        testObject.getArray(JSONPointer("/field2")) shouldBe JSONArray.of(JSONString("abc"), JSONString("def"))
    }

    @Test fun `should fail on getArray from object using pointer when not array`() {
        shouldThrow<JSONTypeException>("Node not correct type (JSONArray), was 123, at /field1") {
            testObject.getArray(JSONPointer("/field1"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "JSONArray"
            it.key shouldBe JSONPointer("/field1")
            it.value shouldBe JSONInt(123)
        }
    }

    @Test fun `should get object from object using pointer`() {
        testObject.getObject(JSONPointer.root) shouldBe testObject
    }

    @Test fun `should fail on getObject from object using pointer when not object`() {
        shouldThrow<JSONTypeException>("Node not correct type (JSONObject), was [ ... ], at /field2") {
            testObject.getObject(JSONPointer("/field2"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "JSONObject"
            it.key shouldBe JSONPointer("/field2")
            it.value shouldBe JSONArray.of(JSONString("abc"), JSONString("def"))
        }
    }

}
