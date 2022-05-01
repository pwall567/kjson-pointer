/*
 * @(#) JSONPointer.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2021, 2022 Peter Wall
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

import net.pwall.pipeline.AbstractIntPipeline
import net.pwall.pipeline.IntAcceptor
import net.pwall.pipeline.StringAcceptor
import net.pwall.pipeline.codec.CodePoint_UTF8
import net.pwall.pipeline.codec.UTF8_CodePoint
import net.pwall.pipeline.uri.URIDecoder
import net.pwall.pipeline.uri.URIEncoder
import net.pwall.util.IntOutput.output2Hex

/**
 * JSON Pointer.
 *
 * @author  Peter Wall
 */
class JSONPointer internal constructor(val tokens: Array<String>) {

    constructor(pointer: String) : this(parseString(pointer))

    fun find(base: JSONValue?) = find(tokens, base)

    infix fun existsIn(json: JSONValue?): Boolean = existsIn(tokens, json)

    fun parent(): JSONPointer = when (val len = tokens.size) {
        0 -> throw JSONPointerException("Can't get parent of root JSON Pointer")
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

    fun toURIFragment(): String {
        val sb = StringBuilder()
        sb.append('#')
        val pipeline = EscapePipeline(CodePoint_UTF8(SchemaURIEncoder(StringAcceptor(sb))))
        for (token in tokens) {
            sb.append('/')
            pipeline.accept(token)
        }
        return sb.toString()
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

    class SchemaURIEncoder<T>(next: IntAcceptor<T>) : AbstractIntPipeline<T>(next) {

        override fun acceptInt(value: Int) {
            if (URIEncoder.isUnreservedURI(value) || value == '$'.code)
                emit(value)
            else {
                emit('%'.code)
                output2Hex(value, ::emit)
            }
        }

    }

    class EscapePipeline<T>(next: IntAcceptor<T>) : AbstractIntPipeline<T>(next) {

        override fun acceptInt(value: Int) {
            when (value) {
                '~'.code -> {
                    emit('~'.code)
                    emit('0'.code)
                }
                '/'.code -> {
                    emit('~'.code)
                    emit('1'.code)
                }
                else -> emit(value)
            }
        }

    }

    companion object {

        val root = JSONPointer(emptyArray())
        private const val emptyString = ""

        fun find(pointer: String, base: JSONValue?) = find(parseString(pointer), base)

        fun find(tokens: Array<String>, base: JSONValue?): JSONValue? {
            var result = base
            for (i in tokens.indices) {
                val token = tokens[i]
                result = when (result) {
                    is JSONObject -> {
                        if (!result.containsKey(token))
                            error(tokens, i + 1)
                        result[token]
                    }
                    is JSONArray -> {
                        if (token == "-")
                            throw JSONPointerException(
                                    "Can't dereference end-of-array JSON Pointer ${toStr1(tokens, i)}")
                        val index = checkIndex(token) { "Illegal array index in JSON Pointer ${toStr1(tokens, i)}" }
                        if (index < 0 || index >= result.size)
                            throw JSONPointerException("Array index out of range in JSON Pointer ${toStr1(tokens, i)}")
                        result[index]
                    }
                    else -> error(tokens, i + 1)
                }
            }
            return result
        }

        fun existsIn(string: String, base: JSONValue?): Boolean = existsIn(parseString(string), base)

        fun existsIn(tokens: Array<String>, base: JSONValue?): Boolean {
            var current: JSONValue? = base ?: return false
            for (token in tokens) {
                when (current) {
                    is JSONObject -> {
                        if (!current.containsKey(token))
                            return false
                        current = current[token]
                    }
                    is JSONArray -> {
                        if (!checkNumber(token))
                            return false
                        val index = token.toInt()
                        if (index < 0 || index >= current.size)
                            return false
                        current = current[index]
                    }
                    else -> return false
                }
            }
            return true
        }

        private fun checkIndex(token: String, lazyMessage: () -> String): Int {
            if (!checkNumber(token))
                throw JSONPointerException(lazyMessage())
            return token.toInt()
        }

        private fun checkNumber(token: String): Boolean {
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

        private fun error(tokens: Array<String>, tokenIndex: Int): Nothing {
            throw JSONPointerException("Can't resolve JSON Pointer ${toString(tokens, tokenIndex)}")
        }

        private fun toStr1(tokens: Array<String>, n: Int) = toString(tokens, n + 1)

        fun toString(tokens: Array<String>, n: Int): String {
            if (n == 0)
                return emptyString
            return StringBuilder().apply {
                for (i in 0 until n) {
                    append('/')
                    append(escape(tokens[i]))
                }
            }.toString()
        }

        fun escape(token: String): String {
            val len = token.length
            var i = 0
            var ch: Char
            while (true) {
                if (i >= len)
                    return token
                ch = token[i]
                if (ch == '~' || ch == '/')
                    break
                i++
            }
            val sb = StringBuilder(len + 8)
            sb.append(token, 0, i)
            while (true) {
                when (ch) {
                    '~' -> sb.append("~0")
                    '/' -> sb.append("~1")
                    else -> sb.append(ch)
                }
                if (++i >= len)
                    return sb.toString()
                ch = token[i]
            }
        }

        fun parseString(string: String): Array<String> {
            if (string.isEmpty())
                return emptyArray()
            if (string[0] != '/')
                throw JSONPointerException("Illegal JSON Pointer $string")
            return string.substring(1).split('/').map { unescape(it) }.toTypedArray()
        }

        private fun unescape(token: String): String {
            val len = token.length
            var i = 0
            while (true) {
                if (i >= len)
                    return token
                if (token[i] == '~')
                    break
                i++
            }
            val sb = StringBuilder(len)
            sb.append(token, 0, i)
            while (true) {
                if (++i >= len)
                    throw JSONPointerException("Illegal token in JSON Pointer $token")
                when (token[i]) {
                    '0' -> sb.append('~')
                    '1' -> sb.append('/')
                    else -> throw JSONPointerException("Illegal token in JSON Pointer $token")
                }
                while (true) {
                    if (++i >= len)
                        return sb.toString()
                    when (val ch = token[i]) {
                        '~' -> break
                        else -> sb.append(ch)
                    }
                }
            }
        }

        fun fromURIFragment(fragment: String): JSONPointer {
            if (fragment.isEmpty() || fragment[0] != '#')
                throw JSONPointerException("Illegal URI fragment $fragment")
            val pipeline = URIDecoder(UTF8_CodePoint(StringAcceptor()))
            try {
                for (i in 1 until fragment.length)
                    pipeline.accept(fragment[i].code)
                pipeline.close()
            }
            catch (e: Exception) {
                throw JSONPointerException("Illegal URI fragment $fragment")
            }
            return JSONPointer(pipeline.result)
        }

    }

}
