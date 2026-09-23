```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

@EventType
data class ComplianceReadModelsPatientAdmitted(val name: ComplianceReadModelsPersonName, val admittedAt: Instant)

@ReadModel
data class ComplianceReadModelsPatientSummary(
    val patientId: String = "",
    @Pii val name: String = "",
    val lastAdmittedAt: Instant = Instant.EPOCH
)

@Reducer
class ComplianceReadModelsPatientSummaryReducer {
    fun admitted(
        event: ComplianceReadModelsPatientAdmitted,
        current: ComplianceReadModelsPatientSummary?,
        context: EventContext
    ): ComplianceReadModelsPatientSummary =
        ComplianceReadModelsPatientSummary(
            patientId = context.eventSourceId,
            name = event.name.value,
            lastAdmittedAt = event.admittedAt
        )
}
```
