```kotlin title="Parent key from event content"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "group-created-with-event-parent-key")
data class GroupCreatedWithEventParentKey(val name: String)

@EventType(id = "user-added-with-event-parent-key")
data class UserAddedWithEventParentKey(val groupId: String, val userId: String, val role: String)

data class GroupWithEventParentKey(
    val name: String = "",
    val members: List<GroupMemberWithEventParentKey> = emptyList()
)

data class GroupMemberWithEventParentKey(
    val userId: String = "",
    val role: String = ""
)

class GroupWithEventParentKeyProjection : IProjectionFor<GroupWithEventParentKey> {
    override fun define(builder: IProjectionBuilderFor<GroupWithEventParentKey>) {
        builder
            .from(GroupCreatedWithEventParentKey::class)
            .children(GroupWithEventParentKey::members, GroupMemberWithEventParentKey::class) { children ->
                children
                    .identifiedBy("userId")
                    .from(UserAddedWithEventParentKey::class) {
                        it.usingParentKey("groupId")
                            .usingKey("userId")
                    }
            }
    }
}
```
