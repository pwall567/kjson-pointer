/*
 * @(#) JSONReferenceTest.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2021, 2022, 2024 Peter Wall
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

import io.kjson.JSONArray
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.pointer.test.SampleJSON.testObject
import io.kjson.pointer.test.SampleJSON.testString
import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldNotBe

class JSONReferenceTest {

    @Test fun `should create JSONReference with given pointer`() {
        val testReference = JSONPointer.root ref testString
        testReference.base shouldBe testString
        testReference.pointer shouldBe JSONPointer.root
        testReference.valid shouldBe true
        testReference.value shouldBe testString
        testReference.toString() shouldBe "\"test1\""
    }

    @Test fun `should create JSONReference with default pointer`() {
        val testReference = JSONReference(testString)
        testReference.base shouldBe testString
        testReference.pointer shouldBe JSONPointer.root
        testReference.valid shouldBe true
        testReference.value shouldBe testString
        testReference.toString() shouldBe "\"test1\""
    }

    @Test fun `should create JSONReference with non-root pointer`() {
        val testReference = JSONPointer("/field1") ref testObject
        testReference.base shouldBeSameInstance testObject
        testReference.pointer shouldBe JSONPointer.root.child("field1")
        testReference.valid shouldBe true
        testReference.value shouldBe JSONInt(123)
        testReference.toString() shouldBe "123"
    }

    @Test fun `should create JSONReference with invalid pointer`() {
        val testReference = JSONPointer("/field99") ref testObject
        testReference.base shouldBeSameInstance testObject
        testReference.pointer shouldBe JSONPointer.root.child("field99")
        testReference.valid shouldBe false
        testReference.value shouldBe null
        testReference.toString() shouldBe "invalid"
    }

    @Test fun `should navigate to child by name`() {
        val testReference1 = JSONReference(testObject)
        testReference1.base shouldBeSameInstance testObject
        testReference1.pointer shouldBe JSONPointer.root
        testReference1.valid shouldBe true
        testReference1.value.shouldBeType<JSONObject>()
        testReference1.toString() shouldBe """{"field1":123,"field2":["abc","def"],"field3":true}"""
        val testReference2 = testReference1.child("field1")
        testReference2.base shouldBeSameInstance testObject
        testReference2.pointer shouldBe JSONPointer.root.child("field1")
        testReference2.valid shouldBe true
        testReference2.value shouldBe JSONInt(123)
        testReference2.toString() shouldBe "123"
    }

    @Test fun `should navigate to child by index`() {
        val testReference1 = JSONReference(testObject)
        testReference1.base shouldBeSameInstance testObject
        testReference1.pointer shouldBe JSONPointer.root
        testReference1.valid shouldBe true
        testReference1.value.shouldBeType<JSONObject>()
        testReference1.toString() shouldBe """{"field1":123,"field2":["abc","def"],"field3":true}"""
        val testReference2 = testReference1.child("field2")
        testReference2.base shouldBeSameInstance testObject
        testReference2.pointer shouldBe JSONPointer.root.child("field2")
        testReference2.valid shouldBe true
        testReference2.value.shouldBeType<JSONArray>()
        val testReference3 = testReference2.child(1)
        testReference3.base shouldBeSameInstance testObject
        testReference3.pointer shouldBe JSONPointer.root.child("field2").child(1)
        testReference3.valid shouldBe true
        testReference3.value shouldBe JSONString("def")
        testReference3.toString() shouldBe "\"def\""
    }

    @Test fun `should navigate back to parent`() {
        val testReference1 = JSONReference(testObject)
        testReference1.base shouldBeSameInstance testObject
        testReference1.pointer shouldBe JSONPointer.root
        testReference1.valid shouldBe true
        testReference1.value.shouldBeType<JSONObject>()
        testReference1.toString() shouldBe """{"field1":123,"field2":["abc","def"],"field3":true}"""
        val testReference2 = testReference1.child("field1")
        testReference2.base shouldBeSameInstance testObject
        testReference2.pointer shouldBe JSONPointer.root.child("field1")
        testReference2.valid shouldBe true
        testReference2.value shouldBe JSONInt(123)
        testReference2.toString() shouldBe "123"
        val testReference3 = testReference2.parent()
        testReference3.base shouldBeSameInstance testObject
        testReference3.pointer shouldBe JSONPointer.root
        testReference3.valid shouldBe true
        testReference3.value.shouldBeType<JSONObject>()
        testReference3.toString() shouldBe """{"field1":123,"field2":["abc","def"],"field3":true}"""
    }

    @Test fun `should respond correctly to hasChild`() {
        val testReference1 = JSONReference(testObject)
        testReference1.hasChild("field99") shouldBe false
        testReference1.hasChild("field2") shouldBe true
        val testReference2 = testReference1.child("field2")
        testReference2.hasChild("field99") shouldBe false
        testReference2.hasChild(2) shouldBe false
        testReference2.hasChild(0) shouldBe true
    }

    @Test fun `should regard null base as not valid`() {
        val testReference1 = JSONReference(null)
        testReference1.valid shouldBe false
        val testReference2 = JSONPointer.root ref null
        testReference2.valid shouldBe false
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
        JSONReference(obj1).locateChild(obj2) shouldBe (JSONPointer("/aaa") ref obj1)
        JSONReference(obj1).locateChild(str1) shouldBe (JSONPointer("/aaa/bbb") ref obj1)
        JSONReference(obj2).locateChild(str1) shouldBe (JSONPointer("/bbb") ref obj2)
        JSONReference(obj1).locateChild(int2) shouldBe (JSONPointer("/aaa/ccc/1") ref obj1)
    }

    @Test fun `should regard equal references as equal`() {
        val testReference1 = JSONPointer("/field2/1") ref testObject
        val testReference2 = JSONReference(testObject).child("field2").child(1)
        testReference2 shouldBe testReference1
    }

    @Test fun `should regard references with different paths as not equal`() {
        val testReference1 = JSONPointer("/field2/1") ref testObject
        val testReference2 = JSONReference(testObject).child("field2")
        testReference2 shouldNotBe testReference1
    }

    @Test fun `should regard references with different bases as not equal`() {
        val testReference1 = JSONPointer("/field2") ref testObject
        val testReference2 = JSONPointer("/field2") ref JSONObject.from(testObject)
        testReference2 shouldNotBe testReference1
    }

}
