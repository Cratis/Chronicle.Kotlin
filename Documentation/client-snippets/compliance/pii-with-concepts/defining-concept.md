```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.fundamentals.concepts.ConceptAs

@Pii
data class PiiConceptsPersonName(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}
```
