/*
 * @(#) ExtensionTest.kt
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
import kotlin.test.fail

import java.math.BigDecimal

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeNonNull
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
import io.kjson.pointer.test.SampleJSON.testArray
import io.kjson.pointer.test.SampleJSON.testMixedArray
import io.kjson.pointer.test.SampleJSON.testObject
import io.kjson.pointer.test.SampleJSON.testObjectWithNull

class ExtensionTest {

    @Test fun `should conditionally execute code`() {
        val json = JSON.parseObject("""{"a":1,"b":1,"c":2,"d":3,"e":5}""")
        val ref = JSONRef(json)
        ref.ifPresent<JSONInt>("a") {
            it.value shouldBe 1
        }
        ref.ifPresent<JSONInt>("x") {
            fail("Should not get here")
        }
        ref.ifPresent<JSONString>("a") {
            fail("Should not get here")
        }
    }

    @Test fun `should map contents of array to its primitive type`() {
        val json = JSON.parseArray("[1,10,100,1000,10000]")
        val ref = JSONRef(json)
        val array = ref.map<JSONInt, Int>()
        array shouldBe listOf(1, 10, 100, 1000, 10000)
    }

    @Test fun `should throw exception when 'map' operation finds invalid value`() {
        val json = JSON.parseArray("""[1,10,"error",1000,10000]""")
        val ref = JSONRef(json)
        shouldThrow<JSONTypeException>("Child not correct type (JSONInt), was \"error\", at /2") {
            ref.map<JSONInt, Int>()
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONInt"
            it.value shouldBe JSONString("error")
            it.key shouldBe JSONPointer("/2")
        }
    }

    @Test fun `should map contents of array with transformation`() {
        val json = JSON.parseArray("[1,10,100,1000,10000]")
        val ref = JSONRef(json)
        val array = ref.map<JSONInt, Int> { value * (it + 1) }
        array shouldBe listOf(1, 20, 300, 4000, 50000)
    }

    @Test fun `should throw exception when transforming 'map' operation finds invalid value`() {
        val json = JSON.parseArray("[1,10,100,false,10000]")
        val ref = JSONRef(json)
        shouldThrow<JSONTypeException>("Child not correct type (JSONInt), was false, at /3") {
            ref.map<JSONInt, Int> { value * (it + 1) }
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONInt"
            it.value shouldBe JSONBoolean.FALSE
            it.key shouldBe JSONPointer("/3")
        }
    }

    @Test fun `should map contents of array with complex transformation`() {
        val json = JSON.parseArray("""[{"f1":"AAA"},{"f1":"BBB"},{"f1":"CCC"},{"f1":"DDD"}]""")
        val ref = JSONRef(json)
        val array = ref.map<JSONObject, String> { child<JSONString>("f1").value }
        array shouldBe listOf("AAA", "BBB", "CCC", "DDD")
    }

    @Test fun `should test whether any array item matches predicate`() {
        val json = JSON.parseArray("[1,2,3,4,5]")
        val ref = JSONRef(json)
        ref.any<JSONInt> { value > 3 } shouldBe true
        ref.any<JSONInt> { value > 10 } shouldBe false
    }

    @Test fun `should throw exception when 'any' test finds invalid value`() {
        val json = JSON.parseArray("""[1,2,3,"wrong",5]""")
        val ref = JSONRef(json)
        shouldThrow<JSONTypeException>("Child not correct type (JSONInt), was \"wrong\", at /3") {
            ref.any<JSONInt> { value > 3 }
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONInt"
            it.value shouldBe JSONString("wrong")
            it.key shouldBe JSONPointer("/3")
        }
    }

    @Test fun `should test whether all array items match predicate`() {
        val json = JSON.parseArray("[1,2,3,4,5]")
        val ref = JSONRef(json)
        ref.all<JSONInt> { value < 10 } shouldBe true
        ref.all<JSONInt> { value < 5 } shouldBe false
    }

    @Test fun `should throw exception when 'all' test finds invalid value`() {
        val json = JSON.parseArray("""[1,2,"bad",4,5]""")
        val ref = JSONRef(json)
        shouldThrow<JSONTypeException>("Child not correct type (JSONInt), was \"bad\", at /2") {
            ref.all<JSONInt> { value < 10 }
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONInt"
            it.value shouldBe JSONString("bad")
            it.key shouldBe JSONPointer("/2")
        }
    }

    @Suppress("DEPRECATION")
    @Test fun `should unconditionally map value`() {
        val json = JSON.parseObject("""{"a":1,"b":1,"c":2,"d":3,"e":5}""")
        val ref = JSONRef(json)
        val d = ref.map<JSONInt, Int>("d") {
            it.value
        }
        d shouldBe 3
        shouldThrow<JSONPointerException>("Can't locate JSON property \"x\"") {
            ref.map<JSONInt, Int>("x") { it.value }
        }.let {
            it.text shouldBe "Can't locate JSON property \"x\""
            it.pointer shouldBe JSONPointer.root
            it.cause shouldBe null
        }
        val e: Int = ref.map("e") { prop: JSONInt -> prop.value }
        e shouldBe 5
        // alternative, following deprecation of original function, using recommended replacement code
        val dd = ref.child<JSONInt>("d").value
        dd shouldBe 3
        shouldThrow<JSONPointerException>("Can't locate JSON property \"x\"") {
            ref.child<JSONInt>("x").value
        }.let {
            it.text shouldBe "Can't locate JSON property \"x\""
            it.pointer shouldBe JSONPointer.root
            it.cause shouldBe null
        }
    }

    @Suppress("DEPRECATION")
    @Test fun `should conditionally map value`() {
        val json = JSON.parseObject("""{"a":1,"b":1,"c":2,"d":3,"e":5}""")
        val ref = JSONRef(json)
        val d = ref.mapIfPresent<JSONInt, Int>("d") {
            it.value
        }
        d shouldBe 3
        val x = ref.mapIfPresent<JSONInt, Int>("x") {
            it.value
        }
        x shouldBe null
        val e: Int? = ref.mapIfPresent("e") { prop: JSONInt -> prop.value }
        e shouldBe 5
        // alternative, following deprecation of original function, using recommended replacement code
        val dd = ref.optionalChild<JSONInt>("d")?.value
        dd shouldBe 3
        val xx = ref.optionalChild<JSONInt>("x")?.value
        xx shouldBe null
    }

    @Test fun `should get optional String`() {
        val json = JSON.parseObject("""{"aaa":"Hello","bbb":"World","xxx":999}""")
        val ref = JSONRef(json)
        ref.optionalString("aaa") shouldBe "Hello"
        ref.optionalString("bbb") shouldBe "World"
        ref.optionalString("ccc") shouldBe null
        shouldThrow<JSONTypeException>("Node not correct type (String), was 999, at /xxx") {
            ref.optionalString("xxx")
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "String"
            it.value shouldBe JSONInt(999)
            it.key shouldBe JSONPointer("/xxx")
        }
    }

    @Test fun `should get optional Boolean`() {
        val json = JSON.parseObject("""{"aaa":true,"bbb":false,"xxx":999}""")
        val ref = JSONRef(json)
        ref.optionalBoolean("aaa") shouldBe true
        ref.optionalBoolean("bbb") shouldBe false
        ref.optionalBoolean("ccc") shouldBe null
        shouldThrow<JSONTypeException>("Node not correct type (Boolean), was 999, at /xxx") {
            ref.optionalBoolean("xxx")
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Boolean"
            it.value shouldBe JSONInt(999)
            it.key shouldBe JSONPointer("/xxx")
        }
    }

    @Test fun `should get optional Int`() {
        val json = JSON.parseObject("""{"aaa":123,"bbb":456,"xxx":"wrong"}""")
        val ref = JSONRef(json)
        ref.optionalInt("aaa") shouldBe 123
        ref.optionalInt("bbb") shouldBe 456
        ref.optionalInt("ccc") shouldBe null
        shouldThrow<JSONTypeException>("Node not correct type (Int), was \"wrong\", at /xxx") {
            ref.optionalInt("xxx")
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Int"
            it.value shouldBe JSONString("wrong")
            it.key shouldBe JSONPointer("/xxx")
        }
    }

    @Test fun `should get optional Long`() {
        val json = JSON.parseObject("""{"aaa":123,"bbb":123456789123456789,"xxx":"wrong"}""")
        val ref = JSONRef(json)
        ref.optionalLong("aaa") shouldBe 123
        ref.optionalLong("bbb") shouldBe 123456789123456789
        ref.optionalLong("ccc") shouldBe null
        shouldThrow<JSONTypeException>("Node not correct type (Long), was \"wrong\", at /xxx") {
            ref.optionalLong("xxx")
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Long"
            it.value shouldBe JSONString("wrong")
            it.key shouldBe JSONPointer("/xxx")
        }
    }

    @Test fun `should get optional Decimal`() {
        val json = JSON.parseObject("""{"aaa":123,"bbb":123456789123456789,"ccc":1.5,"xxx":"wrong"}""")
        val ref = JSONRef(json)
        ref.optionalDecimal("aaa") shouldBe BigDecimal(123)
        ref.optionalDecimal("bbb") shouldBe BigDecimal(123456789123456789)
        ref.optionalDecimal("ccc") shouldBe BigDecimal("1.5")
        ref.optionalDecimal("ddd") shouldBe null
        shouldThrow<JSONTypeException>("Node not correct type (Decimal), was \"wrong\", at /xxx") {
            ref.optionalDecimal("xxx")
        }.let {
            it.nodeName shouldBe "Node"
            it.expected shouldBe "Decimal"
            it.value shouldBe JSONString("wrong")
            it.key shouldBe JSONPointer("/xxx")
        }
    }

    @Test fun `should get optional child JSONString`() {
        val json = JSON.parseObject("""{"aaa":"OK"}""")
        val ref = JSONRef(json)
        val childRef = ref.optionalChild<JSONString>("aaa")
        childRef.shouldBeNonNull()
        childRef.value shouldBe "OK"
        ref.optionalChild<JSONString>("bbb") shouldBe null
    }

    @Test fun `should get optional child nullable JSONString`() {
        val json = JSON.parseObject("""{"aaa":"OK","bbb":null,"ccc":123}""")
        val ref = JSONRef(json)
        val childRef = ref.optionalChild<JSONString?>("aaa")
        childRef.shouldBeNonNull()
        childRef.node?.value shouldBe "OK"
        val nullChildRef = ref.optionalChild<JSONString?>("bbb")
        nullChildRef.shouldBeNonNull()
        nullChildRef.node shouldBe null
        shouldThrow<JSONTypeException>("Child not correct type (JSONString?), was 123, at /ccc") {
            ref.optionalChild<JSONString?>("ccc")
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONString?"
            it.value shouldBe JSONInt(123)
            it.key shouldBe JSONPointer("/ccc")
        }
        ref.optionalChild<JSONString?>("ddd") shouldBe null
    }

    @Test fun `should get optional child object`() {
        val json = JSON.parseObject("""{"aaa":{"bbb":1000},"xxx":999}""")
        val ref = JSONRef(json)
        val childRef = ref.optionalChild<JSONObject>("aaa")
        childRef.shouldBeNonNull()
        childRef.node["bbb"].asInt shouldBe 1000
        ref.optionalChild<JSONObject>("ccc") shouldBe null
        shouldThrow<JSONTypeException>("Child not correct type (JSONObject), was 999, at /xxx") {
            ref.optionalChild<JSONObject>("xxx")
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONObject"
            it.value shouldBe JSONInt(999)
            it.key shouldBe JSONPointer("/xxx")
        }
    }

    @Test fun `should get optional child array`() {
        val json = JSON.parseObject("""{"aaa":[888],"xxx":999}""")
        val ref = JSONRef(json)
        val childRef = ref.optionalChild<JSONArray>("aaa")
        childRef.shouldBeNonNull()
        childRef.node[0].asInt shouldBe 888
        ref.optionalChild<JSONArray>("ccc") shouldBe null
        shouldThrow<JSONTypeException>("Child not correct type (JSONArray), was 999, at /xxx") {
            ref.optionalChild<JSONArray>("xxx")
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONArray"
            it.value shouldBe JSONInt(999)
            it.key shouldBe JSONPointer("/xxx")
        }
    }

    @Test fun `should perform operation with optional child`() {
        val json = JSON.parseObject("""{"aaa":[888],"xxx":999}""")
        val ref = JSONRef(json)
        ref.withOptionalChild<JSONArray>("aaa") {
            shouldBeType<JSONRef<JSONArray>>()
            node shouldBe it
            it[0].asInt shouldBe 888
        }
        ref.withOptionalChild<JSONArray>("bbb") {
            fail("Should not get here")
        }
        shouldThrow<JSONTypeException>("Child not correct type (JSONArray), was 999, at /xxx") {
            ref.withOptionalChild<JSONArray>("xxx") {
                fail("Definitely should not get here")
            }
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONArray"
            it.value shouldBe JSONInt(999)
            it.key shouldBe JSONPointer("/xxx")
        }
    }

    @Test fun `should iterate over object`() {
        val json = JSON.parseObject("""{"a":1,"b":1,"c":2,"d":3,"e":5}""")
        val ref = JSONRef(json)
        val results = mutableListOf<Pair<String, Int>>()
        ref.forEachKey<JSONInt> {
            results.add(it to node.value)
        }
        results.size shouldBe 5
        results[0] shouldBe ("a" to 1)
        results[1] shouldBe ("b" to 1)
        results[2] shouldBe ("c" to 2)
        results[3] shouldBe ("d" to 3)
        results[4] shouldBe ("e" to 5)
    }

    @Test fun `should iterate over array`() {
        val json = JSON.parseArray("""["now","is","the","hour"]""")
        val ref = JSONRef(json)
        val results = mutableListOf<Pair<Int, String>>()
        ref.forEach<JSONString> {
            results.add(it to node.value)
        }
        results[0] shouldBe (0 to "now")
        results[1] shouldBe (1 to "is")
        results[2] shouldBe (2 to "the")
        results[3] shouldBe (3 to "hour")
    }

    @Test fun `should get untyped child of object`() {
        val ref = JSONRef(testObjectWithNull)
        val child = ref.untypedChild("field1")
        child.isRef<JSONInt>() shouldBe true
    }

    @Test fun `should get untyped child of array`() {
        val ref = JSONRef(testMixedArray)
        val child0 = ref.untypedChild(0)
        child0.isRef<JSONInt>() shouldBe true
        val child1 = ref.untypedChild(1)
        child1.isRef<JSONBoolean>() shouldBe true
    }

    @Test fun `should create a reference to a JSONObject`() {
        val ref = testObject.ref()
        ref.isRef<JSONObject>() shouldBe true
        testObject shouldBeSameInstance ref.base
        testObject shouldBeSameInstance ref.node
        ref.pointer shouldBe JSONPointer.root
    }

    @Test fun `should create a reference to a JSONArray`() {
        val ref = testArray.ref()
        ref.isRef<JSONArray>() shouldBe true
        testArray shouldBeSameInstance ref.base
        testArray shouldBeSameInstance ref.node
        ref.pointer shouldBe JSONPointer.root
    }

    @Test fun `should correctly report hasChild for object`() {
        val ref1 = JSONRef(testObject)
        ref1.hasChild<JSONInt>("field1") shouldBe true
        ref1.hasChild<JSONArray>("field2") shouldBe true
        ref1.hasChild<JSONInt>("field3") shouldBe false
        ref1.hasChild<JSONString>("field1") shouldBe false
    }

    @Test fun `should correctly report nullable hasChild for object`() {
        val ref = JSONRef(testObjectWithNull)
        ref.hasChild<JSONInt>("field1") shouldBe true
        ref.hasChild<JSONInt?>("field1") shouldBe true
        ref.hasChild<JSONInt?>("field3") shouldBe true
        ref.hasChild<JSONInt>("field3") shouldBe false
        ref.hasChild<JSONInt?>("field2") shouldBe false
        ref.hasChild<JSONInt?>("field9") shouldBe false
    }

    @Test fun `should correctly report hasChild for array`() {
        val ref1 = JSONRef(testMixedArray)
        ref1.hasChild<JSONInt>(0) shouldBe true
        ref1.hasChild<JSONNumber>(0) shouldBe true
        ref1.hasChild<JSONBoolean>(1) shouldBe true
        ref1.hasChild<JSONString>(2) shouldBe true
        ref1.hasChild<JSONArray>(3) shouldBe true
        ref1.hasChild<JSONInt>(1) shouldBe false
        ref1.hasChild<JSONValue>(4) shouldBe false
    }

    @Test fun `should correctly report nullable hasChild for array`() {
        val ref1 = JSONRef(testMixedArray)
        ref1.hasChild<JSONInt?>(0) shouldBe true
        ref1.hasChild<JSONNumber?>(0) shouldBe true
        ref1.hasChild<JSONBoolean?>(1) shouldBe true
        ref1.hasChild<JSONString?>(2) shouldBe true
        ref1.hasChild<JSONArray?>(3) shouldBe true
        ref1.hasChild<JSONInt?>(4) shouldBe true
        ref1.hasChild<JSONString?>(0) shouldBe false
    }

    @Test fun `should navigate to child of object`() {
        val ref1 = JSONRef(testObject)
        ref1.hasChild<JSONInt>("field1") shouldBe true
        val ref2 = ref1.child<JSONInt>("field1")
        ref2.node.value shouldBe 123
        ref2.pointer shouldBe JSONPointer("/field1")
    }

    @Test fun `should navigate to nullable child of object`() {
        val ref1 = JSONRef(testObjectWithNull)
        ref1.hasChild<JSONInt?>("field1") shouldBe true
        val ref2 = ref1.child<JSONInt?>("field1")
        ref2.node?.value shouldBe 123
        ref2.pointer shouldBe JSONPointer("/field1")
        ref1.hasChild<JSONInt?>("field3") shouldBe true
        val ref3 = ref1.child<JSONInt?>("field3")
        ref3.node shouldBe null
    }

    @Test fun `should throw exception navigating to child of incorrect type`() {
        val ref1 = JSONRef(testObject)
        shouldThrow<JSONTypeException>("Child not correct type (JSONBoolean), was 123, at /field1") {
            ref1.child<JSONBoolean>("field1")
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONBoolean"
            it.value shouldBe JSONInt(123)
            it.key shouldBe JSONPointer("/field1")
        }
    }

    @Test fun `should throw exception navigating to null child of non-nullable type`() {
        val ref1 = JSONRef(testObjectWithNull)
        shouldThrow<JSONTypeException>("Child not correct type (JSONInt), was null, at /field3") {
            ref1.child<JSONInt>("field3")
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONInt"
            it.value shouldBe null
            it.key shouldBe JSONPointer("/field3")
        }
    }

    @Test fun `should throw exception navigating to child of incorrect nullable type`() {
        val ref1 = JSONRef(testObject)
        shouldThrow<JSONTypeException>("Child not correct type (JSONString?), was 123, at /field1") {
            ref1.child<JSONString?>("field1")
        }.let {
            it.nodeName shouldBe "Child"
            it.expected shouldBe "JSONString?"
            it.value shouldBe JSONInt(123)
            it.key shouldBe JSONPointer("/field1")
        }
    }

    @Test fun `should navigate to child of array`() {
        val refArray = JSONRef(testMixedArray)
        refArray.hasChild<JSONInt>(0) shouldBe true
        with(refArray.child<JSONInt>(0)) {
            node.value shouldBe 123
            pointer shouldBe JSONPointer("/0")
        }
    }

    @Test fun `should build JSONRef using infix operator`() {
        val ref1: JSONRef<JSONInt> = testObject ptr JSONPointer("/field1")
        ref1.node.value shouldBe 123
        val ref2: JSONRef<JSONString> = testObject ptr JSONPointer("/field2/1")
        ref2.node.value shouldBe "def"
        val ref3 = ref2.parent<JSONArray>()
        ref3.node shouldBeSameInstance testObject["field2"]
        val ref4 = ref3.parent<JSONObject>()
        ref4.node shouldBeSameInstance testObject
    }

}
