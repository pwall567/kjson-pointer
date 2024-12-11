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

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

import io.kjson.JSON
import io.kjson.JSON.asInt
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
        ref1.base shouldBeSameInstance testString
        ref1.pointer shouldBe JSONPointer.root
        ref1.node shouldBeSameInstance testString
    }

    @Test fun `should navigate back to parent`() {
        val refObject = JSONRef.of<JSONInt>(testObject, JSONPointer("/field1"))
        val parent = refObject.parent<JSONObject>()
        parent.node["field3"] shouldBe JSONBoolean.TRUE
        parent.pointer shouldBe JSONPointer.root
    }

    @Test fun `should throw exception when trying to navigate to parent of root pointer`() {
        val refObject = JSONRef(testObject)
        shouldThrow<JSONPointerException>("Can't get parent of root JSON Pointer") {
            refObject.parent<JSONObject>()
        }.let {
            it.text shouldBe "Can't get parent of root JSON Pointer"
            it.pointer shouldBeSameInstance JSONPointer.root
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception when parent is not correct type`() {
        val refChild = JSONRef(testObject).child<JSONInt>("field1")
        shouldThrow<JSONTypeException>("Parent not correct type (JSONArray), was { ... }") {
            refChild.parent<JSONArray>()
        }.let {
            it.nodeName shouldBe "Parent"
            it.expected shouldBe "JSONArray"
            it.value shouldBe testObject
            it.key shouldBe JSONPointer.root
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
        JSONRef(obj1).locateChild(obj2)?.pointer shouldBe JSONPointer("/aaa")
        JSONRef(obj1).locateChild(str1)?.pointer shouldBe JSONPointer("/aaa/bbb")
        JSONRef(obj2).locateChild(str1)?.pointer shouldBe JSONPointer("/bbb")
        JSONRef(obj1).locateChild(int2)?.pointer shouldBe JSONPointer("/aaa/ccc/1")
    }

    @Test fun `should convert to ref of correct type`() {
        val str = JSONString("mango")
        val ref = JSONRef<JSONValue>(str)
        ref.asRef<JSONString>().node.value shouldBe "mango"
    }

    @Test fun `should throw exception converting to ref of incorrect type`() {
        val str = JSONString("mango")
        val ref = JSONRef<JSONValue>(str)
        shouldThrow<JSONTypeException>("Node not correct type (JSONObject), was \"mango\"") {
            ref.asRef<JSONObject>()
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "JSONObject"
            it.value shouldBe JSONString("mango")
            it.key shouldBe JSONPointer.root
        }
    }

    @Test fun `should convert to ref of nullable type`() {
        val str = JSONString("mango")
        val ref = JSONRef<JSONValue>(str)
        ref.asRef<JSONString?>().node?.value shouldBe "mango"
    }

    @Test fun `should test for specified type`() {
        val number = JSONInt(42)
        val ref = JSONRef<JSONValue>(number)
        ref.isRef<JSONNumber>() shouldBe true
        ref.isRef<JSONInt>() shouldBe true
        ref.isRef<JSONValue>() shouldBe true
        ref.isRef<JSONString>() shouldBe false
    }

    @Test fun `should test for specified type including nullable`() {
        val number = JSONInt(42)
        val ref = JSONRef<JSONValue>(number)
        ref.isRef<JSONNumber?>() shouldBe true
        ref.isRef<JSONInt?>() shouldBe true
        ref.isRef<JSONValue?>() shouldBe true
        ref.isRef<JSONString?>() shouldBe false
    }

    @Test fun `should test for specified type with null value`() {
        val ref = JSONRef<JSONValue?>(null)
        ref.isRef<JSONNumber?>() shouldBe true
        ref.isRef<JSONInt?>() shouldBe true
        ref.isRef<JSONValue?>() shouldBe true
        ref.isRef<JSONString?>() shouldBe true
    }

    @Test fun `should throw exception building JSONRef using of with wrong type`() {
        shouldThrow<JSONTypeException>("Node not correct type (JSONString), was 123, at /field1") {
            JSONRef.of<JSONString>(testObject, JSONPointer("/field1"))
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "JSONString"
            it.value shouldBe JSONInt(123)
            it.key shouldBe JSONPointer("/field1")
        }
    }

    @Test fun `should build JSONRef using of`() {
        val ref1 = JSONRef.of<JSONInt>(testObject, JSONPointer("/field1"))
        ref1.node.value shouldBe 123
        val ref2 = JSONRef.of<JSONString>(testObject, JSONPointer("/field2/0"))
        ref2.node.value shouldBe "abc"
        val ref3 = ref2.parent<JSONArray>()
        ref3.node shouldBeSameInstance testObject["field2"]
        val ref4 = ref3.parent<JSONObject>()
        ref4.node shouldBeSameInstance testObject
    }

    @Test fun `should build JSONRef of null using of`() {
        val json = JSON.parseObject("""{"a":{"b":null}}""")
        val ref = JSONRef.of<JSONInt?>(json, JSONPointer("/a/b"))
        ref.node shouldBe null
    }

    @Test fun `should throw exception on null property when building JSONRef using of`() {
        val json = JSON.parseObject("""{"a":{"b":null}}""")
        shouldThrow<JSONPointerException>("Intermediate node is null, at /a/b") {
            JSONRef.of<JSONInt>(json, JSONPointer("/a/b/c"))
        }.let {
            it.text shouldBe "Intermediate node is null"
            it.pointer shouldBe JSONPointer("/a/b")
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception on null array item when building JSONRef using of`() {
        val json = JSON.parseObject("""{"a":[null]}""")
        shouldThrow<JSONPointerException>("Intermediate node is null, at /a/0") {
            JSONRef.of<JSONInt>(json, JSONPointer("/a/0/c"))
        }.let {
            it.text shouldBe "Intermediate node is null"
            it.pointer shouldBe JSONPointer("/a/0")
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception on property not found when building JSONRef using of`() {
        val json = JSON.parseObject("""{"a":{"b":1}}""")
        shouldThrow<JSONPointerException>("Can't locate JSON property \"c\", at /a") {
            JSONRef.of<JSONInt>(json, JSONPointer("/a/c"))
        }.let {
            it.text shouldBe "Can't locate JSON property \"c\""
            it.pointer shouldBe JSONPointer("/a")
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception on array item not found when building JSONRef using of`() {
        val json = JSON.parseObject("""{"a":[11,22]}""")
        shouldThrow<JSONPointerException>("Array index 99 out of range in JSON Pointer, at /a") {
            JSONRef.of<JSONInt>(json, JSONPointer("/a/99"))
        }.let {
            it.text shouldBe "Array index 99 out of range in JSON Pointer"
            it.pointer shouldBe JSONPointer("/a")
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception on incorrect type when building JSONRef using of`() {
        val json = JSON.parseObject("""{"a":{"b":"BAD"}}""")
        shouldThrow<JSONTypeException>("Node not correct type (JSONInt), was \"BAD\", at /a/b") {
            JSONRef.of<JSONInt>(json, JSONPointer("/a/b"))
        }.let {
            it.text shouldBe "Node not correct type (JSONInt), was \"BAD\""
            it.key shouldBe JSONPointer("/a/b")
            it.cause shouldBe null
        }
    }

    @Test fun `should create ref using untyped`() {
        val json = JSON.parseObject("""{"a":{"b":123}}""")
        val ref = JSONRef.untyped(json, JSONPointer("/a"))
        with(ref.node) {
            shouldBeType<JSONObject>()
            this["b"].asInt shouldBe 123
        }
    }

}
