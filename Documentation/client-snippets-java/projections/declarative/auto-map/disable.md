```java title="Disable AutoMap"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record AutoMapDisabledAccountRegistered(String accountName, String contactEmail) {}

class AutoMapDisabledAccount {
    public String name = "";
    public String email = "";
    public String createdAt = "";
}

class AutoMapDisabledAccountProjection implements IProjectionFor<AutoMapDisabledAccount> {
    @Override
    public void define(IProjectionBuilderFor<AutoMapDisabledAccount> builder) {
        builder
            .noAutoMap()
            .from(AutoMapDisabledAccountRegistered.class, fb -> {
                fb.set("name").toProperty("accountName");
                fb.set("email").toProperty("contactEmail");
                fb.set("createdAt").toEventContextProperty("occurred");
                return null; // Java lambda returning Unit
            });
    }
}
```
