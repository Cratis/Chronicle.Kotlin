```java title="Initial values"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

enum InitialValuesUserStatus {
    Inactive,
    Active
}

@EventType(id = "initial-values-user-created")
record InitialValuesUserCreated(String name, String email) {}

// Field initializers are the read model's initial values — the kernel builds the starting
// instance by calling the no-argument constructor.
class InitialValuesUserProfile {
    public String name = "Unknown user";
    public String email = "";
    public InitialValuesUserStatus status = InitialValuesUserStatus.Inactive;
    public String lastLogin = null;
    public int loginCount = 0;
    public boolean isVerified = false;
}

class InitialValuesUserProfileProjection implements IProjectionFor<InitialValuesUserProfile> {
    @Override
    public void define(IProjectionBuilderFor<InitialValuesUserProfile> builder) {
        builder.from(InitialValuesUserCreated.class);
    }
}
```
