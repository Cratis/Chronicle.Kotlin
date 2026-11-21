---
title: Behavior patterns
description: Ask the Chronicle kernel what a person normally does at a moment in the week, or query the mined behavior patterns by facet set.
---

Chronicle mines behavior patterns from what happens in an event store: who
tends to do what, on which day, in which part of the day. The question an
application has is rarely "which patterns match this facet set" — it is
*what does this person normally do at this point in the week?*

## Asking about a moment

`getPatternsAt` takes the scope — normally the user — and, optionally, the
moment to ask about. Leave the moment out and it asks about now:

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.patterns.PatternGroupingKey

val patterns = store.patterns.getPatternsAt(PatternGroupingKey("user-1"))
```

Pass a moment to ask about another time:

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.patterns.PatternGroupingKey
import java.time.OffsetDateTime

val tomorrowMorning = OffsetDateTime.now().plusDays(1).withHour(9)
val patterns = store.patterns.getPatternsAt(PatternGroupingKey("user-1"), tomorrowMorning)
```

The `Day` and `TimeBucket` facets come from the moment; you never pass them.
Narrow the question with further facets through `alsoConstraining`. When it
also names a day or a time bucket, the moment's values win:

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.patterns.FacetName
import io.cratis.chronicle.patterns.FacetSet
import io.cratis.chronicle.patterns.PatternGroupingKey

val patterns = store.patterns.getPatternsAt(
    PatternGroupingKey("user-1"),
    alsoConstraining = FacetSet.empty.with(FacetName.AggregateType, "Invoice"),
)
```

## Time buckets

The time bucket is read from the moment's own offset, not from UTC: somebody
who works at nine does so at nine in *their* morning. Pass a moment that
carries the offset of the person you are asking about.

| Hours | Bucket |
| --- | --- |
| 05:00–08:00 | `EarlyMorning` |
| 08:00–11:00 | `Morning` |
| 11:00–14:00 | `Midday` |
| 14:00–17:00 | `Afternoon` |
| 17:00–22:00 | `Evening` |
| 22:00–05:00 | `Night` |

`toTimeBucket()` on an `OffsetDateTime` or a `ZonedDateTime` applies the
same rule the kernel mines with. Use it rather than writing your own: a
bucket derived any other way asks about a slot the mining never used and
comes back empty, which looks exactly like "no established behavior".

## Querying by facet set

The low-level queries take the facet set directly. `getPatterns` returns the
patterns that match the context, `getUsualActions` the actions usually taken
in it:

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.patterns.FacetName
import io.cratis.chronicle.patterns.FacetSet
import io.cratis.chronicle.patterns.PatternGroupingKey

val context = FacetSet.empty
    .with(FacetName.Day, "Monday")
    .with(FacetName.CommandType, "PayInvoice")
val patterns = store.patterns.getPatterns(PatternGroupingKey("user-1"), context)
```

Leave the minimum confidence and maximum results out to use the values the
server is configured with. `getPatternsForScope` lists every pattern of a
scope, and `getScopes` every scope with patterns.

An event store implementation that does not support patterns reports
`IPatterns.unsupported`, which throws `PatternsNotSupported` when asked.
