```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record UserAddedWithDefaultParentKey(String userId, String role) {}

class GroupMembershipWithDefaultParentKey {
    private final EventStore eventStore;

    GroupMembershipWithDefaultParentKey(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    void addUserToGroup(String groupId, String userId, String role) {
        EventLogJavaBridge.append(eventStore.getEventLog(), groupId, new UserAddedWithDefaultParentKey(userId, role), null);
    }
}
```
