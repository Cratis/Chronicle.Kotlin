```text
Java does not support this workflow yet.
The fluent `IProjectionBuilderFor`/`ISetBuilderFor` API supports `.to()`,
`.toEventSourceId()`, and `.toProperty()`, but has no `.toValue()`-equivalent for
assigning a constant value — so a flag like `isBorrowed` cannot be toggled per
event type through the declarative builder alone. Track the client SDK issue,
or use a reducer instead.
```
