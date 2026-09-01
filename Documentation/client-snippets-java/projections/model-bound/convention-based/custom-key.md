```java title="Custom key"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ConventionUserRegisteredWithKey(String userId, String name, String email) {}

@ReadModel
@FromEvent(eventType = ConventionUserRegisteredWithKey.class, key = "userId")
class ConventionUserById {
    public String name = "";
    public String email = "";
}
```
