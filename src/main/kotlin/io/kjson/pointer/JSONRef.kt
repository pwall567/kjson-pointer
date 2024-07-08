/*
 * @(#) JSONRef.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2022, 2023, 2024 Peter Wall
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
    val pointer: JSONPointer,
    private val nodes: Array<JSONValue?>,
    val node: J,
) {

    constructor(base: J) : this(base, JSONPointer.root, emptyArray(), base)

    /**
     * Get the parent reference of this reference.
     */
    inline fun <reified T : JSONStructure<*>> parent(): JSONRef<T> = parent { parentNode ->
        if (parentNode !is T)
            parentNode.typeError(typeOf<T>().refClassName(), pointer.parent(), nodeName = "Parent")
        parentNode
    }

    /**
     * Get the parent reference of this reference (using a supplied checking function to confirm the type).
     */
    fun <T : JSONStructure<*>> parent(checkType: (JSONValue?) -> T): JSONRef<T> {
        val len = pointer.depth - 1
        val parentNode = checkType(when {
            len > 0 -> nodes[len - 1]
            len == 0 -> base
            else -> JSONPointer.throwRootParentError()
        })
        return JSONRef(
            base = base,
            pointer = pointer.parent(),
            nodes = if (len == 0) emptyArray() else nodes.copyOfRange(0, len),
            node = parentNode,
        )
    }

    /**
     * Create a child reference, checking the type of the target node.
     */
    inline fun <reified T : JSONValue?> createTypedChildRef(token: String, targetNode: JSONValue?): JSONRef<T> =
        if (targetNode is T)
            createChildRef(token, targetNode)
        else
            targetNode.typeError(typeOf<T>().refClassName(), pointer.child(token), "Child")

    /**
     * Create a child reference.
     */
    fun <T : JSONValue?> createChildRef(token: String, targetNode: T): JSONRef<T> = JSONRef(
        base = base,
        pointer = pointer.child(token),
        nodes = nodes + targetNode,
        node = targetNode,
    )

    /**
     * Locate the specified target [JSONValue] in the structure below this reference and return a reference to it, or
     * `null` if not found.
     *
     * This will perform a depth-first search of the JSON structure.
     */
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

    /**
     * "Downcast" a reference to a particular type, or throw an exception if the target is not of that type.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : JSONValue?> asRef(nodeName: String = "Node"): JSONRef<T> = if (node is T)
        this as JSONRef<T>
    else
        node.typeError(typeOf<T>().refClassName(), pointer, nodeName)

    /**
     * Test whether reference refers to a nominated type.
     */
    inline fun <reified T : JSONValue?> isRef(): Boolean = node is T

    override fun equals(other: Any?): Boolean = this === other ||
            other is JSONRef<*> && base === other.base && node === other.node && pointer == other.pointer

    override fun hashCode(): Int = base.hashCode() xor node.hashCode() xor pointer.hashCode()

    override fun toString() = pointer.toString()

    companion object {

        /**
         * Get the class name of the reference class for error messages (this function is public only because it is
         * required by public inline functions).
         */
        fun KType.refClassName(): String  {
            val name = (classifier as KClass<*>).simpleName ?: "Unknown"
            return if (isMarkedNullable) "$name?" else name
        }

        /**
         * Create a strongly-typed reference to the base of the given JSON.
         */
        fun <T : JSONValue?> of(json: T): JSONRef<T> = JSONRef(json)

        /**
         * Create a strongly-typed reference using the given base JSON and a pointer in `String` form.
         */
        inline fun <reified T : JSONValue?> of(json: JSONValue?, pointer: String): JSONRef<T> =
                of(json, JSONPointer(pointer))

        /**
         * Create a strongly-typed reference using the given base JSON and pointer.
         */
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : JSONValue?> of(json: JSONValue?, pointer: JSONPointer): JSONRef<T> {
            val result = untyped(json, pointer)
            if (result.node !is T)
                result.node.typeError(typeOf<T>().refClassName(), pointer)
            return result as JSONRef<T>
        }

        /**
         * Create an untyped reference using the given base JSON and pointer.
         */
        fun untyped(base: JSONValue?, pointer: JSONPointer): JSONRef<JSONValue?> {
            val len = pointer.depth
            val nodes: Array<JSONValue?> = arrayOfNulls(len)
            var node: JSONValue? = base
            for (i in 0 until len) {
                val token = pointer.getToken(i)
                when (node) {
                    is JSONObject -> {
                        if (!node.containsKey(token))
                            throw JSONPointerException("Node does not exist", pointer.truncate(i + 1))
                        node = node[token] ?: throw JSONPointerException("Node is null", pointer.truncate(i + 1))
                        nodes[i] = node
                    }
                    is JSONArray -> {
                        if (!checkNumber(token) || token.toInt() >= node.size)
                            throw JSONPointerException("Node index incorrect", pointer.truncate(i + 1))
                        node = node[token.toInt()] ?:
                                throw JSONPointerException("Node is null", pointer.truncate(i + 1))
                        nodes[i] = node
                    }
                    else -> throw JSONPointerException("Not an object or array", pointer.truncate(i))
                }
            }
            return JSONRef(base, pointer, nodes, node)
        }

    }

}
