---
title: Migrate to Fundamentals
description: Update concepts and correlation ids for Chronicle 7.x in Kotlin and Java without changing stored JSON or schemas.
---

Chronicle 7.x adopts `io.cratis:fundamentals:0.1.0`. This is a **major,
breaking release**: recompile consumers after updating. There is no dependency
to add; Fundamentals comes transitively with `io.cratis:chronicle`.

The JSON wire format and event schemas do not change. Concepts still serialize
as their wrapped scalar value, so existing events continue to read back.

## Kotlin concepts

Before:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.concepts.ConceptAs

data class BookId(override val value: String) : ConceptAs<String>
```

After:

<!-- validate: skip -->

```kotlin
import io.cratis.fundamentals.concepts.ConceptAs

data class BookId(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}
```

Apply the same pattern to `@JvmInline value class` concepts. The backing
property can also have a different name and be private; the interface only
requires `fun value(): T`.

Reading through a `ConceptAs<T>` reference changes from `concept.value` to
`concept.value()`. To keep property-style reads in Kotlin, import
`io.cratis.fundamentals.concepts.value`. A concrete `bookId.value` still uses
the data-class property and needs no extension import.

`EventSourceId` keeps its name and package,
`io.cratis.chronicle.concepts.EventSourceId`. Its implementers make the same
property-to-function change.

## Java concepts

Before:

<!-- validate: skip -->

```java
import io.cratis.chronicle.concepts.ConceptAs;

record BookId(String value) implements ConceptAs<String> {
    @Override public String getValue() { return value; }
}
```

After:

<!-- validate: skip -->

```java
import io.cratis.fundamentals.concepts.ConceptAs;

record BookId(String value) implements ConceptAs<String> {}
```

Delete the old getter override: the record accessor implements the new
interface directly. Change calls from `getValue()` to `value()`. Records
implementing `EventSourceId` follow the same pattern.

## Correlation ids

Before:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.correlation.CorrelationId

val empty = CorrelationId.notSet
val uuid = CorrelationId.create().value
```

After:

<!-- validate: skip -->

```kotlin
import io.cratis.fundamentals.execution.CorrelationId

val empty = CorrelationId.NOT_SET
val uuid = CorrelationId.create().value()
```

`CorrelationId(uuid)` and `CorrelationId.create()` remain available. The new
type is an ordinary class rather than a Kotlin value class, with value-based
equality and hashing. Java can now call `begin(CorrelationId)`,
`tryGetFor(CorrelationId)` and `getCorrelationId()` on units of work and their
manager without value-class name mangling. `BlockingUnitOfWork` still exposes
its correlation id as a string.

## Geospatial types

`Point`, `LineString`, `LinearRing` and `Polygon` also move to Fundamentals, in
`io.cratis.fundamentals.geospatial`. This is a non-breaking, minor change:
change the import. The constructors, property names and `copy`/destructuring
are the same. The wire format and event schemas are unchanged, so stored
events read back as before. The old `io.cratis.chronicle.geospatial` types are
deprecated at warning level in 7.3 and removed in the next major. For code
that mixes both, the old types have `toFundamentals()` and a constructor that
takes the Fundamentals type.

## Deprecated names

For 7.x, `io.cratis.chronicle.concepts.ConceptAs` and
`io.cratis.chronicle.correlation.CorrelationId` remain deprecated Kotlin
typealiases, at warning level, with IDE import quick-fixes. The
`io.cratis.chronicle.geospatial` classes are likewise deprecated at warning
level. References to the old types can still compile, but concept
implementations must implement `value()` and the old `notSet` name is not
available. Java cannot see Kotlin typealiases; Java imports must change to
Fundamentals.

## Release notes

### Added

- Java can begin, look up and inspect units of work by correlation id
  (part of [#151](https://github.com/Cratis/Chronicle.Kotlin/issues/151)).

### Changed

- Breaking: `ConceptAs<T>` moves to Fundamentals and requires `fun value(): T`
  instead of a property. Java records no longer need an extra getter.
- Breaking: `CorrelationId` moves to Fundamentals as an ordinary class;
  use `NOT_SET` and `value()`.
- Concept JSON and event schemas remain unchanged.

### Deprecated

- The two old Chronicle type names remain as Kotlin typealiases for this
  major version; migrate imports as shown above.
