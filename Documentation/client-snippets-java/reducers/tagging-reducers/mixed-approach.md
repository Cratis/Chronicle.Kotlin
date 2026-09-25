```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.observation.Tag;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record TaggingReducersExecutiveDashboard(int metricCount) {
    TaggingReducersExecutiveDashboard() {
        this(0);
    }
}

@Reducer
@Tag({"Analytics", "Reporting"})
@Tag("Executive")
class TaggingReducersExecutiveDashboardReducer {
    public TaggingReducersExecutiveDashboard on(TaggingReducersKpiRecorded event) {
        return new TaggingReducersExecutiveDashboard(event.metricCount());
    }
}

@EventType
record TaggingReducersKpiRecorded(int metricCount) {}
```
