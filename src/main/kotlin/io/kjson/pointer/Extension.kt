/*
 * @(#) Extension.kt
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

import io.kjson.JSONArray
import io.kjson.JSONObject
import io.kjson.JSONValue

/**
 * Create a [JSONRef] from `this` [JSONValue] and the specified [JSONPointer].
 */
inline infix fun <reified T : JSONValue> JSONValue.ptr(pointer: JSONPointer): JSONRef<T> =
        JSONRef.of(T::class, this, pointer)

/**
 * Conditionally execute if [JSONObject] referenced by `this` [JSONRef] contains a member with the specified key and the
 * expected type.
 */
inline fun <reified T : JSONValue> JSONRef<JSONObject>.ifPresent(name: String, block: JSONRef<T>.(T) -> Unit) {
    if (hasChild<T>(name))
        child<T>(name).let { it.block(it.node) }
}

/**
 * Iterate over the members of the [JSONObject] referenced by `this` [JSONRef].
 */
inline fun <reified T : JSONValue> JSONRef<JSONObject>.forEachKey(block: JSONRef<T>.(String) -> Unit) {
    node.keys.forEach { child<T>(it).block(it) }
}

/**
 * Iterate over the members of the [JSONArray] referenced by `this` [JSONRef].
 */
inline fun <reified T : JSONValue> JSONRef<JSONArray>.forEach(block: JSONRef<T>.(Int) -> Unit) {
    node.indices.forEach { child<T>(it).block(it) }
}
