```java title="Convention-based mapping"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "convention-user-registered")
record ConventionUserRegistered(String name, String email, String registeredAt) {}

@ReadModel
@FromEvent(eventType = ConventionUserRegistered.class)
class ConventionUser {
    public String name = "";
    public String email = "";
    public String registeredAt = "";
}
```
