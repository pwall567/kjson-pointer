/*
 * @(#) Extension.kt
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

import kotlin.reflect.KClass
import kotlin.reflect.typeOf

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
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.ifPresent(name: String, block: JSONRef<T>.(T) -> Unit) {
    if (hasChild<T>(name))
        child<T>(name).let { it.block(it.node) }
}

/**
 * Map the [JSONObject] property referenced by `this` [JSONRef] and the specified key, using the provided mapping
 * function, returning `null` if property not present.
 */
inline fun <reified T : JSONValue?, R : Any> JSONRef<JSONObject>.mapIfPresent(name: String,
        block: JSONRef<T>.(T) -> R): R? = if (hasChild<T>(name)) child<T>(name).let { it.block(it.node) } else null

/**
 * Iterate over the members of the [JSONObject] referenced by `this` [JSONRef].
 */
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.forEachKey(block: JSONRef<T>.(String) -> Unit) {
    node.keys.forEach { child<T>(it).block(it) }
}

/**
 * Iterate over the members of the [JSONArray] referenced by `this` [JSONRef].
 */
inline fun <reified T : JSONValue?> JSONRef<JSONArray>.forEach(block: JSONRef<T>.(Int) -> Unit) {
    node.indices.forEach { child<T>(it).block(it) }
}

/**
 * Get the named child reference (strongly typed) from this [JSONObject] reference, using the implied child type.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified Q : JSONValue?> JSONRef<JSONObject>.child(name: String): JSONRef<Q> {
    val type = typeOf<Q>()
    val childClass = type.classifier as KClass<JSONValue>
    return child(childClass, name, type.isMarkedNullable) as JSONRef<Q>
}

/**
 * Get the named child reference (strongly typed) from this [JSONObject] reference, using the supplied child class and
 * nullability.
 */
fun <T : JSONValue> JSONRef<JSONObject>.child(
    childClass: KClass<T>,
    name: String,
    nullable: Boolean = false,
): JSONRef<T?> {
    checkName(name)
    return createTypedChildRef(childClass, nullable, node[name], name)
}

/**
 * Get the named child reference (strongly typed) from this [JSONArray] reference, using the implied child type.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified Q : JSONValue?> JSONRef<JSONArray>.child(index: Int): JSONRef<Q> {
    val type = typeOf<Q>()
    val childClass = type.classifier as KClass<JSONValue>
    return child(childClass, index, type.isMarkedNullable) as JSONRef<Q>
}

/**
 * Get the named child reference (strongly typed) from this [JSONArray] reference, using the supplied child class and
 * nullability.
 */
fun <T : JSONValue> JSONRef<JSONArray>.child(
    childClass: KClass<T>,
    index: Int,
    nullable: Boolean = false,
): JSONRef<T?> {
    checkIndex(index)
    return createTypedChildRef(childClass, nullable, node[index], index.toString())
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
 * Test whether this [JSONObject] reference has the named child.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONObject>.hasChild(name: String): Boolean =
        node.containsKey(name) && node[name] is T

/**
 * Test whether this [JSONArray] reference has a child at the given index.
 */
inline fun <reified T : JSONValue?> JSONRef<JSONArray>.hasChild(index: Int): Boolean =
        index >= 0 && index < node.size && node[index] is T

internal fun JSONRef<JSONObject>.checkName(name: String) {
    if (!node.containsKey(name))
        JSONPointer.pointerError("Node does not exist", "$pointer/$name")
}

internal fun JSONRef<JSONArray>.checkIndex(index: Int) {
    if (index !in node.indices)
        JSONPointer.pointerError("Index not valid", "$pointer/$index")
}
