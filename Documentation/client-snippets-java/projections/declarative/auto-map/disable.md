```java title="Disable AutoMap"
// Requires io.cratis:chronicle 6.5.0 or later.
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
        builder.noAutoMap()
            .from(AutoMapDisabledAccountRegistered.class, fb -> {
                fb.<String>set("name").toProperty("accountName");
                fb.<String>set("email").toProperty("contactEmail");
                fb.<String>set("createdAt").toEventContextProperty("occurred");
            });
    }
}
```
