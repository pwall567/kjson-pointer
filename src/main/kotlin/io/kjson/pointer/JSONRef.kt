/*
 * @(#) JSONRef.kt
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
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.typeOf

import io.kjson.JSON.typeError
import io.kjson.JSONArray
import io.kjson.JSONObject
import io.kjson.JSONStructure
import io.kjson.JSONValue

/**
 * A reference to a JSON value of a specified type.
 *
 * @author  Peter Wall
 */
class JSONRef<out J : JSONValue?> internal constructor(
    val base: JSONValue?,
    tokens: Array<String>,
    private val nodes: Array<JSONValue?>,
    val node: J,
) {

    constructor(base: J) : this(base, emptyArray(), emptyArray(), base)

    val pointer = JSONPointer(tokens)

    inline fun <reified T : JSONStructure<*>> parent(): JSONRef<T> = parent(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : JSONStructure<*>> parent(parentClass: KClass<T>): JSONRef<T> {
        val tokens = pointer.tokens
        val len = tokens.size - 1
        val newValue = when {
            len > 0 -> nodes[len - 1]
            len == 0 -> base
            else -> JSONPointer.rootParentError()
        }
        if (newValue == null || !newValue::class.isSubclassOf(parentClass))
            newValue.typeError(parentClass.simpleName ?: "unknown", pointer, nodeName = "Parent")
        return JSONRef(
            base = base,
            tokens = tokens.copyOfRange(0, len),
            nodes = nodes.copyOfRange(0, len),
            node = newValue
        ) as JSONRef<T>
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : JSONValue> createTypedChildRef(
        childClass: KClass<T>,
        nullable: Boolean,
        childNode: JSONValue?,
        token: String,
    ): JSONRef<T?> {
        if (childNode == null && !nullable || childNode != null && !childNode::class.isSubclassOf(childClass))
            childNode.typeError(childClass.refClassName(nullable), pointer.child(token), nodeName = "Child")
        return createChildRef(token, childNode as T?)
    }

    internal fun <T : JSONValue?> createChildRef(token: String, targetNode: T): JSONRef<T> = JSONRef(
        base = base,
        tokens = pointer.tokens + token,
        nodes = nodes + targetNode,
        node = targetNode,
    )

    @Suppress("UNCHECKED_CAST")
    fun <T : JSONValue> locateChild(target: T): JSONRef<T>? {
        when {
            node === target -> return this as JSONRef<T>
            node is JSONObject -> {
                val refObject = asRef<JSONObject>()
                for (key in node.keys)
                    refObject.child<JSONValue>(key).locateChild(target)?.let { return it }
            }
            node is JSONArray -> {
                val refArray = asRef<JSONArray>()
                for (i in node.indices)
                    refArray.child<JSONValue>(i).locateChild(target)?.let { return it }
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : JSONValue?> asRef(): JSONRef<T> {
        val type = typeOf<T>()
        return if (node.isType(type))
            this as JSONRef<T>
        else
            node.typeError(T::class.refClassName(type.isMarkedNullable), pointer)
    }

    inline fun <reified T : JSONValue?> isRef(): Boolean = node.isType(typeOf<T>())

    override fun equals(other: Any?): Boolean =
        this === other || other is JSONRef<*> && base === other.base && node === other.node && pointer == other.pointer

    override fun hashCode(): Int = base.hashCode() xor node.hashCode() xor pointer.hashCode()

    override fun toString() = "JSONRef<${node.nodeClass()}>(pointer=\"$pointer\",node=${node?.toJSON()})"

    private fun JSONValue?.nodeClass(): String = if (this == null) "JSONValue?" else this::class.simpleName ?: "unknown"

    companion object {

        fun KClass<*>.refClassName(nullable: Boolean): String = buildString {
            append(simpleName)
            if (nullable)
                append('?')
        }

        fun JSONValue?.isType(refType: KType): Boolean =
            if (this == null) refType.isMarkedNullable else this::class.isSubclassOf(refType.classifier as KClass<*>)

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
            val nodes = Array<JSONValue?>(len) { json }
            var node: JSONValue = json
            for (i in tokens.indices) {
                val token = tokens[i]
                when (node) {
                    is JSONObject -> {
                        if (!node.containsKey(token))
                            JSONPointer.pointerError("Node does not exist", tokens, i + 1)
                        node = node[token] ?: JSONPointer.pointerError("Node is null", tokens, i + 1)
                        nodes[i] = node
                    }
                    is JSONArray -> {
                        if (!JSONPointer.checkNumber(token) || token.toInt() >= node.size)
                            JSONPointer.pointerError("Node index incorrect", tokens, i + 1)
                        node = node[token.toInt()] ?: JSONPointer.pointerError("Node is null", tokens, i + 1)
                        nodes[i] = node
                    }
                    else -> JSONPointer.pointerError("Not an object or array", tokens, i)
                }
            }
            if (!node::class.isSubclassOf(refClass))
                node.typeError(refClass.simpleName ?: "unknown", pointer)
            return JSONRef(json, tokens, nodes, node) as JSONRef<T>
        }

    }

}
