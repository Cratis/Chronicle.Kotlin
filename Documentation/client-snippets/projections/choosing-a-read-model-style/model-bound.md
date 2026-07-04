```text
Kotlin does not support this workflow yet.
The model-bound `@SetFrom` annotation maps a property from an event, but there is
no equivalent to C#'s `[SetValue<TEvent>(...)]` for assigning a constant value —
so a flag like `isBorrowed` cannot be toggled per event type through model-bound
attributes alone. Track the client SDK issue, or use a reducer instead.
```
