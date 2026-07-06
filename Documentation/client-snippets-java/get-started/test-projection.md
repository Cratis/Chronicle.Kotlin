```java title="The projection - builds queryable state"
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
@FromEvent(eventType = TestEvent.class)
record TestProjection(String message) {
    TestProjection() {
        this("");
    }
}
```
