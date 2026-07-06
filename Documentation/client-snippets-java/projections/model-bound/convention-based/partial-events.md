```java title="Partial event shapes"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "convention-partial-user-registered")
record ConventionPartialUserRegistered(String email) {}

@EventType(id = "convention-partial-user-completed")
record ConventionPartialUserCompleted(String firstName, String lastName, String phone) {}

@ReadModel
@FromEvent(eventType = ConventionPartialUserRegistered.class)
@FromEvent(eventType = ConventionPartialUserCompleted.class)
class ConventionPartialUser {
    public String email = "";
    public String firstName = "";
    public String lastName = "";
    public String phone = "";
}
```
