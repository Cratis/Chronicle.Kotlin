```java
import io.cratis.chronicle.events.EventType;
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
    public TaggingReducersComplianceReport on(TaggingReducersAuditEntryRecorded event) {
        return new TaggingReducersComplianceReport(event.status());
    }
}

@EventType
record TaggingReducersAuditEntryRecorded(String status) {}
```
