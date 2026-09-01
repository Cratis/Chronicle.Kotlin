```kotlin title="Append child event to parent"
import io.cratis.chronicle.IEventStore

class GroupMembershipWithDefaultParentKey(private val eventStore: IEventStore) {
    suspend fun addUserToGroup(groupId: String, userId: String, role: String) =
        eventStore.eventLog.append(groupId, UserAddedWithDefaultParentKey(userId, role))
}
```
