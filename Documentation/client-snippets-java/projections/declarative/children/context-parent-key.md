```text
Java does not support this workflow yet.
`IChildFromBuilderFor` only has `usingParentKey(eventPropertyName: String)`, which reads the parent
key from an event property. There is no `usingParentKeyFromContext`-equivalent for explicitly
documenting that the EventSourceId is used — that is only ever the implicit default.
```
