# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [8.9] - 2025-06-07
### Changed
- `JSONRef`: added `rebase()`
- `JSONRef`, `JSONReference`, `Find`: modified `locateChild()` functions (identity comparisons not applicable on enums
  and value classes)
- `pom.xml`: updated dependency versions

## [8.8] - 2025-02-06
### Changed
- `pom.xml`: updated dependency version

## [8.7] - 2025-02-01
### Changed
- `pom.xml`: updated Kotlin version to 2.0.21, updated dependencies

## [8.6] - 2024-12-13
### Changed
- `JSONRef`: make `parent()` and `createChildRef()` public

## [8.5] - 2024-12-13
### Changed
- `JSONRef`: make `refClassName()` public (made internal earlier by mistake)

## [8.4] - 2024-12-12
### Changed
- `JSONRef`: make use of `@PublishedApi` annotation
- tests : switched to `should-test` library

## [8.3] - 2024-09-04
### Changed
- `Extension.kt`: bug fix in `optionalChild()`, added `withOptionalChild()`

## [8.2] - 2024-08-17
### Changed
- `pom.xml`: updated dependency version

## [8.1] - 2024-07-25
### Changed
- `JSONRef`: fixed bug in creating ref to null node
- `JSONRef`, `Find.kt`: standardised error messages

## [8.0] - 2024-07-09
### Added
- `build.yml`, `deploy.yml`: converted project to GitHub Actions
- `Find.kt`: `find` functions _etc._ from `JSONPointer` converted to extension functions
### Changed
- `JSONRef`, `JSONReference`: major changes to use `kjson-pointer-core`
- `pom.xml`: include `kjson-pointer-core`
- `pom.xml`: updated Kotlin version to 1.9.24
### Removed
- `JSONPointer`: moved to `kjson-pointer-core`
- `.travis.yml`

## [7.4] - 2024-02-14
### Changed
- `pom.xml`: updated dependency versions

## [7.3] - 2024-02-14
### Changed
- `pom.xml`: updated dependency version

## [7.2] - 2024-02-11
### Changed
- `pom.xml`: updated dependency version
- tests: moved some tests to the appropriate class

## [7.1] - 2024-01-01
### Changed
- `pom.xml`: updated dependency version

## [7.0] - 2023-12-31
### Changed
- `Extension.kt`: deprecated `JSONRef<JSONObject>.map()` and `mapIfPresent()`
- `Extension.kt`: added `JSONRef<JSONArray>.map()`, `JSONRef<JSONArray>.any()` and `JSONRef<JSONArray>.all()`
- `Extension.kt`: added `JSONRef<JSONPrimitive<*>>.value`
- `Extension.kt`: genericised `JSONValue.ref()`
- `JSONRef`: simplified `toString()` to allow it to be used as `key` in error messages
- `pom.xml`: updated dependency version
- `pom.xml`: incremented major version for probable breaking changes

## [6.0] - 2023-12-11
### Changed
- `JSONPointer`: added `isRoot`
- `Extension.kt`: added `optionalString`, `optionalBoolean`, `optionalInt`, `optionalLong`, `optionalDecimal`
- `Extension.kt`: added `optionalChild`, various other improvements
- `JSONRef`: extended `of()` functions to allow nullable types in result, added `untyped()` (these changes to various
  functions probably constitute a breaking change)
- `JSONReference`: added possible future deprecation notice

## [5.2] - 2023-11-20
### Changed
- `Extension.kt`: added `mapIfPresent`

## [5.1] - 2023-09-24
### Changed
- `pom.xml`: updated dependency versions

## [5.0] - 2023-07-29
### Changed
- `JSONPointer`, `JSONRef`, `JSONReference`: switch to use of Kotlin intrinsic array functions
- `JSONPointer`: remove public access to tokens array
- `JSONPointer`: added KDoc

## [4.7] - 2023-07-24
### Changed
- `pom.xml`: updated dependency version

## [4.6] - 2023-07-23
### Changed
- `JSONPointer`: added `from()` function to create pointer from list of tokens
- `pom.xml`: updated Kotlin version to 1.8.22

## [4.5] - 2023-07-07
### Changed
- `pom.xml`: bumped dependency versions

