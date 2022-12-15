/*
 * @(#) JSONReference.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2021 Peter Wall
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
import io.kjson.JSON.toJSON

/**
 * A JSON Reference - a combination of a JSON Pointer and the JSON value to which it refers.  This allows for a single
 * object to be used (and passed as a parameter or a return value between functions) in the common case of a pointer
 * being employed to navigate a tree of JSON values.
 *
 * @author  Peter Wall
 */
class JSONReference internal constructor(val base: JSONValue?, tokens: Array<String>, val valid: Boolean,
        val value: JSONValue?) {

    constructor(base: JSONValue?) : this(base, emptyArray(), base != null, base)

    val pointer = JSONPointer(tokens)

    fun hasChild(name: String): Boolean = valid && value is JSONObject && value.containsKey(name)

    fun hasChild(index: Int): Boolean = valid &&
            ((value is JSONArray && index < value.size) || (value is JSONObject && value.containsKey(index.toString())))

    fun parent(): JSONReference {
        val tokens = pointer.tokens
        val len = tokens.size
        if (len == 0)
            JSONPointer.rootParentError()
        val newArray = Array(len - 1) { i -> tokens[i] }
        return JSONReference(base, newArray, true, JSONPointer.find(newArray, base))
    }

    fun child(name: String): JSONReference {
        val tokens = pointer.tokens
        val len = tokens.size
        val newArray = Array(len + 1) { i -> if (i < len) tokens[i] else name }
        return if (valid && value is JSONObject && value.containsKey(name))
            JSONReference(base, newArray, true, value[name])
        else
            JSONReference(base, newArray, false, null)
    }

    fun child(index: Int): JSONReference {
        if (index < 0)
            throw JSONPointerException("JSON Pointer index must not be negative")
        val tokens = pointer.tokens
        val len = tokens.size
        val name = index.toString()
        val newArray = Array(len + 1) { i -> if (i < len) tokens[i] else name }
        if (valid) {
            if (value is JSONArray) {
                if (index < value.size)
                    return JSONReference(base, newArray, true, value[index])
            }
            else if (value is JSONObject) {
                if (value.containsKey(name))
                    return JSONReference(base, newArray, true, value[name])
            }
        }
        return JSONReference(base, newArray, false, null)
    }

    fun locateChild(target: JSONValue?): JSONReference? {
        when {
            value === target -> return this
            value is JSONObject -> {
                for (key in value.keys) {
                    child(key).locateChild(target)?.let { return it }
                }
            }
            value is JSONArray -> {
                for (i in value.indices)
                    child(i).locateChild(target)?.let { return it }
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean = this === other || other is JSONReference &&
            base === other.base && valid == other.valid && value === other.value && pointer == other.pointer

    override fun hashCode(): Int = base.hashCode() xor valid.hashCode() xor value.hashCode() xor pointer.hashCode()

    override fun toString() = if (valid) value.toJSON() else "invalid"

}
