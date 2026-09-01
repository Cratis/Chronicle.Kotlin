```text
Java does not support this workflow yet.
`fromEvery()`/`children()` both take Kotlin-only parameters (`IFromEveryBuilderFor.set()` needs a
`KProperty1`, and `children()` needs a `KProperty1` plus a `KClass<TChild>`) with no `Class<T>`-based
overload and no bridge in `io.cratis.chronicle.java`.
```
