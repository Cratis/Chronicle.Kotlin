```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.events.EventType

@Pii
data class ComplianceClientEmailAddress(override val value: String) : ConceptAs<String>

@EventType(id = "compliance-client-customer-registered")
data class ComplianceClientCustomerRegistered(
    val name: ComplianceClientPersonName,      // encrypted via concept type
    val email: ComplianceClientEmailAddress,   // encrypted via concept type
    @Pii val phoneNumber: String,              // encrypted via property annotation
    val country: String                        // plaintext
)
```
