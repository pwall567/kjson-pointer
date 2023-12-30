/*
 * @(#) JSONPointer.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2021, 2022, 2023 Peter Wall
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

import net.pwall.text.CharMapResult
import net.pwall.text.StringMapper.checkLength
import net.pwall.text.StringMapper.mapCharacters
import net.pwall.text.StringMapper.mapSubstrings
import net.pwall.text.URIStringMapper.decodeURI
import net.pwall.text.URIStringMapper.encodeURI
import net.pwall.text.UTF8StringMapper.decodeUTF8
import net.pwall.text.UTF8StringMapper.encodeUTF8
import net.pwall.util.ImmutableList

/**
 * JSON Pointer.
 *
 * @author  Peter Wall
 */
class JSONPointer internal constructor(internal val tokens: Array<String>) {

    /**
     * Construct a `JSONPointer` from the supplied `String`, which must consist of zero or more tokens representing
     * either an object property name or an array index, with each token preceded by a slash (`/`).
     */
    constructor(pointer: String) : this(parseString(pointer))

    /** The depth of the pointer (the number of tokens). */
    val depth: Int
        get() = tokens.size

    /**
     * Get the tokens that make up this pointer as an [Array].
     */
    fun tokensAsArray(): Array<String> = if (tokens.isEmpty()) emptyArray() else tokens.copyOf()

    /**
     * Get the tokens that make up this pointer as a [List].
     */
    fun tokensAsList(): List<String> = ImmutableList.listOf(tokens)

    /**
     * Find the [JSONValue] that this `JSONPointer` points to within the specified base value, or throw an exception
     * if the pointer does not reference a valid location in the base value.
     */
    fun find(base: JSONValue?): JSONValue? = find(tokens, base)

    /**
     * Find the [JSONValue] that this `JSONPointer` points to within the specified base value, or `null` if the pointer
     * does not reference a valid location in the base value.
     */
    fun findOrNull(base: JSONValue?): JSONValue? = findOrNull(tokens, base)

    /**
     * Find the [JSONObject] that this `JSONPointer` points to within the specified base value, or throw an exception
     * if the pointer does not reference a valid location in the base value or if the value referenced is not a
     * [JSONObject].
     */
    fun findObject(base: JSONValue?): JSONObject = findOrNull(tokens, base).let {
        it as? JSONObject ?: it.typeError("JSONObject", this)
    }

    /**
     * Find the [JSONArray] that this `JSONPointer` points to within the specified base value, or throw an exception
     * if the pointer does not reference a valid location in the base value or if the value referenced is not a
     * [JSONArray].
     */
    fun findArray(base: JSONValue?): JSONArray = findOrNull(tokens, base).let {
        it as? JSONArray ?: it.typeError("JSONArray", this)
    }

    /**
     * Test whether this `JSONPointer` references a valid location in specified base value.
     */
    infix fun existsIn(json: JSONValue?): Boolean = existsIn(tokens, json)

    /**
     * Return a new `JSONPointer` referencing the parent [JSONObject] or [JSONArray] of the value referenced by this
     * pointer.
     */
    fun parent(): JSONPointer = when (val len = tokens.size) {
        0 -> rootParentError()
        1 -> root
        else -> JSONPointer(tokens.copyOfRange(0, len - 1))
    }

    /**
     * Return a new `JSONPointer` referencing the nominated child property of the object referenced by this pointer.
     */
    fun child(string: String): JSONPointer = JSONPointer(tokens + string)

    /**
     * Return a new `JSONPointer` referencing the nominated child item of the array referenced by this pointer.
     */
    fun child(index: Int): JSONPointer {
        if (index < 0)
            throw JSONPointerException("JSON Pointer index must not be negative", "$this/$index")
        return child(index.toString())
    }

    /** The last token of the `JSONPointer` (the current property name or array index). */
    val current: String?
        get() = tokens.lastOrNull()

    /** `true` if the pointer is pointing to root. */
    val isRoot: Boolean
        get() = tokens.isEmpty()

    /**
     * Convert the `JSONPointer` to a form suitable for use in a URI fragment.
     */
    fun toURIFragment(): String = buildString {
        for (token in tokens) {
            append('/')
            append(token.encodeJSONPointer().encodeUTF8().encodeURI())
        }
    }

    /**
     * Locate the specified target [JSONValue] in the base [JSONValue], and return a `JSONPointer` pointing to it, based
     * on the current pointer.  Returns `null` if not found.
     *
     * This will perform a depth-first search of the JSON structure.
     */
    fun locateChild(base: JSONValue?, target: JSONValue?): JSONPointer? {
        when {
            base === target -> return this
            base is JSONObject -> {
                for (key in base.keys) {
                    child(key).locateChild(base[key], target)?.let { return it }
                }
            }
            base is JSONArray -> {
                for (i in base.indices)
                    child(i).locateChild(base[i], target)?.let { return it }
            }
        }
        return null
    }

    /**
     * Create a [JSONReference] using this `JSONPointer` and the specified [JSONValue] base.
     */
    infix fun ref(base: JSONValue?) = if (existsIn(tokens, base)) JSONReference(base, tokens, true, find(tokens, base))
            else JSONReference(base, tokens, false, null)

    override fun equals(other: Any?): Boolean =
        this === other || other is JSONPointer && tokens.contentEquals(other.tokens)

    override fun hashCode(): Int = tokens.contentHashCode()

    override fun toString(): String = toString(tokens, tokens.size)

