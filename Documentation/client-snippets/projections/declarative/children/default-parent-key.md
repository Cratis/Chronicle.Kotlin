```kotlin title="Default parent key"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "group-created-with-default-parent-key")
data class GroupCreatedWithDefaultParentKey(val name: String)

@EventType(id = "user-added-with-default-parent-key")
data class UserAddedWithDefaultParentKey(val userId: String, val role: String)

data class GroupWithDefaultParentKey(
    val name: String = "",
    val members: List<GroupMemberWithDefaultParentKey> = emptyList()
)

data class GroupMemberWithDefaultParentKey(
    val userId: String = "",
    val role: String = ""
)

class GroupWithDefaultParentKeyProjection : IProjectionFor<GroupWithDefaultParentKey> {
    override fun define(builder: IProjectionBuilderFor<GroupWithDefaultParentKey>) {
        builder
            .from(GroupCreatedWithDefaultParentKey::class)
            .children(GroupWithDefaultParentKey::members, GroupMemberWithDefaultParentKey::class) { children ->
                children
                    .identifiedBy("userId")
                    .from(UserAddedWithDefaultParentKey::class) { it.usingKey("userId") }
            }
    }
}
```
