# Strongly-typed identifiers

A `String` is a `String` is a `String`. Nothing stops a book's identifier being
passed where a member's was expected — both compile, and the bug surfaces in
production as a lookup that quietly finds nothing.

<!-- validate: skip -->

```kotlin
fun borrow(bookId: String, memberId: String) { }

// Compiles. Wrong.
borrow(memberId, bookId)
```

Give each value a type of its own and the compiler catches it while you are
still typing.

## Declaring one

A concept is any type implementing `ConceptAs<T>`:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.concepts.ConceptAs

data class BookId(override val value: String) : ConceptAs<String>
data class MemberId(override val value: String) : ConceptAs<String>
data class CopyNumber(override val value: Int) : ConceptAs<Int>
```

Then use them wherever the value used to be:

<!-- validate: skip -->

```kotlin
@EventType
data class BookBorrowed(
    val book: BookId = BookId(""),
    val member: MemberId = MemberId("")
)

// Won't compile — which is the entire point.
BookBorrowed(memberId, bookId)
```

## What goes on the wire

A concept serializes as the value it wraps, not as an object wrapping one:

```json
{ "book": "dune", "member": "ada" }
```

That is what makes concepts something you can adopt for a property that is
already in production. The JSON does not change, the schema the kernel
validates against does not change, and every event stored before the concept
existed still reads back. The kernel never learns that the value has a type on
your side, and does not need to.

The same holds for read models, reducer state, and constraint declarations —
everything goes through one serializer, so a concept behaves identically
wherever it appears.

## Event source ids

The event source id is a `String` on the wire and always will be. So that your
side of the call can still be typed, every client method taking one has an
overload taking a `ConceptAs<String>`:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.concepts.append

store.eventLog.append(BookId("dune"), BookBorrowed(BookId("dune"), MemberId("ada")))
```

These are extension functions in `io.cratis.chronicle.concepts`, so import the
one you need. They cover `append`, `appendMany`, `hasEventsFor`,
`getTailSequenceNumber`, `getForEventSourceIdAndEventTypes`,
`redactForEventSource`, and `getInstanceByKey`.

## From Java

A concept is an ordinary type, so Java declares one as a record:

<!-- validate: skip -->

```java
public record BookId(String value) implements ConceptAs<String> {
    @Override public String getValue() { return value; }
}
```

The extra `getValue()` is the Kotlin property's accessor — a Java record's
`value()` does not satisfy it on its own.

## Data class or value class

Kotlin's `@JvmInline value class` also works and avoids the allocation, but its
mangled JVM signatures are awkward from Java. Prefer a `data class` unless the
allocation genuinely matters and Java never touches the type.
