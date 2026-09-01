```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.events.EventType

// Property-level: requires repetition across every event
@EventType
data class PiiConceptsComparisonEmployeeRegistered(@Pii val name: String, val department: String)

@EventType
data class PiiConceptsComparisonEmployeeNameChanged(@Pii val newName: String) // must remember @Pii again

// Concept-level: declare once, apply everywhere automatically
@Pii
data class PiiConceptsComparisonPersonName(override val value: String) : ConceptAs<String>

// name is encrypted
@EventType
data class PiiConceptsComparisonEmployeeRegisteredGood(val name: PiiConceptsComparisonPersonName, val department: String)

// also encrypted, no extra annotation needed
@EventType
data class PiiConceptsComparisonEmployeeNameChangedGood(val newName: PiiConceptsComparisonPersonName)
```
