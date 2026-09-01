```java title="Equivalent explicit mappings"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ExplicitConventionUserRegistered(String name, String email, String registeredAt) {}

@ReadModel
@FromEvent(eventType = ExplicitConventionUserRegistered.class)
class ExplicitConventionUser {
    @SetFrom(propertyPath = "name")
    public String name = "";

    @SetFrom(propertyPath = "email")
    public String email = "";

    @SetFrom(propertyPath = "registeredAt")
    public String registeredAt = "";
}
```
