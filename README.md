# kjson-pointer

[![Build Status](https://travis-ci.com/pwall567/kjson-pointer.svg?branch=main)](https://app.travis-ci.com/github/pwall567/kjson-pointer)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.8.22&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.8.22)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/kjson-pointer?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%22kjson-pointer%22)

Kotlin implementation of [JSON Pointer](https://tools.ietf.org/html/rfc6901).

Note &ndash; **Breaking Change** &ndash; from version 5.0 of the library, the `tokens` array is no longer accessible as
a `public` value.
It was an oversight to allow it to be accessible previously since array members may be modified, and a `JSONPointer` is
intended to be immutable.
The array of tokens may be obtained by the functions `tokensAsArray()` which returns a copy of the array, or
`tokensAsList()` which returns an immutable `List`.

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

The parameter class may be any of the `JSONValue` sealed interface types (or their nullable forms):
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

This may be simplified by the use of the `ref()` extension function:
```kotlin
    val jsonRef = JSON.parseObject("file.json").ref()
```
The result is the same; the only difference is that the syntax may be easier to read.

`JSONRef<J>` exposes three properties:

| Name      | Type          | Description                              |
|-----------|---------------|------------------------------------------|
| `base`    | `JSONValue`   | The base JSON value                      |
| `pointer` | `JSONPointer` | The `JSONPointer` to the referenced node |
| `node`    | `J`           | The node (`J` a subtype of `JSONValue?`) |

#### Navigation

`JSONRef` is ideally suited to the task of navigation a JSON structure.
The `JSONRef` object itself is immutable, so to navigate to a nested part of a structure, the `child` operation creates
a new `JSONRef` pointing to the appropriate location in the structure.

To navigate to the `id` property of the object in the above example, expecting it to be a string:
```kotlin
    val idRef = ref.child<JSONString>("id")
    val id = idRef.node
```
`idRef` will be of type `JSONRef<JSONString>`, and `id` will be of type `JSONString`.
The function will throw an exception if the property `id` is missing, is null or is not a `JSONString`

Now imagine that the object contains a property named `address`, which is an array of address line strings:
```kotlin
    val addressRef = ref.child<JSONArray>("address")
    val line0Ref = addressRef.child<JSONString>(0)
    val line0 = line0Ref.node
```
`addressRef` will be of type `JSONRef<JSONArray>`, `line0Ref` will be of type `JSONRef<JSONString>` and `line0` will be
of type `JSONString`.

To navigate to the parent node:
```kotlin
   val parentRef = addressRef.parent<JSONObject>()
```
In this example, `parentRef` will now be identical to the original `ref`.

#### Iteration

To iterate over the address lines from the example above:
```kotlin
    addressRef.forEach<JSONString> {
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
This also illustrates the use of `JSONValue` as the parameterised type &ndash; this is saying that the value may be any
non-null `JSONValue`, to allow for the case where properties are of different types.
When using a `JSONRef<JSONValue>`, the `isRef()` function tests whether the reference is to a node of a specific type:
```kotlin
    if (genericRef.isRef<JSONBoolean>()) {
        // the node is Boolean
    }
```
and the `asRef()` function converts to a specified type, throwing an exception if the type is incorrect:
```kotlin
    val stringRef = genericRef.asRef<JSONString>()
```

#### Optional Properties

There are a number of functions provided as extension functions on `JSONRef<JSONObject>` to simplify access to optional
properties of the object:

- `optionalString(name)`: returns the `String` value of the named property, or `null` if it is not present
- `optionalInt(name)`: returns the `Int` value of the named property, or `null` if it is not present
- `optionalLong(name)`: returns the `Long` value of the named property, or `null` if it is not present
- `optionalDecimal(name)`: returns the `BigDecimal` value of the named property, or `null` if it is not present
- `optionalBoolean(name)`: returns the `Boolean` value of the named property, or `null` if it is not present

In all cases, if the property is not of the required type, an exception is thrown detailing the expected type, the
actual value and the location in the structure, in `JSONPointer` form.

When the optional property is a nested sub-structure, the `optionalChild()` function may be used:
```kotlin
    ref.optionalChild<JsonObject>("address")?.apply {
        // within this code, "this" is a JSONRef<JSONObject>
    }
```
As with the other optional functions, if the property is present but of the wrong type, a detailed exception is thrown.

More documentation to follow&hellip;

## Dependency Specification

The latest version of the library is 7.2, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson-pointer</artifactId>
      <version>7.2</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:kjson-pointer:7.2'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson-pointer:7.2")
```

Peter Wall

2024-02-11
