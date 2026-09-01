```java
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
}
```
