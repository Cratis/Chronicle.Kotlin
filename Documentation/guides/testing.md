# Testing

Specifying a Chronicle-backed slice used to mean driving the real client against
a running event store: a kernel, a sink, Docker, and a test that takes seconds
instead of milliseconds. That is an integration test, and it has its place — but
it is a poor place to specify "given these three events, the state should be
this".

`io.cratis:chronicle-testing` runs the same code with nothing behind it.

## Adding it

<!-- validate: skip -->

```kotlin
dependencies {
    testImplementation("io.cratis:chronicle-testing:<version>")
}
```

## Specifying what gets appended

`EventScenario` hands your code an `IEventSequence` that keeps events in a list,
then lets you ask what landed in it:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.testing.EventScenario
import io.cratis.chronicle.testing.shouldHaveAppended
import io.cratis.chronicle.testing.shouldHaveAppendedExactly

@Test
fun `registering an employee records that they were hired`() = runBlocking {
    val scenario = EventScenario()

    Registrations(scenario.eventLog)
        .register("employee-1", "Ada", "Lovelace")

    scenario.shouldHaveAppended<EmployeeHired>("employee-1") {
        it.firstName == "Ada"
    }
    scenario.shouldHaveAppendedExactly(1)
}
```

`given` appends preconditions — what was already true before the code under test
ran — so an assertion about what it appended does not have to count them:

<!-- validate: skip -->

```kotlin
scenario.given(
    "employee-1",
    EmployeeHired("Ada", "Lovelace", "Engineer")
)

Registrations(scenario.eventLog).promote("employee-1", "Principal")

scenario.shouldHaveAppended<EmployeePromoted>("employee-1")
```

| Assertion | Fails unless |
| --- | --- |
| `shouldHaveAppended<T>(source) { }` | One `T` matching the condition landed |
| `shouldHaveAppendedExactly(count)` | Exactly `count` events landed in total |
| `shouldHaveAppendedExactly<T>(count)` | Exactly `count` events of `T` landed |
| `shouldNotHaveAppended<T>()` | No `T` was appended |
| `shouldHaveAppendedNothing()` | Nothing at all was appended |

The assertions exist for their failure messages rather than for the check — a
failure prints every event that *was* appended, with its event source and its
JSON, which is usually enough to see the problem without a debugger.

## Specifying what a reducer folds

A reducer is a fold: events in, read model out. `ReadModelScenario` runs it the
way the client runs it against a real kernel — handlers discovered the same way,
chosen by event type the same way, invoked with the same shapes, awaited if they
suspend:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.testing.ReadModelScenario

@Test
fun `promotion changes the title and keeps the name`() = runBlocking {
    val scenario = ReadModelScenario<EmployeeState>(EmployeeStateReducer())

    val state = scenario.fold(
        "employee-1",
        EmployeeHired("Ada", "Lovelace", "Engineer"),
        EmployeePromoted("Principal Engineer")
    )

    assertEquals("Ada", state!!.firstName)
    assertEquals("Principal Engineer", state.title)
}
```

Chronicle folds each event source independently, and so does this — state does
not leak between event sources, which is exactly the mistake a reducer spec
should be able to catch. `stateFor(eventSourceId)` reads back what each one
folded to, and folding the same event source again continues from where it left
off, so a history can be built in stages.

## What is real and what is not

Real:

- The client's own serializer. Every event round-trips through it on the way in,
  so an event that could not survive a trip to the kernel fails here too — and so
  does a reducer that only works on an instance it was handed directly.
- Event contexts. Sequence numbers, event source ids, stream type and id, tags
  and the time it occurred are filled in the way the kernel fills them.
- Handler dispatch. The same discovery, the same shapes, the same `suspend`
  support.

Not real, and deliberately so:

- **Constraints.** Nothing is enforced, so every append succeeds. A constraint is
  the kernel's job and belongs in an integration test.
- **Projections.** Only reducers fold in-process. A projection is a declaration
  the kernel executes.
- **Concurrency scopes.** Accepted and ignored.
- **Observation.** Nothing observes, so reactors do not run and there is no
  replay.

When a spec needs any of those, it needs a kernel — and at that point it is an
integration test, which is a different and more expensive thing on purpose.
