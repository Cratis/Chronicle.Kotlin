```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "dec-constant-key-user-registered")
record DecConstantKeyUserRegistered(String name) {}

@EventType(id = "dec-constant-key-user-logged-in")
record DecConstantKeyUserLoggedIn() {}

@EventType(id = "dec-constant-key-user-logged-out")
record DecConstantKeyUserLoggedOut() {}

class DecConstantKeySiteStatistics {
    public int totalUsers = 0;
    public int activeSessions = 0;
}

class DecConstantKeySiteStatisticsProjection implements IProjectionFor<DecConstantKeySiteStatistics> {
    @Override
    public void define(IProjectionBuilderFor<DecConstantKeySiteStatistics> builder) {
        builder
            .from(DecConstantKeyUserRegistered.class, fb -> {
                fb.usingConstantKey("site");
                fb.count("totalUsers");
                return null; // Java lambda returning Unit
            })
            .from(DecConstantKeyUserLoggedIn.class, fb -> {
                fb.usingConstantKey("site");
                fb.increment("activeSessions");
                return null; // Java lambda returning Unit
            })
            .from(DecConstantKeyUserLoggedOut.class, fb -> {
                fb.usingConstantKey("site");
                fb.decrement("activeSessions");
                return null; // Java lambda returning Unit
            });
    }
}
```
