```text
Java does not support this workflow yet.
`IFromBuilderFor`/`IChildFromBuilderFor` have no `clear()` method at all — the only clearing operation
in the fluent builder is `INestedBuilderFor.clearWith()`, which clears an entire nested object, not a
single member of the root, a nested object, or a child item.
```
