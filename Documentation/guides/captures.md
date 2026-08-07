# Captures

Not everything that matters happens inside your application. An exchange rate
moves, a partner posts a webhook, a supplier's API changes a price. Those are
facts too — and the usual answer is a scheduled job somewhere that polls, maps,
and appends, which nobody owns and nobody notices when it stops.

A capture is that job, declared rather than written, and run by the kernel.

## Declaring one

Implement `ICapture` and discovery saves it with the kernel on connect and
starts it, so the source is live as soon as your application is:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.captures.ICapture

class ExchangeRates : ICapture {
    override val id = "exchange-rates"

    override val declaration = """
        capture ExchangeRates
            from api "https://api.example.com/rates" every 5 minutes
                append RateObserved
                    set currency to base
                    set rate to value
    """
}
```

The body is a Capture Declaration Language document. The language belongs to
the kernel, which is what parses and runs it — see the
[Chronicle documentation](https://github.com/Cratis/Chronicle) for its full
shape.

Set `startOnRegistration` to `false` to have the capture saved but held — an
environment where the source is not reachable, or one where starting it is an
operator's decision.

A declaration the kernel cannot parse is reported to standard error naming the
capture, the line and the column. One bad capture does not take the rest of
your application's registration down with it.

## When the declaration is not known at build time

`store.captures` is the whole surface, for declarations that come from
somewhere else — an editor, configuration, an operator:

<!-- validate: skip -->

```kotlin
// Check as it is typed, without saving anything
val problems = store.captures.validate(declaration)

// Save, then start
when (val result = store.captures.save("exchange-rates", declaration)) {
    is CaptureDeclarationResult.Accepted -> store.captures.start(result.capture.id)
    is CaptureDeclarationResult.Rejected -> result.messages.forEach(::println)
}
```

| Member | Does |
| --- | --- |
| `getAll()` | Every capture the store holds, running or not |
| `observeAll()` | The same, re-emitted on every change |
| `save(id, declaration)` | Saves under an id, replacing what was there |
| `validate(declaration)` | Checks a declaration without saving anything |
| `start(id)` | Starts it appending |
| `stop(id)` | Stops it. It stays saved |
| `delete(id)` | Removes it entirely |

A declaration is a piece of text, so the first thing that can go wrong is that
the kernel cannot make sense of it. That is an ordinary outcome of writing in a
language rather than an exception, so `save` returns `Accepted` or `Rejected`
and every message names a line and column.

Messages do not by themselves mean rejection — a declaration can be accepted
with something worth telling you. `isSuccess`, or the shape of the result, is
what says whether it took.

## What stopping does not undo

Events a capture already appended are facts. Stopping or deleting the capture
changes nothing about them — they stay exactly where they are, and everything
projected from them stays as it was.

From Java, use `CapturesServiceJavaBridge`.
