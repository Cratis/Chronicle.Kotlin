```text
Java does not support this workflow yet.
`ProjectionsService.collectChildrenMap` only reads `@FromEvent`/`@SetFrom`/aggregate annotations off
the child element type — it never recurses into a `@Nested` property declared on that child type, so a
nested object inside a child collection item is never wired up.
```
