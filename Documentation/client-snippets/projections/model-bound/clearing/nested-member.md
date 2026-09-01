```text
Kotlin does not support this workflow yet.
`@ClearWith` targets `CLASS` only, so it cannot sit on a single property inside a `@Nested` object's
type to clear just that member — only the whole nested object can be cleared, by placing `@ClearWith`
on the nested class itself (see `model-bound/nested/clear-with`).
```
