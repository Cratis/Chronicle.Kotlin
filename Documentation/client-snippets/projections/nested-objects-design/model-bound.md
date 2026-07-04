```text
Kotlin does not support this workflow yet.
There is no `@Nested`/`@ClearWith` equivalent for model-bound projections —
`@FromEvent`/`@SetFrom` can only map flat properties, not a single nested
sub-object that gets cleared by a specific event. Track the client SDK issue
before relying on nested object projection from Kotlin.
```
