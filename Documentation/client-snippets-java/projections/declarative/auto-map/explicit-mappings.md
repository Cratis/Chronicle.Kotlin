```java title="AutoMap with explicit mappings"
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record AutoMapAccountOpened(String name, String email) {}

@EventType
record AutoMapAccountEmailChanged(String email) {}

class AutoMapAccount {
    public String name = "";
    public String email = "";
    public String status = "";
    public String createdAt = "";
}

class AutoMapAccountProjection implements IProjectionFor<AutoMapAccount> {
    @Override
    public void define(IProjectionBuilderFor<AutoMapAccount> builder) {
        builder
            .from(AutoMapAccountOpened.class, fb -> {
                fb.<String>set("status").toValue("Active");
                fb.<String>set("createdAt").toEventContextProperty("occurred");
            })
            .from(AutoMapAccountEmailChanged.class);
    }
}
```
