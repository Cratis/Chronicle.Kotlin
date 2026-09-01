```text
Kotlin does not support this workflow yet.
`@Tag` only labels a reactor or reducer — neither the declarative nor the model-bound projection
builder in `ProjectionsService` ever reads a `Tag` annotation off a projection class, so tagging a
projection (fluent or model-bound) has no effect yet.
```
