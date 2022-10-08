/*
 * @(#) ExtensionTest.kt
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
import kotlin.test.expect
import kotlin.test.fail

import io.kjson.JSON
import io.kjson.JSONInt
import io.kjson.JSONString

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

    @Test fun `should iterate over object`() {
        val json = JSON.parseObject("""{"a":1,"b":1,"c":2,"d":3,"e":5}""")
        val ref = JSONRef(json)
        val results = mutableListOf<Pair<String, Int>>()
        ref.forEachKey<JSONInt> {
            results.add(it to node.value)
        }
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

}
