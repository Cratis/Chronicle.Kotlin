```text
Java does not support this workflow yet.
`@Tag` exists, but only labels a reactor or reducer — neither `ProjectionsService.buildDeclarativeDefinition`
nor `buildModelBoundDefinition` ever reads a `Tag` annotation off a projection class, so tagging a
projection for discoverability has no effect yet.
```
