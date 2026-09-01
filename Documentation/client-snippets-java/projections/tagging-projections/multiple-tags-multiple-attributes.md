```text
Java does not support this workflow yet.
`@Tag` only labels a reactor or reducer — neither `ProjectionsService.buildDeclarativeDefinition` nor
`buildModelBoundDefinition` ever reads a `Tag` annotation off a projection class, so repeated `@Tag`
attributes on a projection have no effect yet.
```
