```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecJoinsUserProjection : IProjectionFor<DecJoinsUser> {
    override fun define(builder: IProjectionBuilderFor<DecJoinsUser>) {
        builder
            .from(DecJoinsUserCreated::class)
            .from(DecJoinsUserAssignedToGroup::class) {
                it.usingKey("userId")
                it.set(DecJoinsUser::groupId).toEventSourceId()
            }
            .join(DecJoinsGroupCreated::class) { it.on(DecJoinsUser::groupId) }
            .join(DecJoinsGroupRenamed::class) { it.on(DecJoinsUser::groupId) }
    }
}
```
