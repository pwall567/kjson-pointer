# kjson-pointer

[![Build Status](https://travis-ci.com/pwall567/kjson-pointer.svg?branch=main)](https://app.travis-ci.com/github/pwall567/kjson-pointer)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.6.10&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.6.10)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/kjson-pointer?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%kjson-pointer%22)

Kotlin implementation of [JSON Pointer](https://tools.ietf.org/html/rfc6901).

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

To create a pointer to a specified child value within a structure:
```kotlin
        val childPointer = JSONPointer.root.locateChild(structure, target)
```
(This will perform a depth-first search of the JSON structure, so it should be used only when there is no alternative.)

### `JSONReference`

A `JSONReference` is a combination of a `JSONPointer` and a `JSONValue`.
This can be valuable when navigating around a complex tree &ndash; it removes the necessity to pass around both a
pointer and the base value to which it refers, and it pre-calculates the destination value (and its validity).

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


## Dependency Specification

The latest version of the library is 1.9.2, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson-pointer</artifactId>
      <version>1.9.2</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:kjson-pointer:1.9.2'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson-pointer:1.9.2")
```

Peter Wall

2022-09-19
