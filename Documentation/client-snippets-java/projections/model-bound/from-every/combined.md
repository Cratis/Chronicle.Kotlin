```java title="Combine specific mappings with every-event metadata"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEvery;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "user-registered-for-every")
record UserRegisteredForEvery(String name, String email) {}

@EventType(id = "user-name-changed-for-every")
record UserNameChangedForEvery(String newName) {}

@EventType(id = "user-email-changed-for-every")
record UserEmailChangedForEvery(String newEmail) {}

@ReadModel
@FromEvent(eventType = UserRegisteredForEvery.class)
@FromEvent(eventType = UserNameChangedForEvery.class)
@FromEvent(eventType = UserEmailChangedForEvery.class)
class UserProfileFromEvery {
    @SetFrom(propertyPath = "name", eventType = UserRegisteredForEvery.class)
    @SetFrom(propertyPath = "newName", eventType = UserNameChangedForEvery.class)
    public String name = "";

    @SetFrom(propertyPath = "email", eventType = UserRegisteredForEvery.class)
    @SetFrom(propertyPath = "newEmail", eventType = UserEmailChangedForEvery.class)
    public String email = "";

    @FromEvery(contextProperty = "occurred")
    public String lastUpdated = "";
}
```