    companion object {

        /** The root `JSONPointer`. */
        val root = JSONPointer(emptyArray())

        private const val emptyString = ""

        /**
         * Create a `JSONPointer` from an array of tokens.
         */
        fun from(array: Array<String>): JSONPointer = if (array.isEmpty()) root else JSONPointer(array.copyOf())

        /**
         * Create a `JSONPointer` from a list of tokens.
         */
        fun from(list: List<String>): JSONPointer = if (list.isEmpty()) root else JSONPointer(list.toTypedArray())

        /**
         * Find the [JSONValue] that this pointer string points to within the specified base value, or throw an
         * exception if the pointer string does not reference a valid location in the base value.
         */
        fun find(pointer: String, base: JSONValue?) = find(parseString(pointer), base)

        /**
         * Find the [JSONValue] that this set of tokens points to within the specified base value, or throw an
         * exception if the tokens do not reference a valid location in the base value.
         */
        fun find(tokens: Array<String>, base: JSONValue?): JSONValue? {
            var result = base
            for (i in tokens.indices) {
                val token = tokens[i]
                result = when (result) {
                    is JSONObject -> {
                        if (!result.containsKey(token))
                            pointerError("Can't resolve JSON Pointer", tokens, i + 1)
                        result[token]
                    }
                    is JSONArray -> {
                        if (token == "-")
                            pointerError("Can't dereference end-of-array JSON Pointer", tokens, i + 1)
                        val index = checkIndex(token, tokens, i + 1)
                        if (index < 0 || index >= result.size)
                            pointerError("Array index out of range in JSON Pointer", tokens, i + 1)
                        result[index]
                    }
                    else -> pointerError("Can't resolve JSON Pointer", tokens, i + 1)
                }
            }
            return result
        }

        /**
         * Find the [JSONValue] that this pointer string points to within the specified base value, or `null` if the
         * pointer string does not reference a valid location in the base value.
         */
        fun findOrNull(pointer: String, base: JSONValue?) = findOrNull(parseString(pointer), base)

        /**
         * Find the [JSONValue] that this set of tokens points to within the specified base value, or `null` if the
         * tokens do not reference a valid location in the base value.
         */
        fun findOrNull(tokens: Array<String>, base: JSONValue?): JSONValue? {
            var result = base
            for (i in tokens.indices) {
                val token = tokens[i]
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
         * Test whether this pointer string references a valid location in specified base value.
         */
        fun existsIn(string: String, base: JSONValue?): Boolean = existsIn(parseString(string), base)

        /**
         * Test whether this set of tokens references a valid location in specified base value.
         */
        fun existsIn(tokens: Array<String>, base: JSONValue?): Boolean {
            var current: JSONValue? = base ?: return false
            for (token in tokens) {
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

        private fun checkIndex(token: String, tokens: Array<String>, tokenIndex: Int): Int {
            if (!checkNumber(token))
                pointerError("Illegal array index in JSON Pointer", tokens, tokenIndex)
            return token.toInt()
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
         * Create the string form of a JSON pointer from the first _n_ tokens in the specified array.
         */
        fun toString(tokens: Array<String>, n: Int): String {
            if (n == 0)
                return emptyString
            return buildString {
                for (i in 0 until n) {
                    append('/')
                    append(tokens[i].encodeJSONPointer())
                }
            }
        }

        /**
         * Parse a JSON Pointer string into an array of string tokens.
         */
        fun parseString(string: String): Array<String> {
            if (string.isEmpty())
                return emptyArray()
            if (string[0] != '/')
                pointerError("Illegal JSON Pointer", string)
            return string.substring(1).split('/').map {
                try {
                    it.decodeJSONPointer()
                }
                catch (e: Exception) {
                    pointerError("Illegal token in JSON Pointer", string)
                }
            }.toTypedArray()
        }

        /**
         * Create a `JSONPointer` from a URI fragment.
         */
        fun fromURIFragment(fragment: String): JSONPointer {
            val pointer: String = try {
                fragment.decodeURI().decodeUTF8()
            } catch (e: Exception) {
                pointerError("Illegal URI fragment", fragment)
            }
            return JSONPointer(pointer)
        }

        internal fun pointerError(mainText: String, tokens: Array<String>, tokenIndex: Int): Nothing {
            pointerError(mainText, toString(tokens, tokenIndex))
        }

        internal fun pointerError(mainText: String, pointer: Any? = null): Nothing {
            throw JSONPointerException(mainText, pointer)
        }

        internal fun rootParentError(): Nothing {
            throw JSONPointerException("Can't get parent of root JSON Pointer")
        }

        /**
         * Encode a string using the character substitutions specified for JSON pointer token.
         */
        fun String.encodeJSONPointer() = mapCharacters {
            when (it) {
                '~' -> "~0"
                '/' -> "~1"
                else -> null
            }
        }

        /**
         * Decode a string encoded using the character substitutions specified for JSON pointer token.
         */
        fun String.decodeJSONPointer() = mapSubstrings { index ->
            when (this[index]) {
                '~' -> {
                    checkLength(this, index, 2)
                    when (this[index + 1]) {
                        '0' -> mapJSONPointerTilde
                        '1' -> mapJSONPointerSlash
                        else -> throw IllegalArgumentException("Invalid escape sequence")
                    }
                }
                else -> null
            }
        }

        private val mapJSONPointerTilde = CharMapResult(2, '~')
        private val mapJSONPointerSlash = CharMapResult(2, '/')

    }

}
