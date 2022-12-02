/*
 * @(#) SampleJSON.kt
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

package io.kjson.pointer.test

import io.kjson.JSON
import io.kjson.JSONString

object SampleJSON {

    val testString = JSONString("test1")
    val testObject = JSON.parseObject("""{"field1":123,"field2":["abc","def"],"field3":true}""")
    val testArray = JSON.parseArray("""["A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P"]""")
    val testMixedArray = JSON.parseArray("""[123,true,"cat",[]]""")
    val testNestedObject = JSON.parseObject("""{"field1":123,"field2":{"aaa":99,"bbb":[1,1,2,3,5]}}""")

}
