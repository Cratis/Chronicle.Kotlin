```java title="Convention-based set mapping"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record UserRegisteredForProfile(String name, String email) {}

@ReadModel
@FromEvent(eventType = UserRegisteredForProfile.class)
class UserProfile {
    @SetFrom
    public String name = "";

    @SetFrom
    public String email = "";
}
```
