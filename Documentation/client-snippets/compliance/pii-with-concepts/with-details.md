```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.fundamentals.concepts.ConceptAs

@Pii(
    description = "Collected under GDPR Art. 6(1)(b) — necessary for contract performance. " +
        "Retention: contract duration + 7 years."
)
data class PiiConceptsLegalName(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}
```
