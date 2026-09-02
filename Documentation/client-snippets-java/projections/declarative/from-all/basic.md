```java title="Declarative FromAll"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record UserCreatedDeclarativeAll(String name, String email) {}

@EventType
record UserEmailChangedDeclarativeAll(String email) {}

class UserProfileDeclarativeAll {
    public String name = "";
    public String email = "";
    public String lastUpdated = "";
}

class UserProfileDeclarativeAllProjection implements IProjectionFor<UserProfileDeclarativeAll> {
    @Override
    public void define(IProjectionBuilderFor<UserProfileDeclarativeAll> builder) {
        builder
            .from(UserCreatedDeclarativeAll.class)
            .from(UserEmailChangedDeclarativeAll.class)
            .fromAll(feb -> {
                feb.set("lastUpdated").toEventContextProperty("occurred");
            });
    }
}
```
