```java title="AutoMap with explicit mappings"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "auto-map-account-opened")
record AutoMapAccountOpened(String name, String email) {}

@EventType(id = "auto-map-account-email-changed")
record AutoMapAccountEmailChanged(String email) {}

class AutoMapAccount {
    public String name = "";
    public String email = "";
    public String status = "";
}

class AutoMapAccountProjection implements IProjectionFor<AutoMapAccount> {
    @Override
    public void define(IProjectionBuilderFor<AutoMapAccount> builder) {
        builder
            .from(AutoMapAccountOpened.class, fb -> {
                fb.<String>set("status").to(e -> "Active");
                return null; // Java lambda returning Unit
            })
            .from(AutoMapAccountEmailChanged.class);
    }
}
```
