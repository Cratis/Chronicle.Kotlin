```text
Java does not support this workflow yet.
The fluent `IProjectionBuilderFor` API has no `.nested()`/`.clearWith()`
equivalent — it can only map flat properties, not a single nested sub-object
that gets cleared by a specific event. Track the client SDK issue before
relying on nested object projection from Java.
```
