# kjson-pointer

[![Build Status](https://travis-ci.com/pwall567/kjson-pointer.svg?branch=main)](https://app.travis-ci.com/github/pwall567/kjson-pointer)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.8.22&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.8.22)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/kjson-pointer?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%22kjson-pointer%22)

Kotlin implementation of [JSON Pointer](https://tools.ietf.org/html/rfc6901).

Note &ndash; **Breaking Change** for users of the `toURIFragment()` and `fromURIFragment()` functions &ndash; from
version 4.0 of the library, these functions no longer output or expect the leading `#` symbol.

## Quick Start

### `JSONPointer`

To create a JSON Pointer:
```kotlin
        val pointer = JSONPointer("/prop1/0")
```
This creates a pointer to the 0th element of the "prop1" property (of whatever JSON value is addressed).

To test whether such an element exists in the JSON object "obj":
```kotlin
        if (pointer existsIn obj) {
            // whatever
        }
```

To retrieve the element:
```kotlin
        val value = pointer.find(obj)
```

A pointer to the root element of any JSON value:
```kotlin
        val pointer = JSONPointer.root
```

To navigate to a child property:
```kotlin
        val newPointer = pointer.child("prop1")
```

To navigate to a child array element:
```kotlin
        val newPointer2 = newPointer.child(0)
```
(the result of the last two operations is a pointer equivalent to the pointer in the first example).

To create a pointer from a list of pointer elements:
```kotlin
        val pointerFromList = JSONPointer.from(listOf("prop1", "0"))
```
(again, the result will be a pointer equivalent to the pointer in the first example).

To create a pointer to a specified child value within a structure:
```kotlin
        val childPointer = JSONPointer.root.locateChild(structure, target)
```
(This will perform a depth-first search of the JSON structure, so it should be used only when there is no alternative.)

### `JSONReference`

A `JSONReference` is a combination of a `JSONPointer` and a `JSONValue`.
This can be valuable when navigating around a complex tree &ndash; it removes the necessity to pass around both a
pointer and the base value to which it refers, and it pre-calculates the destination value (and its validity).

(`JSONReference` has since been superseded by [`JSONRef`](#jsonref), and may eventually be deprecated.)

To create a `JSONReference` to the root of an object:
```kotlin
        val ref = JSONReference(base)
```

Or to create a reference to a location in an object specified by a pointer:
```kotlin
        val ref = JSONPointer("/field") ref base
```

The `parent()` and `child()` operations work on `JSONReference`s similarly to their `JSONPointer` equivalents.

To get the value from a `JSONReference` (the value within the base object pointed to by the pointer part of the
reference):
```kotlin
        val value: JSONValue? = ref.value // may be null
```

To test whether the reference is valid, that is, the pointer refers to a valid location in the base object:
```kotlin
        if (ref.valid) {
            // the reference can be taken to be valid
        }
```

To test whether the reference has a nominated child:
```kotlin
        if (ref.hasChild(name)) { // or index
            // code to make use of ref.child(name)
        }
```

To create a reference to a specified target child value:
```kotlin
        val childRef = baseRef.locateChild(target)
```
(This will perform a depth-first search of the JSON structure, so it should be used only when there is no alternative.)

### `JSONRef`

`JSONRef` is an evolution of the concept first implemented in [`JSONReference`](#jsonreference).
Like the earlier class, it combines a `JSONPointer` with the object into which the pointer navigates, but as a
parameterised class, it allows the target element to be accessed in a type-safe manner.

The parameter class may be any of the `JSONValue` sealed interface types:
- `JSONString`
- `JSONInt`
- `JSONLong`
- `JSONDecimal`
- `JSONNumber`: `JSONInt`, `JSONLong` or `JSONDecimal`
- `JSONBoolean`
- `JSONPrimitive`: `JSONString`, `JSONInt`, `JSONLong`, `JSONDecimal` or `JSONBoolean`
- `JSONArray`
- `JSONObject`
- `JSONStructure`: `JSONArray` or `JSONObject`
- `JSONValue`: any of the above types

The usage of `JSONRef` is best explained be example:
```kotlin
    val json = JSON.parseObject("file.json")
    val ref = JSONRef(json)
```
The value `ref` will be of type `JSONRef<JSONObject>`; it will be a reference to the root of the object tree.

`JSONRef<J>` exposes three properties:

| Name      | Type          | Description                              |
|-----------|---------------|------------------------------------------|
| `base`    | `JSONValue`   | The base JSON value                      |
| `pointer` | `JSONPointer` | The `JSONPointer` to the referenced node |
| `node`    | `J`           | The node (`J` a subclass of `JSONValue`) |

To navigate to the `id` property of the object in the above example, expecting it to be a string:
```kotlin
    val idRef = ref.child<JSONString>("id")
    val id = idRef.node
```
`idRef` will be of type `JSONRef<JSONString>`, and `id` will be of type `JSONString`.

Now imagine that the object contains a property named `address`, which is an array of address line strings:
```kotlin
    val address = ref.child<JSONArray>("address")
    val line0 = address.child<JSONString>(0).node
```

And to iterate over the address lines:
```kotlin
    address.forEach<JSONString> {
        // within this code, "this" is a JSONRef<JSONString>, and "it" is an Int (the index)
        println("Address line ${it + 1}: ${node.value}")
    }
```

Or over the properties of an object:
```kotlin
    ref.forEachKey<JSONValue> {
        // within this code, "this" is a JSONRef<JSONValue>, and "it" is a String (the object key / property name)
    }
```
This also illustrates the use of `JSONValue` as the parameterised type, to allow for the case where properties are of
different types.
When using a `JSONRef<JSONValue>`, the `isRef()` function tests whether the reference is to a node of a specific type:
```kotlin
    if (genericRef.isRef<JSONBoolean>) {
        // the node is Boolean
    }
```
and the `asRef()` function converts to a specified type, throwing an exception if the type is incorrect:
```kotlin
    val stringRef = genericRef.asRef<JSONString>()
```

More documentation to follow&hellip;

## Dependency Specification

The latest version of the library is 4.7, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson-pointer</artifactId>
      <version>4.7</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:kjson-pointer:4.7'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson-pointer:4.7")
```

Peter Wall

2023-07-24
