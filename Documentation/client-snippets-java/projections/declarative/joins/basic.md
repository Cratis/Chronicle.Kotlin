```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecJoinsUserProjection implements IProjectionFor<DecJoinsUser> {
    @Override
    public void define(IProjectionBuilderFor<DecJoinsUser> builder) {
        builder
            .from(DecJoinsUserCreated.class)
            .from(DecJoinsUserAssignedToGroup.class, fb -> {
                fb.usingKey("userId");
                fb.set("groupId").toEventSourceId();
            })
            .join(DecJoinsGroupCreated.class, jb -> {
                jb.on("groupId");
            })
            .join(DecJoinsGroupRenamed.class, jb -> {
                jb.on("groupId");
            });
    }
}
```
