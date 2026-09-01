```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ArchitectureModelBoundItemAdded(String category) {}

@ReadModel
@FromEvent(eventType = ArchitectureModelBoundItemAdded.class, key = "category")
class ArchitectureModelBoundSummary {
    @Count(eventType = ArchitectureModelBoundItemAdded.class)
    public int count = 0;
}
```
