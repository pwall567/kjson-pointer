/*
 * @(#) Extension.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2022, 2023, 2024, 2025 Peter Wall
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

import io.kjson.JSONArray
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.JSONBoolean
import io.kjson.JSONNumber
import io.kjson.JSONPrimitive
import io.kjson.JSON.typeError

/**
 * Create a [JSONReference] using this [JSONPointer] and the specified [JSONValue] base.
 */
infix fun JSONPointer.ref(base: JSONValue?) = if (existsIn(base)) JSONReference(base, this, true, find(base))
        else JSONReference(base, this, false, null)

/**
 * Create a [JSONRef] from `this` [JSONValue] and the specified [JSONPointer].
 */
inline infix fun <reified T : JSONValue> JSONValue.ptr(pointer: JSONPointer): JSONRef<T> = JSONRef.of(this, pointer)

/**
 * Conditionally execute if [JSONObject] referenced by `this` [JSONRef] contains a member with the specified key and the
 * expected type.
 *
 * **NOTE:** this function will not throw an exception if the property is present but is of the wrong type, and it may
 * be removed from future releases.  To achieve the same effect with strong type checking, use:
 * ```kotlin
 *     ref.optionalChild<JSONObject>("name")?.let { doSomething(it) }
 * ```
 */
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.ifPresent(name: String, block: JSONRef<T>.(T) -> Unit) {
    if (hasChild<T>(name))
        child<T>(name).let { it.block(it.node) }
}

/**
 * Map the values of the [JSONArray] referenced by `this` [JSONRef] to an array of the primitive type for the
 * [JSONValue].
 */
inline fun <reified T : JSONPrimitive<R>, R : Any> JSONRef<JSONArray>.map(): List<R> =
        List(node.size) { index -> child<T>(index).node.value }

/**
 * Map the values of the [JSONArray] referenced by `this` [JSONRef] to an array of the target type, applying a
 * transformation to each item.
 */
inline fun <reified T : JSONValue?, R> JSONRef<JSONArray>.map(transform: JSONRef<T>.(Int) -> R) : List<R> =
        List(node.size) { index -> child<T>(index).transform(index) }

/**
 * Return `true` if any of the values of the [JSONArray] referenced by `this` [JSONRef] satisfy a given predicate.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONArray>.any(predicate: JSONRef<T>.(Int) -> Boolean) : Boolean =
        node.indices.any { child<T>(it).predicate(it) }

/**
 * Return `true` if all of the values of the [JSONArray] referenced by `this` [JSONRef] satisfy a given predicate.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONArray>.all(predicate: JSONRef<T>.(Int) -> Boolean) : Boolean =
        node.indices.all { child<T>(it).predicate(it) }

/**
 * Map the [JSONObject] property referenced by `this` [JSONRef] and the specified key, using the provided mapping
 * function.
 */
@Deprecated("Confusing function name", ReplaceWith("child(name).apply(block)"))
inline fun <reified T : JSONValue?, R : Any> JSONRef<JSONObject>.map(name: String, block: JSONRef<T>.(T) -> R): R =
    child<T>(name).let { it.block(it.node) }

/**
 * Map the [JSONObject] property referenced by `this` [JSONRef] and the specified key, using the provided mapping
 * function, returning `null` if property not present.
 *
 * **NOTE:** this function will not throw an exception if the property is present but is of the wrong type, and it has
 * been deprecated.  To achieve the same effect with strong type checking, use:
 * ```kotlin
 *     ref.optionalChild<JSONObject>("name")?.let { doSomething(it) }
 * ```
 */
@Deprecated("Does not check property type", ReplaceWith("optionalChild(name)?.apply(block)"))
inline fun <reified T : JSONValue?, R : Any> JSONRef<JSONObject>.mapIfPresent(name: String,
        block: JSONRef<T>.(T) -> R): R? = if (hasChild<T>(name)) child<T>(name).let { it.block(it.node) } else null

