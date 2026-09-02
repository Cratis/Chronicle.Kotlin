```java title="Declarative FromEvery"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record UserCreatedDeclarativeEvery(String name, String email) {}

@EventType
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
            });
    }
}
```
