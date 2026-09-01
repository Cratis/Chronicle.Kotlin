```text
Kotlin does not support this workflow yet.
`ISetBuilderFor` (used by both the top-level `from()` and the child `from()` builders) only has
`to()`, `toEventSourceId()` and `toProperty()` — `toEventContextProperty()` exists solely on
`IAllSetBuilderFor`, reachable only through `fromEvery()`/`fromAll()`, which apply to every event the
projection observes rather than a single event type inside a child collection.
```
