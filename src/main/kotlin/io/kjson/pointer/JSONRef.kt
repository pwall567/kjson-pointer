/*
 * @(#) JSONRef.kt
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

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

import io.kjson.JSON.typeError
import io.kjson.JSONArray
import io.kjson.JSONObject
import io.kjson.JSONStructure
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer.Companion.pointerError

/**
 * A reference to a JSON value of a specified type.
 *
 * @author  Peter Wall
 */
class JSONRef<out J : JSONValue> internal constructor(
    val base: JSONValue,
    tokens: Array<String>,
    private val nodes: Array<JSONValue>,
    val node: J,
) {

    constructor(base: J) : this(base, emptyArray(), emptyArray(), base)

    val pointer = JSONPointer(tokens)

    inline fun <reified T : JSONValue> hasChild(name: String): Boolean = node is JSONObject && node[name] is T

    inline fun <reified T : JSONValue> hasChild(index: Int): Boolean =
        (node is JSONArray && index < node.size && node[index] is T)

    inline fun <reified T : JSONStructure<*>> parent(): JSONRef<T> = parent(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : JSONStructure<*>> parent(parentClass: KClass<T>): JSONRef<T> {
        val tokens = pointer.tokens
        val len = tokens.size - 1
        val newValue = when {
            len > 0 -> nodes[len - 1]
            len == 0 -> base
            else -> throw JSONPointerException("Can't get parent of root JSON Pointer")
        }
        if (!newValue::class.isSubclassOf(parentClass))
            newValue.typeError(parentClass, pointer, nodeName = "Parent")
        return JSONRef(
            base = base,
            tokens = Array(len) { i -> tokens[i] },
            nodes = Array(len) { i -> nodes[i] },
            node = newValue
        ) as JSONRef<T>
    }

    inline fun <reified T : JSONValue> child(name: String): JSONRef<T> = child(T::class, name)

    fun <T : JSONValue> child(childClass: KClass<T>, name: String): JSONRef<T> {
        if (node !is JSONObject)
            pointerError("Not an object", pointer.toString())
        if (!node.containsKey(name))
            pointerError("Node does not exist", "$pointer/$name")
        return createChild(childClass, node[name], name)
    }

    inline fun <reified T : JSONValue> child(index: Int): JSONRef<T> = child(T::class, index)

    fun <T : JSONValue> child(childClass: KClass<T>, index: Int): JSONRef<T> {
        if (node !is JSONArray)
            pointerError("Not an array", pointer.toString())
        if (index !in node.indices)
            pointerError("Index not valid", "$pointer/$index")
        return createChild(childClass, node[index], index.toString())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : JSONValue> createChild(childClass: KClass<T>, node: JSONValue?, token: String): JSONRef<T> {
        if (node == null || !node::class.isSubclassOf(childClass))
            node.typeError(childClass, pointer.child(token), nodeName = "Child")
        val tokens = pointer.tokens
        val len = tokens.size
        return JSONRef(
            base = base,
            tokens = Array(len + 1) { i -> if (i < len) tokens[i] else token },
            nodes = Array(len + 1) { i -> if (i < len) nodes[i] else node },
            node = node,
        ) as JSONRef<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : JSONValue> locateChild(target: T): JSONRef<T>? {
        when {
            node === target -> return this as JSONRef<T>
            node is JSONObject -> {
                for (key in node.keys)
                    child<JSONValue>(key).locateChild(target)?.let { return it }
            }
            node is JSONArray -> {
                for (i in node.indices)
                    child<JSONValue>(i).locateChild(target)?.let { return it }
            }
        }
        return null
    }

    inline fun <reified T : JSONValue> asRef(): JSONRef<T> = asRef(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : JSONValue> asRef(refClass: KClass<T>): JSONRef<T> {
        if (!node::class.isSubclassOf(refClass))
            node.typeError(refClass, pointer)
        return this as JSONRef<T>
    }

    inline fun <reified T : JSONValue> isRef(): Boolean = isRef(T::class)

    fun <T : JSONValue> isRef(refClass: KClass<T>): Boolean {
        return node::class.isSubclassOf(refClass)
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is JSONRef<*> && base === other.base && node === other.node && pointer == other.pointer

    override fun hashCode(): Int = base.hashCode() xor node.hashCode() xor pointer.hashCode()

    override fun toString() = "JSONRef<${node::class.simpleName}>(pointer=\"$pointer\",node=${node.toJSON()})"

    companion object {

        fun <T : JSONValue> of(json: T): JSONRef<T> = JSONRef(json)

        inline fun <reified T : JSONValue> of(json: JSONValue, pointer: String): JSONRef<T> =
            of(T::class, json, JSONPointer(pointer))

        inline fun <reified T : JSONValue> of(json: JSONValue, pointer: JSONPointer): JSONRef<T> =
            of(T::class, json, pointer)

        fun <T : JSONValue> of(refClass: KClass<T>, json: JSONValue, pointer: String): JSONRef<T> =
            of(refClass, json, JSONPointer(pointer))

        @Suppress("UNCHECKED_CAST")
        fun <T : JSONValue> of(refClass: KClass<T>, json: JSONValue, pointer: JSONPointer): JSONRef<T> {
            val tokens = pointer.tokens
            val len = tokens.size
            val nodes = Array(len) { json }
            var node: JSONValue = json
            for (i in tokens.indices) {
                val token = tokens[i]
                when (node) {
                    is JSONObject -> {
                        if (!node.containsKey(token))
                            pointerError("Node does not exist", tokens, i + 1)
                        node = node[token] ?: pointerError("Node is null", tokens, i + 1)
                        nodes[i] = node
                    }
                    is JSONArray -> {
                        if (!JSONPointer.checkNumber(token) || token.toInt() >= node.size)
                            pointerError("Node index incorrect", tokens, i + 1)
                        node = node[token.toInt()] ?: pointerError("Node is null", tokens, i + 1)
                        nodes[i] = node
                    }
                    else -> pointerError("Not an object or array", tokens, i)
                }
            }
            if (!node::class.isSubclassOf(refClass))
                node.typeError(refClass, pointer)
            return JSONRef(json, tokens, nodes, node) as JSONRef<T>
        }

    }

}
