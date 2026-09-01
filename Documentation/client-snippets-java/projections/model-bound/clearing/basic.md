```text
Java does not support this workflow yet.
`@ClearWith` targets `CLASS` only (it is meant to sit on a `@Nested` object's own class, alongside its
`@FromEvent`) — there is no way to clear a plain top-level scalar property back to null in response to
an event.
```
