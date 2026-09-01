```text
Java does not support this workflow yet.
Renaming a property while mapping requires `IFromBuilderFor.set(property: KProperty1<...>)`, which
Java cannot call — there is no `Class<T>`/String-based overload and no bridge in
`io.cratis.chronicle.java`. AutoMap-only fluent projections work from Java, but this example needs an
explicit rename (`amount` → `totalAmount`).
```
