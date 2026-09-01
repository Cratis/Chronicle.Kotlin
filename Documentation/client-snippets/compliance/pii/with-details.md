```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs

@Pii(description = "Collected under GDPR Art. 6(1)(b) — necessary for contract performance")
data class PiiAttrPersonName(override val value: String) : ConceptAs<String>
```
