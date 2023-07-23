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

/**
 * JSON Pointer.
 *
 * @author  Peter Wall
 */
class JSONPointer internal constructor(val tokens: Array<String>) {

    constructor(pointer: String) : this(parseString(pointer))

    fun find(base: JSONValue?): JSONValue? = find(tokens, base)

    fun findOrNull(base: JSONValue?): JSONValue? = findOrNull(tokens, base)

    fun findObject(base: JSONValue?): JSONObject = findOrNull(tokens, base).let {
        it as? JSONObject ?: it.typeError("JSONObject", this)
    }

    fun findArray(base: JSONValue?): JSONArray = findOrNull(tokens, base).let {
        it as? JSONArray ?: it.typeError("JSONArray", this)
    }

    infix fun existsIn(json: JSONValue?): Boolean = existsIn(tokens, json)

    fun parent(): JSONPointer = when (val len = tokens.size) {
        0 -> rootParentError()
        1 -> root
        else -> JSONPointer(Array(len - 1) { i -> tokens[i] })
    }

    fun child(string: String): JSONPointer {
        val len = tokens.size
        return JSONPointer(Array(len + 1) { i -> if (i < len) tokens[i] else string })
    }

    fun child(index: Int): JSONPointer {
        if (index < 0)
            throw JSONPointerException("JSON Pointer index must not be negative")
        return child(index.toString())
    }

    val current: String?
        get() = if (tokens.isEmpty()) null else tokens[tokens.size - 1]

    fun toURIFragment(): String = buildString {
        for (token in tokens) {
            append('/')
            append(token.encodeJSONPointer().encodeUTF8().encodeURI())
        }
    }

    fun locateChild(value: JSONValue?, target: JSONValue?): JSONPointer? {
        when {
            value === target -> return this
            value is JSONObject -> {
                for (key in value.keys) {
                    child(key).locateChild(value[key], target)?.let { return it }
                }
            }
            value is JSONArray -> {
                for (i in value.indices)
                    child(i).locateChild(value[i], target)?.let { return it }
            }
        }
        return null
    }

    infix fun ref(base: JSONValue?) = if (existsIn(tokens, base)) JSONReference(base, tokens, true, find(tokens, base))
            else JSONReference(base, tokens, false, null)

    override fun equals(other: Any?): Boolean =
        this === other || other is JSONPointer && tokens.contentEquals(other.tokens)

    override fun hashCode(): Int = tokens.contentHashCode()

    override fun toString(): String = toString(tokens, tokens.size)

    companion object {

        val root = JSONPointer(emptyArray())
        private const val emptyString = ""

        fun from(list: List<String>): JSONPointer = if (list.isEmpty()) root else JSONPointer(list.toTypedArray())

        fun find(pointer: String, base: JSONValue?) = find(parseString(pointer), base)

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

        fun findOrNull(pointer: String, base: JSONValue?) = findOrNull(parseString(pointer), base)

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

        fun existsIn(string: String, base: JSONValue?): Boolean = existsIn(parseString(string), base)

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
                    pointerError("Illegal token in JSON Pointer", it)
                }
            }.toTypedArray()
        }

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

        internal fun pointerError(mainText: String, pointer: String): Nothing {
            throw JSONPointerException(if (pointer.isEmpty()) mainText else "$mainText - \"$pointer\"")
        }

        internal fun rootParentError(): Nothing {
            throw JSONPointerException("Can't get parent of root JSON Pointer")
        }

        fun String.encodeJSONPointer() = mapCharacters {
            when (it) {
                '~' -> "~0"
                '/' -> "~1"
                else -> null
            }
        }

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
