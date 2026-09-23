```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@Pii
record ComplianceReadModelsPersonName(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

@EventType
record ComplianceReadModelsEmployeeRegistered(ComplianceReadModelsPersonName name, String department) {
}

@ReadModel
@FromEvent(eventType = ComplianceReadModelsEmployeeRegistered.class)
class ComplianceReadModelsEmployee {
    public String id = "";
    public String name = "";        // mapped from ComplianceReadModelsPersonName - stored encrypted at rest
    public String department = "";
}
```
