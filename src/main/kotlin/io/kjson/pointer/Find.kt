/*
 * @(#) Find.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2024, 2025 Peter Wall
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

import io.kjson.JSON.typeError
import io.kjson.JSONArray
import io.kjson.JSONObject
import io.kjson.JSONValue

// Implementation note: The same algorithm for dereferencing a JSONPointer is implemented three times, because each
// requires a different result (the referenced element or throw an exception, the referenced element or null, or a
// boolean to indicate success0, and to use any one of the three to derive the others would be less efficient

/**
 * Test whether this [JSONPointer] references a valid location in the specified base value.
 */
infix fun JSONPointer.existsIn(json: JSONValue?): Boolean {
    var current: JSONValue? = json ?: return false
    for (i in 0 until depth) {
        val token = getToken(i)
        current = when (current) {
            is JSONObject -> {
                if (!current.containsKey(token))
                    return false
                current[token]
            }
            is JSONArray -> {
                if (!checkNumber(token))
                    return false
                val index = token.toInt()
                if (index < 0 || index >= current.size)
                    return false
                current[index]
            }
            else -> return false
        }
    }
    return true
}

/**
 * Find the [JSONValue] that this [JSONPointer] points to within the specified base value, or throw an exception
 * if the pointer does not reference a valid location in the base value.
 */
fun JSONPointer.find(base: JSONValue?): JSONValue? {
    var result = base
    for (i in 0 until depth) {
        val token = getToken(i)
        result = when (result) {
            null -> truncate(i).throwIntermediateNodeNull()
            is JSONObject -> {
                if (!result.containsKey(token))
                    truncate(i).throwInvalidPropertyName(token)
                result[token]
            }
            is JSONArray -> {
                if (token == "-")
                    truncate(i).throwCantDereferenceEndOfArrayPointer()
                val index = checkIndex(token, i)
                if (index < 0 || index >= result.size)
                    truncate(i).throwInvalidArrayIndex(index)
                result[index]
            }
            else -> truncate(i).throwIntermediateNodeNotObjectOrArray()
        }
    }
    return result
}

/**
 * Find the [JSONValue] that this [JSONPointer] points to within the specified base value, or `null` if the pointer
 * does not reference a valid location in the base value.
 */
fun JSONPointer.findOrNull(base: JSONValue?): JSONValue? {
    var result = base
    for (i in 0 until depth) {
        val token = getToken(i)
        result = when (result) {
            is JSONObject -> {
                if (!result.containsKey(token))
                    return null
                result[token]
            }
            is JSONArray -> {
                if (!checkNumber(token))
                    return null
                val index = token.toInt()
                if (index < 0 || index >= result.size)
                    return null
                result[index]
            }
            else -> return null
        }
    }
    return result
}

/**
 * Find the [JSONObject] that this [JSONPointer] points to within the specified base value, or throw an exception
 * if the pointer does not reference a valid location in the base value or if the value referenced is not a
 * [JSONObject].
 */
fun JSONPointer.findObject(base: JSONValue?): JSONObject = findOrNull(base).let {
    it as? JSONObject ?: it.typeError("JSONObject", this)
}

/**
 * Find the [JSONArray] that this [JSONPointer] points to within the specified base value, or throw an exception
 * if the pointer does not reference a valid location in the base value or if the value referenced is not a
 * [JSONArray].
 */
fun JSONPointer.findArray(base: JSONValue?): JSONArray = findOrNull(base).let {
    it as? JSONArray ?: it.typeError("JSONArray", this)
}

private fun JSONPointer.checkIndex(token: String, tokenIndex: Int): Int {
    if (!checkNumber(token))
        throw JSONPointerException("Illegal array index \"$token\" in JSON Pointer", truncate(tokenIndex))
    return token.toInt()
}

internal fun JSONPointer.throwInvalidPropertyName(propertyName: String): Nothing {
    throw JSONPointerException("Can't locate JSON property \"$propertyName\"", this)
}

internal fun JSONPointer.throwInvalidArrayIndex(index: Int): Nothing {
    throw JSONPointerException("Array index $index out of range in JSON Pointer", this)
}

internal fun JSONPointer.throwIntermediateNodeNull(): Nothing {
    throw throw JSONPointerException("Intermediate node is null", this)
}

internal fun JSONPointer.throwIntermediateNodeNotObjectOrArray(): Nothing {
    throw throw JSONPointerException("Intermediate node is not object or array", this)
}

internal fun JSONPointer.throwCantDereferenceEndOfArrayPointer(): Nothing {
    throw throw JSONPointerException("Can't dereference end-of-array JSON Pointer", this)
}

internal fun checkNumber(token: String): Boolean {
    val len = token.length
    if (len < 1 || len > 8)
        return false
    var ch = token[0]
    if (ch == '0')
        return len == 1
    var i = 1
    while (true) {
        if (ch !in '0'..'9')
            return false
        if (i >= len)
            return true
        ch = token[i++]
    }
}

/**
 * Locate the specified target [JSONValue] in the base [JSONValue], and return a [JSONPointer] pointing to it, based
 * on the current pointer.  Returns `null` if not found.
 *
 * This will perform a depth-first search of the JSON structure, and because it is not possible to distinguish between
 * two instances of, say, `JSONBoolean.TRUE`, the function may not be successful in locating a primitive value.
 */
fun JSONPointer.locateChild(base: JSONValue?, target: JSONValue?): JSONPointer? {
    when (base) {
        is JSONObject -> {
            if (base === target)
                return this
            for (key in base.keys) {
                child(key).locateChild(base[key], target)?.let { return it }
            }
        }
        is JSONArray -> {
            if (base === target)
                return this
            for (i in base.indices)
                child(i).locateChild(base[i], target)?.let { return it }
        }
        target -> return this
        else -> {}
    }
    return null
}
