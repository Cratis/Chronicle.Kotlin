```text
Kotlin does not support this workflow yet.
`IChildrenBuilderFor`/`IChildFromBuilderFor` have no `removedWith`-equivalent method, and
`ProjectionsService` never wires a removal list into the children definition it builds from the
fluent builder — only model-bound `@RemovedWith`/`@RemovedWithJoin` on a `@ChildrenFrom` property
can remove a single child today.
```
