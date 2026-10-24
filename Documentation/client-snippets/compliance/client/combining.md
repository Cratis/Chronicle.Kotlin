```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.chronicle.events.EventType

@Pii
data class ComplianceClientEmailAddress(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}

@EventType
data class ComplianceClientCustomerRegistered(
    val name: ComplianceClientPersonName,      // encrypted via concept type
    val email: ComplianceClientEmailAddress,   // encrypted via concept type
    @Pii val phoneNumber: String,              // encrypted via property annotation
    val country: String                        // plaintext
)
```
