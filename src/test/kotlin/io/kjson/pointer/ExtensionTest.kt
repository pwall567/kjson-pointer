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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.test.fail

import java.math.BigDecimal

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
            expect(1) { it.value }
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
        expect(listOf(1, 10, 100, 1000, 10000)) { array }
    }

    @Test fun `should throw exception when 'map' operation finds invalid value`() {
        val json = JSON.parseArray("""[1,10,"error",1000,10000]""")
        val ref = JSONRef(json)
        assertFailsWith<JSONTypeException> { ref.map<JSONInt, Int>() }.let {
            expect("Child not correct type (JSONInt), was \"error\", at /2") { it.message }
            expect("Child") { it.nodeName }
            expect("JSONInt") { it.target }
            expect(JSONString("error")) { it.value }
            expect(JSONPointer("/2")) { it.key }
        }
    }

    @Test fun `should map contents of array with transformation`() {
        val json = JSON.parseArray("[1,10,100,1000,10000]")
        val ref = JSONRef(json)
        val array = ref.map<JSONInt, Int> { value * (it + 1) }
        expect(listOf(1, 20, 300, 4000, 50000)) { array }
    }

    @Test fun `should throw exception when transforming 'map' operation finds invalid value`() {
        val json = JSON.parseArray("[1,10,100,false,10000]")
        val ref = JSONRef(json)
        assertFailsWith<JSONTypeException> { ref.map<JSONInt, Int> { value * (it + 1) } }.let {
            expect("Child not correct type (JSONInt), was false, at /3") { it.message }
            expect("Child") { it.nodeName }
            expect("JSONInt") { it.target }
            expect(JSONBoolean.FALSE) { it.value }
            expect(JSONPointer("/3")) { it.key }
        }
    }

    @Test fun `should map contents of array with complex transformation`() {
        val json = JSON.parseArray("""[{"f1":"AAA"},{"f1":"BBB"},{"f1":"CCC"},{"f1":"DDD"}]""")
        val ref = JSONRef(json)
        val array = ref.map<JSONObject, String> { child<JSONString>("f1").value }
        expect(listOf("AAA", "BBB", "CCC", "DDD")) { array }
    }

    @Test fun `should test whether any array item matches predicate`() {
        val json = JSON.parseArray("[1,2,3,4,5]")
        val ref = JSONRef(json)
        assertTrue(ref.any<JSONInt> { value > 3 })
        assertFalse(ref.any<JSONInt> { value > 10 })
    }

    @Test fun `should throw exception when 'any' test finds invalid value`() {
        val json = JSON.parseArray("""[1,2,3,"wrong",5]""")
        val ref = JSONRef(json)
        assertFailsWith<JSONTypeException> { ref.any<JSONInt> { value > 3 } }.let {
            expect("Child not correct type (JSONInt), was \"wrong\", at /3") { it.message }
            expect("Child") { it.nodeName }
            expect("JSONInt") { it.target }
            expect(JSONString("wrong")) { it.value }
            expect(JSONPointer("/3")) { it.key }
        }
    }

    @Test fun `should test whether all array items match predicate`() {
        val json = JSON.parseArray("[1,2,3,4,5]")
        val ref = JSONRef(json)
        assertTrue(ref.all<JSONInt> { value < 10 })
        assertFalse(ref.all<JSONInt> { value < 5 })
    }

    @Test fun `should throw exception when 'all' test finds invalid value`() {
        val json = JSON.parseArray("""[1,2,"bad",4,5]""")
        val ref = JSONRef(json)
        assertFailsWith<JSONTypeException> { ref.all<JSONInt> { value < 10 } }.let {
            expect("Child not correct type (JSONInt), was \"bad\", at /2") { it.message }
            expect("Child") { it.nodeName }
            expect("JSONInt") { it.target }
            expect(JSONString("bad")) { it.value }
            expect(JSONPointer("/2")) { it.key }
        }
    }

    @Suppress("DEPRECATION")
    @Test fun `should unconditionally map value`() {
        val json = JSON.parseObject("""{"a":1,"b":1,"c":2,"d":3,"e":5}""")
        val ref = JSONRef(json)
        val d = ref.map<JSONInt, Int>("d") {
            it.value
        }
        expect(3) { d }
        assertFailsWith<JSONPointerException> {
            ref.map<JSONInt, Int>("x") { it.value }
        }.let {
            expect("Can't locate JSON property \"x\"") { it.message }
            expect("Can't locate JSON property \"x\"") { it.text }
            expect(JSONPointer.root) { it.pointer }
            assertNull(it.cause)
        }
        val e: Int = ref.map("e") { prop: JSONInt -> prop.value }
        expect(5) { e }
        // alternative, following deprecation of original function, using recommended replacement code
        val dd = ref.child<JSONInt>("d").value
        expect(3) { dd }
        assertFailsWith<JSONPointerException> {
            ref.child<JSONInt>("x").value
        }.let {
            expect("Can't locate JSON property \"x\"") { it.message }
            expect("Can't locate JSON property \"x\"") { it.text }
            expect(JSONPointer.root) { it.pointer }
            assertNull(it.cause)
        }
    }

    @Suppress("DEPRECATION")
    @Test fun `should conditionally map value`() {
        val json = JSON.parseObject("""{"a":1,"b":1,"c":2,"d":3,"e":5}""")
        val ref = JSONRef(json)
        val d = ref.mapIfPresent<JSONInt, Int>("d") {
            it.value
        }
        expect(3) { d }
        val x = ref.mapIfPresent<JSONInt, Int>("x") {
            it.value
        }
        assertNull(x)
        val e: Int? = ref.mapIfPresent("e") { prop: JSONInt -> prop.value }
        expect(5) { e }
        // alternative, following deprecation of original function, using recommended replacement code
        val dd = ref.optionalChild<JSONInt>("d")?.value
        expect(3) { dd }
        val xx = ref.optionalChild<JSONInt>("x")?.value
        assertNull(xx)
    }

    @Test fun `should get optional String`() {
        val json = JSON.parseObject("""{"aaa":"Hello","bbb":"World","xxx":999}""")
        val ref = JSONRef(json)
        expect("Hello") { ref.optionalString("aaa") }
        expect("World") { ref.optionalString("bbb") }
        assertNull(ref.optionalString("ccc"))
        assertFailsWith<JSONTypeException> { ref.optionalString("xxx") }.let {
            expect("Node not correct type (String), was 999, at /xxx") { it.message }
            expect("Node") { it.nodeName }
            expect("String") { it.target }
            expect(JSONInt(999)) { it.value }
            expect(JSONPointer("/xxx")) { it.key }
        }
    }

    @Test fun `should get optional Boolean`() {
        val json = JSON.parseObject("""{"aaa":true,"bbb":false,"xxx":999}""")
        val ref = JSONRef(json)
        expect(true) { ref.optionalBoolean("aaa") }
        expect(false) { ref.optionalBoolean("bbb") }
        assertNull(ref.optionalBoolean("ccc"))
        assertFailsWith<JSONTypeException> { ref.optionalBoolean("xxx") }.let {
            expect("Node not correct type (Boolean), was 999, at /xxx") { it.message }
            expect("Node") { it.nodeName }
            expect("Boolean") { it.target }
            expect(JSONInt(999)) { it.value }
            expect(JSONPointer("/xxx")) { it.key }
        }
    }

    @Test fun `should get optional Int`() {
        val json = JSON.parseObject("""{"aaa":123,"bbb":456,"xxx":"wrong"}""")
        val ref = JSONRef(json)
        expect(123) { ref.optionalInt("aaa") }
        expect(456) { ref.optionalInt("bbb") }
        assertNull(ref.optionalInt("ccc"))
        assertFailsWith<JSONTypeException> { ref.optionalInt("xxx") }.let {
            expect("Node not correct type (Int), was \"wrong\", at /xxx") { it.message }
            expect("Node") { it.nodeName }
            expect("Int") { it.target }
            expect(JSONString("wrong")) { it.value }
            expect(JSONPointer("/xxx")) { it.key }
        }
    }

    @Test fun `should get optional Long`() {
        val json = JSON.parseObject("""{"aaa":123,"bbb":123456789123456789,"xxx":"wrong"}""")
        val ref = JSONRef(json)
        expect(123) { ref.optionalLong("aaa") }
        expect(123456789123456789) { ref.optionalLong("bbb") }
        assertNull(ref.optionalLong("ccc"))
        assertFailsWith<JSONTypeException> { ref.optionalLong("xxx") }.let {
            expect("Node not correct type (Long), was \"wrong\", at /xxx") { it.message }
            expect("Node") { it.nodeName }
            expect("Long") { it.target }
            expect(JSONString("wrong")) { it.value }
            expect(JSONPointer("/xxx")) { it.key }
        }
    }

    @Test fun `should get optional Decimal`() {
        val json = JSON.parseObject("""{"aaa":123,"bbb":123456789123456789,"ccc":1.5,"xxx":"wrong"}""")
        val ref = JSONRef(json)
        expect(BigDecimal(123)) { ref.optionalDecimal("aaa") }
        expect(BigDecimal(123456789123456789)) { ref.optionalDecimal("bbb") }
        expect(BigDecimal("1.5")) { ref.optionalDecimal("ccc") }
        assertNull(ref.optionalDecimal("ddd"))
        assertFailsWith<JSONTypeException> { ref.optionalDecimal("xxx") }.let {
            expect("Node not correct type (Decimal), was \"wrong\", at /xxx") { it.message }
            expect("Node") { it.nodeName }
            expect("Decimal") { it.target }
            expect(JSONString("wrong")) { it.value }
            expect(JSONPointer("/xxx")) { it.key }
        }
    }

    @Test fun `should get optional child JSONString`() {
        val json = JSON.parseObject("""{"aaa":"OK"}""")
        val ref = JSONRef(json)
        val childRef = ref.optionalChild<JSONString>("aaa")
        assertNotNull(childRef)
        expect("OK") { childRef.value }
        assertNull(ref.optionalChild<JSONString>("bbb"))
    }

    @Test fun `should get optional child nullable JSONString`() {
        val json = JSON.parseObject("""{"aaa":"OK","bbb":null,"ccc":123}""")
        val ref = JSONRef(json)
        val childRef = ref.optionalChild<JSONString?>("aaa")
        assertNotNull(childRef)
        expect("OK") { childRef.node?.value }
        val nullChildRef = ref.optionalChild<JSONString?>("bbb")
        assertNotNull(nullChildRef)
        assertNull(nullChildRef.node)
        assertFailsWith<JSONTypeException> { ref.optionalChild<JSONString?>("ccc") }.let {
            expect("Child not correct type (JSONString?), was 123, at /ccc") { it.message }
            expect("Child") { it.nodeName }
            expect("JSONString?") { it.target }
            expect(JSONInt(123)) { it.value }
            expect(JSONPointer("/ccc")) { it.key }
        }
        assertNull(ref.optionalChild<JSONString?>("ddd"))
    }

    @Test fun `should get optional child object`() {
        val json = JSON.parseObject("""{"aaa":{"bbb":1000},"xxx":999}""")
        val ref = JSONRef(json)
        val childRef = ref.optionalChild<JSONObject>("aaa")
        assertNotNull(childRef)
        expect(1000) { childRef.node["bbb"].asInt }
        assertNull(ref.optionalChild<JSONObject>("ccc"))
        assertFailsWith<JSONTypeException> { ref.optionalChild<JSONObject>("xxx") }.let {
            expect("Child not correct type (JSONObject), was 999, at /xxx") { it.message }
            expect("Child") { it.nodeName }
            expect("JSONObject") { it.target }
            expect(JSONInt(999)) { it.value }
            expect(JSONPointer("/xxx")) { it.key }
        }
    }

    @Test fun `should get optional child array`() {
        val json = JSON.parseObject("""{"aaa":[888],"xxx":999}""")
        val ref = JSONRef(json)
        val childRef = ref.optionalChild<JSONArray>("aaa")
        assertNotNull(childRef)
        expect(888) { childRef.node[0].asInt }
        assertNull(ref.optionalChild<JSONArray>("ccc"))
        assertFailsWith<JSONTypeException> { ref.optionalChild<JSONArray>("xxx") }.let {
            expect("Child not correct type (JSONArray), was 999, at /xxx") { it.message }
            expect("Child") { it.nodeName }
            expect("JSONArray") { it.target }
            expect(JSONInt(999)) { it.value }
            expect(JSONPointer("/xxx")) { it.key }
        }
    }

    @Test fun `should perform operation with optional child`() {
        val json = JSON.parseObject("""{"aaa":[888],"xxx":999}""")
        val ref = JSONRef(json)
        ref.withOptionalChild<JSONArray>("aaa") {
            assertIs<JSONRef<JSONArray>>(this)
            expect(it) { node }
            expect(888) { it[0].asInt }
        }
        ref.withOptionalChild<JSONArray>("bbb") {
            fail("Should not get here")
        }
        assertFailsWith<JSONTypeException> {
            ref.withOptionalChild<JSONArray>("xxx") {
                fail("Definitely should not get here")
            }
        }.let {
            expect("Child not correct type (JSONArray), was 999, at /xxx") { it.message }
            expect("Child") { it.nodeName }
            expect("JSONArray") { it.target }
            expect(JSONInt(999)) { it.value }
            expect(JSONPointer("/xxx")) { it.key }
        }
    }

    @Test fun `should iterate over object`() {
        val json = JSON.parseObject("""{"a":1,"b":1,"c":2,"d":3,"e":5}""")
        val ref = JSONRef(json)
        val results = mutableListOf<Pair<String, Int>>()
        ref.forEachKey<JSONInt> {
            results.add(it to node.value)
        }
        expect(5) { results.size }
        expect("a" to 1) { results[0] }
        expect("b" to 1) { results[1] }
        expect("c" to 2) { results[2] }
        expect("d" to 3) { results[3] }
        expect("e" to 5) { results[4] }
    }

    @Test fun `should iterate over array`() {
        val json = JSON.parseArray("""["now","is","the","hour"]""")
        val ref = JSONRef(json)
        val results = mutableListOf<Pair<Int, String>>()
        ref.forEach<JSONString> {
            results.add(it to node.value)
        }
        expect(0 to "now") { results[0] }
        expect(1 to "is") { results[1] }
        expect(2 to "the") { results[2] }
        expect(3 to "hour") { results[3] }
    }

    @Test fun `should get untyped child of object`() {
        val ref = JSONRef(testObjectWithNull)
        val child = ref.untypedChild("field1")
        assertTrue(child.isRef<JSONInt>())
    }

    @Test fun `should get untyped child of array`() {
        val ref = JSONRef(testMixedArray)
        val child0 = ref.untypedChild(0)
        assertTrue(child0.isRef<JSONInt>())
        val child1 = ref.untypedChild(1)
        assertTrue(child1.isRef<JSONBoolean>())
    }

    @Test fun `should create a reference to a JSONObject`() {
        val ref = testObject.ref()
        assertTrue(ref.isRef<JSONObject>())
        assertSame(ref.base, testObject)
        assertSame(ref.node, testObject)
        expect(JSONPointer.root) { ref.pointer }
    }

    @Test fun `should create a reference to a JSONArray`() {
        val ref = testArray.ref()
        assertTrue(ref.isRef<JSONArray>())
        assertSame(ref.base, testArray)
        assertSame(ref.node, testArray)
        expect(JSONPointer.root) { ref.pointer }
    }

    @Test fun `should correctly report hasChild for object`() {
        val ref1 = JSONRef(testObject)
        assertTrue(ref1.hasChild<JSONInt>("field1"))
        assertTrue(ref1.hasChild<JSONArray>("field2"))
        assertFalse(ref1.hasChild<JSONInt>("field3"))
        assertFalse(ref1.hasChild<JSONString>("field1"))
    }

    @Test fun `should correctly report nullable hasChild for object`() {
        val ref = JSONRef(testObjectWithNull)
        assertTrue(ref.hasChild<JSONInt>("field1"))
        assertTrue(ref.hasChild<JSONInt?>("field1"))
        assertTrue(ref.hasChild<JSONInt?>("field3"))
        assertFalse(ref.hasChild<JSONInt>("field3"))
        assertFalse(ref.hasChild<JSONInt?>("field2"))
        assertFalse(ref.hasChild<JSONInt?>("field9"))
    }

    @Test fun `should correctly report hasChild for array`() {
        val ref1 = JSONRef(testMixedArray)
        assertTrue(ref1.hasChild<JSONInt>(0))
        assertTrue(ref1.hasChild<JSONNumber>(0))
        assertTrue(ref1.hasChild<JSONBoolean>(1))
        assertTrue(ref1.hasChild<JSONString>(2))
        assertTrue(ref1.hasChild<JSONArray>(3))
        assertFalse(ref1.hasChild<JSONInt>(1))
        assertFalse(ref1.hasChild<JSONValue>(4))
    }

    @Test fun `should correctly report nullable hasChild for array`() {
        val ref1 = JSONRef(testMixedArray)
        assertTrue(ref1.hasChild<JSONInt?>(0))
        assertTrue(ref1.hasChild<JSONNumber?>(0))
        assertTrue(ref1.hasChild<JSONBoolean?>(1))
        assertTrue(ref1.hasChild<JSONString?>(2))
        assertTrue(ref1.hasChild<JSONArray?>(3))
        assertTrue(ref1.hasChild<JSONInt?>(4))
        assertFalse(ref1.hasChild<JSONString?>(0))
    }

    @Test fun `should navigate to child of object`() {
        val ref1 = JSONRef(testObject)
        assertTrue(ref1.hasChild<JSONInt>("field1"))
        val ref2 = ref1.child<JSONInt>("field1")
        expect(123) { ref2.node.value }
        expect(JSONPointer("/field1")) { ref2.pointer }
    }

    @Test fun `should navigate to nullable child of object`() {
        val ref1 = JSONRef(testObjectWithNull)
        assertTrue(ref1.hasChild<JSONInt?>("field1"))
        val ref2 = ref1.child<JSONInt?>("field1")
        expect(123) { ref2.node?.value }
        expect(JSONPointer("/field1")) { ref2.pointer }
        assertTrue(ref1.hasChild<JSONInt?>("field3"))
        val ref3 = ref1.child<JSONInt?>("field3")
        assertNull(ref3.node)
    }

    @Test fun `should throw exception navigating to child of incorrect type`() {
        val ref1 = JSONRef(testObject)
        assertFailsWith<JSONTypeException> {
            ref1.child<JSONBoolean>("field1")
        }.let {
            expect("Child") { it.nodeName }
            expect("JSONBoolean") { it.target }
            expect(JSONInt(123)) { it.value }
            expect(JSONPointer("/field1")) { it.key }
            expect("Child not correct type (JSONBoolean), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should throw exception navigating to null child of non-nullable type`() {
        val ref1 = JSONRef(testObjectWithNull)
        assertFailsWith<JSONTypeException> { ref1.child<JSONInt>("field3") }.let {
            expect("Child") { it.nodeName }
            expect("JSONInt") { it.target }
            assertNull(it.value)
            expect(JSONPointer("/field3")) { it.key }
            expect("Child not correct type (JSONInt), was null, at /field3") { it.message }
        }
    }

    @Test fun `should throw exception navigating to child of incorrect nullable type`() {
        val ref1 = JSONRef(testObject)
        assertFailsWith<JSONTypeException> { ref1.child<JSONString?>("field1") }.let {
            expect("Child") { it.nodeName }
            expect("JSONString?") { it.target }
            expect(JSONInt(123)) { it.value }
            expect(JSONPointer("/field1")) { it.key }
            expect("Child not correct type (JSONString?), was 123, at /field1") { it.message }
        }
    }

    @Test fun `should navigate to child of array`() {
        val refArray = JSONRef(testMixedArray)
        assertTrue(refArray.hasChild<JSONInt>(0))
        with(refArray.child<JSONInt>(0)) {
            expect(123) { node.value }
            expect(JSONPointer("/0")) { pointer }
        }
    }

    @Test fun `should build JSONRef using infix operator`() {
        val ref1: JSONRef<JSONInt> = testObject ptr JSONPointer("/field1")
        expect(123) { ref1.node.value }
        val ref2: JSONRef<JSONString> = testObject ptr JSONPointer("/field2/1")
        expect("def") { ref2.node.value }
        val ref3 = ref2.parent<JSONArray>()
        assertSame(testObject["field2"], ref3.node)
        val ref4 = ref3.parent<JSONObject>()
        assertSame(testObject, ref4.node)
    }

}
