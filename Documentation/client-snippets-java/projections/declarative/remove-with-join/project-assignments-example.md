```text
Java does not support this workflow yet.
`IChildrenBuilderFor`/`IChildFromBuilderFor` have no `removedWith`/`removedWithJoin` method — removing
a single item from a child collection, whether from the same stream or a joined one, is only possible
today with model-bound `@RemovedWith`/`@RemovedWithJoin` on a `@ChildrenFrom` property.
```
