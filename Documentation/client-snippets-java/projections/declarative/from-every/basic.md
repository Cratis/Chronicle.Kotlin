```java title="Declarative FromEvery"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "user-created-declarative-every")
record UserCreatedDeclarativeEvery(String name, String email) {}

@EventType(id = "user-email-changed-declarative-every")
record UserEmailChangedDeclarativeEvery(String email) {}

class UserProfileDeclarativeEvery {
    public String name = "";
    public String email = "";
    public String lastUpdated = "";
}

class UserProfileDeclarativeEveryProjection implements IProjectionFor<UserProfileDeclarativeEvery> {
    @Override
    public void define(IProjectionBuilderFor<UserProfileDeclarativeEvery> builder) {
        builder
            .from(UserCreatedDeclarativeEvery.class)
            .from(UserEmailChangedDeclarativeEvery.class)
            .fromEvery(feb -> {
                feb.set("lastUpdated").toEventContextProperty("occurred");
                return null; // Java lambda returning Unit
            });
    }
}
```
