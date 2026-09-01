```java title="Multiple convention events"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ConventionUserProfileCreated(String name, String email) {}

@EventType
record ConventionUserProfileUpdated(String name, String email, String phone) {}

@ReadModel
@FromEvent(eventType = ConventionUserProfileCreated.class)
@FromEvent(eventType = ConventionUserProfileUpdated.class)
class ConventionUserProfile {
    public String name = "";
    public String email = "";
    public String phone = "";
}
```
