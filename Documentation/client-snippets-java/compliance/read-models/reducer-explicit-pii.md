```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;

@EventType
record ComplianceReadModelsPatientAdmitted(ComplianceReadModelsPersonName name, Instant admittedAt) {
}

@ReadModel
record ComplianceReadModelsPatientSummary(String patientId, @Pii String name, Instant lastAdmittedAt) {
    ComplianceReadModelsPatientSummary() {
        this("", "", Instant.EPOCH);
    }
}

@Reducer
class ComplianceReadModelsPatientSummaryReducer {
    ComplianceReadModelsPatientSummary admitted(
        ComplianceReadModelsPatientAdmitted event,
        ComplianceReadModelsPatientSummary current,
        EventContext context) {
        return new ComplianceReadModelsPatientSummary(
            context.getEventSourceId(),
            event.name().value(),
            event.admittedAt());
    }
}
```
