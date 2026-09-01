```kotlin title="Projection with children"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class GroupCreatedForChildren(val name: String, val description: String)

@EventType
data class UserAddedToGroupForChildren(val userId: String, val role: String)

@EventType
data class UserRoleChangedForChildren(val userId: String, val role: String)

data class GroupForChildren(
    val name: String = "",
    val description: String = "",
    val members: List<GroupMemberForChildren> = emptyList()
)

data class GroupMemberForChildren(
    val userId: String = "",
    val role: String = ""
)

class GroupProjectionForChildren : IProjectionFor<GroupForChildren> {
    override fun define(builder: IProjectionBuilderFor<GroupForChildren>) {
        builder
            .from(GroupCreatedForChildren::class)
            .children(GroupForChildren::members, GroupMemberForChildren::class) { children ->
                children
                    .identifiedBy("userId")
                    .from(UserAddedToGroupForChildren::class) { it.usingKey("userId") }
                    .from(UserRoleChangedForChildren::class) { it.usingKey("userId") }
            }
    }
}
```
