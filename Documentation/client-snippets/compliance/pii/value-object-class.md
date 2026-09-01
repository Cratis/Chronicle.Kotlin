```kotlin
import io.cratis.chronicle.compliance.Pii

// Every value this type holds is personal, so mark the type once.
@Pii
data class PiiAttrDiagnosis(val condition: String, val diagnosedBy: String)

// Both condition and diagnosedBy are encrypted wherever a PiiAttrDiagnosis appears.
data class PiiAttrPatientRecord(val name: String, val diagnosis: PiiAttrDiagnosis)
```
