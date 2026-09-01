```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "dec-index-user-registered")
record DecIndexUserRegistered(String name, String email, String registeredAt) {}

class DecIndexUserProfile {
    public String name = "";
    public String email = "";
    public String registeredAt = null;
}

class DecIndexUserProfileProjection implements IProjectionFor<DecIndexUserProfile> {
    @Override
    public void define(IProjectionBuilderFor<DecIndexUserProfile> builder) {
        builder.from(DecIndexUserRegistered.class);
    }
}
```
