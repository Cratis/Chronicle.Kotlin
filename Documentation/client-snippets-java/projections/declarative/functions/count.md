```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record DecFunctionsUserLoggedIn(String username) {}

@EventType
record DecFunctionsUserPerformedAction(String username, String actionType) {}

class DecFunctionsUserActivity {
    public String username = "";
    public int loginCount = 0;
    public int actionCount = 0;
}

class DecFunctionsUserActivityProjection implements IProjectionFor<DecFunctionsUserActivity> {
    @Override
    public void define(IProjectionBuilderFor<DecFunctionsUserActivity> builder) {
        builder
            .autoMap()
            .from(DecFunctionsUserLoggedIn.class, fb -> {
                fb.count("loginCount");
                return null; // Java lambda returning Unit
            })
            .from(DecFunctionsUserPerformedAction.class, fb -> {
                fb.count("actionCount");
                return null; // Java lambda returning Unit
            });
    }
}
```
