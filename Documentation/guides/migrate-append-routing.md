---
title: Migrate append routing
description: Preserve explicit legacy event routes when upgrading the Kotlin and Java client.
---

The next major client release stops choosing routing defaults on your
behalf. Before upgrading, decide whether new events should use the
kernel's defaults or continue the routes your application already uses.
Existing stored events are not moved or rewritten.

## Check the route your application needs

With the matching kernel release, omitted or empty routing resolves as
follows. Upgrade the kernel before deploying the new client.

| Route field | Previous single/rich-batch default | Kernel-owned default |
| --- | --- | --- |
| `eventSourceType` | `Default` | `Default` |
| `eventStreamType` | `Default` | `All` |
| `eventStreamId` | The event-source id | `Default` |

This is a **breaking routing change**, not just a serialization change.
Review stream-filtered observers, queries, and concurrency scopes before
switching. Earlier single-source `appendMany` calls also dropped supplied
routing options; they now honor them for every event in the batch.

A missing value (`null`) and an empty string both leave the decision to
the kernel. The client never replaces an explicit nonempty route, including
`Default`, `All`, or a stream id equal to the event-source id. An empty
string is not a way to select a legacy route.

## Preserve the legacy route explicitly

For single appends, set all three route fields to the values you intend
to keep. These helpers accept your application's annotated event object.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventSequence

suspend fun appendToLegacyRoute(
    sequence: IEventSequence,
    eventSourceId: String,
    event: Any
) = sequence.append(
    eventSourceId,
    event,
    AppendOptions(
        eventSourceType = "Default",
        eventStreamType = "Default",
        eventStreamId = eventSourceId
    )
)
```

<!-- validate: declarations -->

```java
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.BlockingEventSequence;

class LegacyRouting {
    static AppendResult appendToLegacyRoute(
            BlockingEventSequence sequence,
            String eventSourceId,
            Object event) {
        var options = new AppendOptionsBuilder()
            .eventSourceType("Default")
            .eventStreamType("Default")
            .eventStreamId(eventSourceId)
            .build();
        return sequence.append(eventSourceId, event, options);
    }
}
```

Pass the same options to `appendMany(eventSourceId, events, options)` for a
single-source batch. For a batch of `EventForEventSourceId` values, set
`eventSourceType`, `eventStreamType`, and `eventStreamId` on **each** value;
use that value's own event-source id for the legacy stream id. Mixed
explicit and omitted routes remain mixed rather than inheriting a route
from another event.

If your application previously passed options to single-source batches,
check stored event contexts instead of assuming those options took effect.
To continue a stored route, supply its actual values explicitly.

## Keep concurrency and compliance choices separate

No concurrency defaults change: ordinary appends still use
`ConcurrencyScope.none` when omitted; an explicitly supplied `none` or
`notSet` is preserved. Rich batches still send exactly the scope-map entries
you supplied. A source omitted from that map stays omitted. Routing fields
are never copied into concurrency scopes automatically.

Subject behavior also stays unchanged: an omitted subject falls back to
the event-source id, while an explicit subject is forwarded unchanged.
Occurred time, tags, causation, identity, and correlation are retained when
a single-source batch uses explicit routing.

## Verify against the kernel

Read newly appended events back and check their route and metadata using
`getForEventSourceIdAndEventTypes` or `getFromSequenceNumber`. These reads
carry stored routing rather than guessing it from the append request.

Do not use local `appendOperations` notifications as proof of stored
routing until authoritative append receipts are supported. The in-memory
testing sequence emulates the canonical kernel route defaults for fast
specifications, but it is a server test double, not an integration test.
Verify the deployed kernel's behavior before switching production routes.
