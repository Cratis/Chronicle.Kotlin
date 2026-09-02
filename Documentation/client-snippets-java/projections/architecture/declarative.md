```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record ArchitectureDeclarativeItemAdded(String category) {}

class ArchitectureDeclarativeSummary {
    public String category = "";
    public int count = 0;
}

class ArchitectureDeclarativeSummaryProjection implements IProjectionFor<ArchitectureDeclarativeSummary> {
    @Override
    public void define(IProjectionBuilderFor<ArchitectureDeclarativeSummary> builder) {
        builder
            .from(ArchitectureDeclarativeItemAdded.class, fb -> {
                fb.usingKey("category");
                fb.count("count");
            });
    }
}
```
