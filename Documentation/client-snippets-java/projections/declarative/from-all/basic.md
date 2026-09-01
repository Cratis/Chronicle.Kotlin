```java title="Declarative FromAll"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "user-created-declarative-all")
record UserCreatedDeclarativeAll(String name, String email) {}

@EventType(id = "user-email-changed-declarative-all")
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
                return null; // Java lambda returning Unit
            });
    }
}
```
