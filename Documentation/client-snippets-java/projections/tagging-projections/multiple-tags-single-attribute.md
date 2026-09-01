```text
Java does not support this workflow yet.
`@Tag` only labels a reactor or reducer — neither `ProjectionsService.buildDeclarativeDefinition` nor
`buildModelBoundDefinition` ever reads a `Tag` annotation off a projection class, so a `@Tag` with
multiple values on a projection has no effect yet.
```
