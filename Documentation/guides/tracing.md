# Tracing

A request comes in, a command appends an event, a reactor picks it up and calls
something else. When that takes eight seconds, the question is which part of it
did — and without Chronicle in the trace, the answer is a gap.

The client reports spans through the OpenTelemetry API, so Chronicle work nests
under whatever span your application was already in.

## Turning it on

Nothing, if your application already runs the OpenTelemetry SDK. The client
depends on the API alone, which no-ops until an SDK is registered, so:

- an application that never instruments pays a virtual call and nothing else
- an application that does gets Chronicle spans without configuring the client

If your application holds its own `OpenTelemetry` rather than registering it
globally, hand it over:

<!-- validate: skip -->

```kotlin
val options = ChronicleOptions.development().copy(openTelemetry = myOpenTelemetry)
```

## What you get

| Span | Produced when |
| --- | --- |
| `Chronicle append <EventType>` | An event is appended |
| `Chronicle appendMany` | A batch is appended |
| `Chronicle observe <EventType>` | A reactor handles an event |
| `Chronicle reduce <EventType>` | A reducer folds an event |

Every span is reported under the `io.cratis.chronicle` instrumentation scope,
and carries the identifiers you would otherwise correlate by hand:

| Attribute | On |
| --- | --- |
| `chronicle.event_type` | Appends and observations |
| `chronicle.event_source_id` | Appends and observations |
| `chronicle.event_sequence_id` | All |
| `chronicle.event_store` | Appends |
| `chronicle.namespace` | Appends |
| `chronicle.observer_id` | Observations |
| `chronicle.sequence_number` | Observations |
| `chronicle.event_count` | Batch appends |
| `chronicle.is_replay` | Observations |

An append the kernel refuses, or a handler that throws, ends its span with an
error status and the exception recorded on it — which is usually the span you
came looking for.

## Reading a trace

An observation span is not a child of the append that produced the event. They
are separate pieces of work, often in separate processes, connected by the
correlation id rather than by span parentage. Query on
`chronicle.event_source_id` to see everything that happened to one event source.

Inside a reactor, anything else instrumented — an outgoing HTTP call, a database
query — nests under the observation span, because the span is made current for
the duration of the handler.

## Adding your own

`IReactorMiddleware` wraps every reactor handler invocation, which is where
application-specific instrumentation belongs rather than inside the reactors.
See [Artifact Registration](artifact-registration.md#reactor-middlewares).
