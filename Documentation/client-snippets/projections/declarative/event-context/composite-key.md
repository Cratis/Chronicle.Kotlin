```text
Kotlin does not support this workflow yet.
`ICompositeKeyBuilderFor.property(targetPropertyName, eventPropertyName)` only pairs a read model
property with a plain event property name — there is no way to source a composite key part from an
event context property (e.g. `Occurred`, `SequenceNumber`), and there is no typed composite-key
result shape like C#'s `UsingCompositeKey<TKey>`.
```
