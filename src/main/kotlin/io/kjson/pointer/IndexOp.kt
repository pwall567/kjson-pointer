/*
 * @(#) IndexOp.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2022, 2023 Peter Wall
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

import java.math.BigDecimal

import io.kjson.JSON.asArrayOrNull
import io.kjson.JSON.asBooleanOrNull
import io.kjson.JSON.asByteOrNull
import io.kjson.JSON.asDecimalOrNull
import io.kjson.JSON.asIntOrNull
import io.kjson.JSON.asLongOrNull
import io.kjson.JSON.asObjectOrNull
import io.kjson.JSON.asShortOrNull
import io.kjson.JSON.asStringOrNull
import io.kjson.JSON.asUByteOrNull
import io.kjson.JSON.asUIntOrNull
import io.kjson.JSON.asULongOrNull
import io.kjson.JSON.asUShortOrNull
import io.kjson.JSON.typeError
import io.kjson.JSONArray
import io.kjson.JSONObject
import io.kjson.JSONValue

/**
 * Get the entry within `this` [JSONValue] using the specified [JSONPointer].
 */
operator fun JSONValue?.get(pointer: JSONPointer): JSONValue? = if (pointer existsIn this) pointer.find(this) else null

/**
 * Test whether the specified [JSONPointer] points to a valid entry in `this` [JSONValue].
 */
operator fun JSONValue?.contains(pointer: JSONPointer) = pointer existsIn this

/**
 * Get a `String` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getString(pointer: JSONPointer): String =
        pointer.find(this).let { it.asStringOrNull ?: it.typeError("String", pointer) }

/**
 * Get a `Long` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getLong(pointer: JSONPointer): Long =
        pointer.find(this).let { it.asLongOrNull ?: it.typeError("Long", pointer) }

/**
 * Get an `Int` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getInt(pointer: JSONPointer): Int =
        pointer.find(this).let { it.asIntOrNull ?: it.typeError("Int", pointer) }

/**
 * Get a `Short` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getShort(pointer: JSONPointer): Short =
        pointer.find(this).let { it.asShortOrNull ?: it.typeError("Short", pointer) }

/**
 * Get a `Byte` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getByte(pointer: JSONPointer): Byte =
        pointer.find(this).let { it.asByteOrNull ?: it.typeError("Byte", pointer) }

/**
 * Get a `ULong` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getULong(pointer: JSONPointer): ULong =
        pointer.find(this).let { it.asULongOrNull ?: it.typeError("ULong", pointer) }

/**
 * Get a `UInt` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getUInt(pointer: JSONPointer): UInt =
        pointer.find(this).let { it.asUIntOrNull ?: it.typeError("UInt", pointer) }

/**
 * Get a `UShort` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getUShort(pointer: JSONPointer): UShort =
        pointer.find(this).let { it.asUShortOrNull ?: it.typeError("UShort", pointer) }

/**
 * Get a `UByte` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getUByte(pointer: JSONPointer): UByte =
        pointer.find(this).let { it.asUByteOrNull ?: it.typeError("UByte", pointer) }

/**
 * Get a `Decimal` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getDecimal(pointer: JSONPointer): BigDecimal =
        pointer.find(this).let { it.asDecimalOrNull ?: it.typeError("BigDecimal", pointer) }

/**
 * Get a `Boolean` from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getBoolean(pointer: JSONPointer): Boolean =
        pointer.find(this).let { it.asBooleanOrNull ?: it.typeError("Boolean", pointer) }

/**
 * Get an array from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getArray(pointer: JSONPointer): JSONArray =
        pointer.find(this).let { it.asArrayOrNull ?: it.typeError("JSONArray", pointer) }

/**
 * Get an object from a [JSONValue] using the specified [JSONPointer].
 */
fun JSONValue.getObject(pointer: JSONPointer): JSONObject =
        pointer.find(this).let { it.asObjectOrNull ?: it.typeError("JSONObject", pointer) }
