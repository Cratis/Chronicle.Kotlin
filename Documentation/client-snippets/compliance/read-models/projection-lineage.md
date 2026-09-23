```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@Pii
data class ComplianceReadModelsPersonName(override val value: String) : ConceptAs<String>

@EventType
data class ComplianceReadModelsEmployeeRegistered(val name: ComplianceReadModelsPersonName, val department: String)

@ReadModel
@FromEvent(ComplianceReadModelsEmployeeRegistered::class)
data class ComplianceReadModelsEmployee(
    val id: String = "",
    val name: String = "",        // mapped from ComplianceReadModelsPersonName - stored encrypted at rest
    val department: String = ""
)
```
