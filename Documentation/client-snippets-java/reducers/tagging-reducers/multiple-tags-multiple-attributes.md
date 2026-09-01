```java
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.observation.Tag;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record TaggingReducersComplianceReport(String status) {
    TaggingReducersComplianceReport() {
        this("");
    }
}

@Reducer
@Tag("Analytics")
@Tag("Compliance")
@Tag("Auditing")
class TaggingReducersComplianceReportReducer {
}
```