## [4.4] - 2023-04-23
### Changed
- `IndexOp.kt`: allow `get()` and `contains()` functions to operate on null values

## [4.3] - 2023-01-10
### Changed
- `IndexOp.kt`: allow `get()` and `contains()` functions to operate on null values

## [4.2] - 2023-01-08
### Changed
- `pom.xml`: bumped dependency version

## [4.1] - 2023-01-03
### Changed
- `pom.xml`: bumped dependency version

## [4.0] - 2022-12-02
### Changed
- `JSONPointer`: changed `toURIFragment()` and `fromURIFragment()` to no longer output or expect the leading `#`
  (breaking change)
- `JSONRef`: added `isRef()` and `asRef()`
- `JSONPointer`: minor change to error message output
- `IndexOp`: Added `getObject()`, `getString()` _etc._
- `JSONRef`, `Extension`: moved `child()` and `hasChild()` from `JSONRef` to `Extension`, added `untypedRef()`

## [3.0] - 2022-11-27
### Changed
- `pom.xml`: updated major version for potential breaking change (in `kjson-core`)

## [2.8] - 2022-11-27
### Changed
- `pom.xml`: bumped dependency version

## [2.7] - 2022-11-23
### Changed
- `pom.xml`: bumped dependency version

## [2.6] - 2022-11-20
### Changed
- `pom.xml`: bumped dependency version

## [2.5] - 2022-11-07
### Changed
- `pom.xml`: bumped dependency version

## [2.4] - 2022-10-27
### Changed
- `JSONPointer`: minor changes for new version of `string-mapper`
- `pom.xml`: bumped dependency version

## [2.3] - 2022-10-16
### Changed
- `JSONPointer`: updated for new version of `string-mapper`
- `pom.xml`: bumped dependency version

## [2.2] - 2022-10-16
### Changed
- `JSONPointer`: changed to use URI encoding from `string-mapper`
- `pom.xml`: dropped `pipelines` dependency and added `string-mapper`

## [2.1] - 2022-10-11
### Changed
- `JSONPointer`: changed to use URI encoding from `pipelines`
- `pom.xml`: bumped dependency version

## [2.0] - 2022-10-08
### Added
- `JSONRef`: improved version of `JSONReference`
- `IndexOp`: extension functions
- `Extension`: extension functions
### Changed
- `JSONPointer`: added `findObject`, `findArray`, `findOrNull`
- `pom.xml`: bumped dependency version

## [1.9.2] - 2022-09-19
### Changed
- `pom.xml`: bumped dependency version

## [1.9.1] - 2022-09-04
### Changed
- `pom.xml`: bumped dependency version

## [1.9] - 2022-06-07
### Changed
- `pom.xml`: bumped dependency version

## [1.8.1] - 2022-05-29
### Changed
- `pom.xml`: bumped dependency version

## [1.8] - 2022-05-01
### Changed
- `pom.xml`: bumped dependency version
- `JSONPointer`: switched to use `int-output` library

## [1.7] - 2022-04-18
### Changed
- `JSONPointer`: minor optimisations
- `pom.xml`: bumped dependency version

## [1.6] - 2022-01-28
### Changed
- `pom.xml`: bumped dependency version

## [1.5] - 2022-01-27
### Changed
- `pom.xml`: bumped dependency version

## [1.4] - 2022-01-24
### Changed
- `pom.xml`: bumped dependency version

## [1.3] - 2022-01-22
### Changed
- `JSONPointer`: bug fix - index range check
- `pom.xml`: updated to Kotlin 1.6.10

## [1.2.1] - 2021-10-27
### Changed
- `pom.xml`: bumped dependency version

## [1.2] - 2021-10-13
### Changed
- `pom.xml`: bumped dependency version

## [1.1] - 2021-08-25
### Changed
- `pom.xml`: bumped dependency version

## [1.0] - 2021-08-22
### Changed
- `JSONPointer`: made tokens visible to other classes

## [0.2] - 2021-08-04
### Added
- `JSONReference`: new
### Changed
- Kotlin files: added comments and kdoc
- `JSONPointer`: switched to use pipelines URI encode/decode

## [0.1] - 2021-08-04
### Added
- all files: initial versions
