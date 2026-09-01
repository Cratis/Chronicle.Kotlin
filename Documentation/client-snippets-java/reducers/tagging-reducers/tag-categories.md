```java
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.observation.Tag;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.UUID;

@ReadModel
record TaggingReducersCategoryExamples(UUID id) {
    TaggingReducersCategoryExamples() {
        this(new UUID(0, 0));
    }
}

// By domain
@Tag({"Sales", "Inventory", "Customer"})
// By purpose
@Tag({"Analytics", "Reporting", "Dashboard", "Auditing"})
// By stakeholder
@Tag({"Executive", "Operations", "Finance"})
// By data type
@Tag({"Aggregates", "Summaries", "Metrics"})
@Reducer
class TaggingReducersCategoryExamplesReducer {
}
```
