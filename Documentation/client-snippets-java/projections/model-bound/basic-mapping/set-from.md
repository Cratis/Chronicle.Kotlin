```java title="Model-bound set mapping"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "user-registered-for-contact")
record UserRegisteredForContact(String name, String email) {}

@ReadModel
@FromEvent(eventType = UserRegisteredForContact.class)
class UserContact {
    @SetFrom(propertyPath = "email", eventType = UserRegisteredForContact.class)
    public String email = "";

    @SetFrom(propertyPath = "name", eventType = UserRegisteredForContact.class)
    public String name = "";
}
```
