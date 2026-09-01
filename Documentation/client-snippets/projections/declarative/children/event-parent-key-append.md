```kotlin title="Append child event with parent key"
import io.cratis.chronicle.IEventStore

class GroupMembershipWithEventParentKey(private val eventStore: IEventStore) {
    suspend fun addUserToGroup(userId: String, groupId: String, role: String) =
        eventStore.eventLog.append(userId, UserAddedWithEventParentKey(groupId, userId, role))
}
```
