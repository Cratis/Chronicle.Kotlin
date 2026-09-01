```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record UserAddedWithEventParentKey(String groupId, String userId, String role) {}

class GroupMembershipWithEventParentKey {
    private final EventStore eventStore;

    GroupMembershipWithEventParentKey(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    void addUserToGroup(String userId, String groupId, String role) {
        EventLogJavaBridge.append(eventStore.getEventLog(), userId, new UserAddedWithEventParentKey(groupId, userId, role), null);
    }
}
```
