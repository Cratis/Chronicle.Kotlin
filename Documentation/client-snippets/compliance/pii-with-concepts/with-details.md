```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs

@Pii(
    description = "Collected under GDPR Art. 6(1)(b) — necessary for contract performance. " +
        "Retention: contract duration + 7 years."
)
data class PiiConceptsLegalName(override val value: String) : ConceptAs<String>
```
