```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.fundamentals.concepts.ConceptAs

@Pii
data class ComplianceClientPersonName(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}
```