/**
 * Get a [String] property from a [JSONObject] using `this` [JSONRef] and the specified key (throws an exception if the
 * property is not present or is the wrong type).
 */
fun JSONRef<JSONObject>.childString(name: String): String = if (node.containsKey(name)) node[name].let {
    if (it is JSONString) it.value else it.typeError("String", pointer.child(name))
} else pointer.throwInvalidPropertyName(name)

/**
 * Get a [String] property from a [JSONObject] using `this` [JSONRef] and the specified key, or `null` if the property
 * is not present (throws an exception if the property is present but is the wrong type).
 */
fun JSONRef<JSONObject>.optionalString(name: String): String? = node[name]?.let {
    if (it is JSONString) it.value else it.typeError("String", pointer.child(name))
}

/**
 * Get a [Boolean] property from a [JSONObject] using `this` [JSONRef] and the specified key (throws an exception if the
 * property is not present or is the wrong type).
 */
fun JSONRef<JSONObject>.childBoolean(name: String): Boolean = if (node.containsKey(name)) node[name].let {
    if (it is JSONBoolean) it.value else it.typeError("Boolean", pointer.child(name))
} else pointer.throwInvalidPropertyName(name)

/**
 * Get a [Boolean] property from a [JSONObject] using `this` [JSONRef] and the specified key, or `null` if the property
 * is not present (throws an exception if the property is present but is the wrong type).
 */
fun JSONRef<JSONObject>.optionalBoolean(name: String): Boolean? = node[name]?.let {
    if (it is JSONBoolean) it.value else it.typeError("Boolean", pointer.child(name))
}

/**
 * Get an [Int] property from a [JSONObject] using `this` [JSONRef] and the specified key (throws an exception if the
 * property is not present or is the wrong type).
 */
fun JSONRef<JSONObject>.childInt(name: String): Int = if (node.containsKey(name)) node[name].let {
    if (it is JSONNumber && it.isInt()) it.toInt() else it.typeError("Int", pointer.child(name))
} else pointer.throwInvalidPropertyName(name)

/**
 * Get an [Int] property from a [JSONObject] using `this` [JSONRef] and the specified key, or `null` if the property is
 * not present (throws an exception if the property is present but is the wrong type).
 */
fun JSONRef<JSONObject>.optionalInt(name: String): Int? = node[name]?.let {
    if (it is JSONNumber && it.isInt()) it.toInt() else it.typeError("Int", pointer.child(name))
}

/**
 * Get a [Long] property from a [JSONObject] using `this` [JSONRef] and the specified key (throws an exception if the
 * property is not present or is the wrong type).
 */
fun JSONRef<JSONObject>.childLong(name: String): Long = if (node.containsKey(name)) node[name].let {
    if (it is JSONNumber && it.isLong()) it.toLong() else it.typeError("Long", pointer.child(name))
} else pointer.throwInvalidPropertyName(name)

/**
 * Get a [Long] property from a [JSONObject] using `this` [JSONRef] and the specified key, or `null` if the property is
 * not present (throws an exception if the property is present but is not an [Int] or [Long]).
 */
fun JSONRef<JSONObject>.optionalLong(name: String): Long? = node[name]?.let {
    if (it is JSONNumber && it.isLong()) it.toLong() else it.typeError("Long", pointer.child(name))
}

/**
 * Get a [BigDecimal] property from a [JSONObject] using `this` [JSONRef] and the specified key (throws an exception if
 * the property is not present or is the wrong type).
 */
fun JSONRef<JSONObject>.childDecimal(name: String): BigDecimal = if (node.containsKey(name)) node[name].let {
    if (it is JSONNumber) it.toDecimal() else it.typeError("Decimal", pointer.child(name))
} else pointer.throwInvalidPropertyName(name)

