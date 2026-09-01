```text
Kotlin does not support this workflow yet.
`IChildrenBuilderFor`/`IChildFromBuilderFor` have no `removedWithJoin` method — removal via a joined
event only exists for the whole read model instance (`IProjectionBuilderFor.removedWithJoin`), not
for a single item inside a `children()` collection.
```
