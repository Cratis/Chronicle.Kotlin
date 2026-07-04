```text
Java does not support this workflow yet.
`AppendOptions` only carries a `correlationId` — there is no way to attach tags
or a custom event stream type when appending from Java. Track the client SDK
issue before relying on metadata-filtered observers from Java.
```
