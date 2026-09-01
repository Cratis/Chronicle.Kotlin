```java title="AutoMap by convention"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record AutoMapUserCreated(String name, String email) {}

@EventType
record AutoMapUserRenamed(String name) {}

class AutoMapUser {
    public String name = "";
    public String email = "";
}

class AutoMapUserProjection implements IProjectionFor<AutoMapUser> {
    @Override
    public void define(IProjectionBuilderFor<AutoMapUser> builder) {
        builder.from(AutoMapUserCreated.class);
        builder.from(AutoMapUserRenamed.class);
    }
}
```
