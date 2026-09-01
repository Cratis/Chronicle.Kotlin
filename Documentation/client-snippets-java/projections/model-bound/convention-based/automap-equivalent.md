```java title="Model-bound and declarative AutoMap"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ConventionEquivalentUserRegistered(String name, String email) {}

@ReadModel
@FromEvent(eventType = ConventionEquivalentUserRegistered.class)
class ConventionEquivalentUser {
    public String name = "";
    public String email = "";
}

class ConventionEquivalentProjection implements IProjectionFor<ConventionEquivalentUser> {
    @Override
    public void define(IProjectionBuilderFor<ConventionEquivalentUser> builder) {
        builder.from(ConventionEquivalentUserRegistered.class);
    }
}
```