/**
 * Get a [BigDecimal] property from a [JSONObject] using `this` [JSONRef] and the specified key, or `null` if the
 * property is not present (throws an exception if the property is present but is the wrong type).
 */
fun JSONRef<JSONObject>.optionalDecimal(name: String): BigDecimal? = node[name]?.let {
    if (it is JSONNumber) it.toDecimal() else it.typeError("Decimal", pointer.child(name))
}

/**
 * Get a child property [JSONRef] from a [JSONObject] using `this` [JSONRef] and the specified key, or `null` if the
 * property is not present (throws an exception if the property is present but is the wrong type).
 */
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.optionalChild(name: String): JSONRef<T>? =
    if (node.containsKey(name)) createTypedChildRef<T>(name, node[name]) else null

/**
 * Execute the supplied lambda if the nominated child property of `this` [JSONRef] exists.  Within the lambda, `this` is
 * a [JSONRef] pointing to the child, and `it` is the child itself.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.withOptionalChild(name: String, block: JSONRef<T>.(T) -> Unit) {
    if (node.containsKey(name))
        createTypedChildRef<T>(name, node[name]).let { it.block(it.node) }
}

/**
 * Iterate over the members of the [JSONObject] referenced by `this` [JSONRef].  Within the lambda, `this` is a
 * [JSONRef] pointing to the value, and `it` is the key.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.forEachKey(block: JSONRef<T>.(String) -> Unit) {
    node.entries.forEach { createTypedChildRef<T>(it.key, it.value).block(it.key) }
}

/**
 * Iterate over the members of the [JSONArray] referenced by `this` [JSONRef].  Within the lambda, `this` is a [JSONRef]
 * pointing to the array item, and `it` is the index.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONArray>.forEach(block: JSONRef<T>.(Int) -> Unit) {
    node.indices.forEach { createTypedChildRef<T>(it.toString(), node[it]).block(it) }
}

/**
 * Get the named child reference (strongly typed) from this [JSONObject] reference, using the implied child type.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.child(name: String): JSONRef<T> {
    checkName(name)
    return createTypedChildRef(name, node[name])
}

/**
 * Get the named child reference (strongly typed) from this [JSONArray] reference, using the implied child type.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONArray>.child(index: Int): JSONRef<T> {
    checkIndex(index)
    return createTypedChildRef(index.toString(), node[index])
}

/**
 * Get the named child reference (untyped) from this [JSONObject] reference.
 */
fun JSONRef<JSONObject>.untypedChild(name: String): JSONRef<JSONValue?> {
    checkName(name)
    return createChildRef(name, node[name])
}

/**
 * Get the named child reference (untyped) from this [JSONArray] reference.
 */
fun JSONRef<JSONArray>.untypedChild(index: Int): JSONRef<JSONValue?> {
    checkIndex(index)
    return createChildRef(index.toString(), node[index])
}

/**
 * Test whether this [JSONObject] reference has the named child with the implied type.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.hasChild(name: String): Boolean =
        node.containsKey(name) && node[name] is T

/**
 * Test whether this [JSONArray] reference has a child at the given index with the implied type.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONArray>.hasChild(index: Int): Boolean =
        index >= 0 && index < node.size && node[index] is T

/**
 * Check that this [JSONObject] reference has the named child, and throw an exception if not.
 */
fun JSONRef<JSONObject>.checkName(name: String) {
    if (!node.containsKey(name))
        pointer.throwInvalidPropertyName(name)
}

/**
 * Check that this [JSONArray] reference has a child at the given index, and throw an exception if not.
 */
fun JSONRef<JSONArray>.checkIndex(index: Int) {
    if (index !in node.indices)
        pointer.throwInvalidArrayIndex(index)
}

/**
 * Create a reference to this [JSONValue].
 */
fun <T : JSONValue?> T.ref(): JSONRef<T> = JSONRef(this)

/** The value of the node. */
val <T> JSONRef<JSONPrimitive<T>>.value: T
    get() = node.value
