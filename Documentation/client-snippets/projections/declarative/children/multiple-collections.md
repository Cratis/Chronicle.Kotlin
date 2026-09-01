```kotlin title="Multiple child collections"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class GroupCreatedWithMultipleCollections(val name: String)

@EventType
data class MemberAddedToGroup(val userId: String, val role: String)

@EventType
data class TaskAssignedToGroup(val taskId: String, val title: String)

data class GroupWithMultipleCollections(
    val name: String = "",
    val members: List<GroupMemberInMultipleCollections> = emptyList(),
    val tasks: List<GroupTaskInMultipleCollections> = emptyList()
)

data class GroupMemberInMultipleCollections(
    val userId: String = "",
    val role: String = ""
)

data class GroupTaskInMultipleCollections(
    val taskId: String = "",
    val title: String = ""
)

class GroupWithMultipleCollectionsProjection : IProjectionFor<GroupWithMultipleCollections> {
    override fun define(builder: IProjectionBuilderFor<GroupWithMultipleCollections>) {
        builder
            .from(GroupCreatedWithMultipleCollections::class)
            .children(GroupWithMultipleCollections::members, GroupMemberInMultipleCollections::class) { children ->
                children
                    .identifiedBy("userId")
                    .from(MemberAddedToGroup::class) { it.usingKey("userId") }
            }
            .children(GroupWithMultipleCollections::tasks, GroupTaskInMultipleCollections::class) { children ->
                children
                    .identifiedBy("taskId")
                    .from(TaskAssignedToGroup::class) { it.usingKey("taskId") }
            }
    }
}
```
