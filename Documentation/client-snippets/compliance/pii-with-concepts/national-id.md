```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.fundamentals.concepts.ConceptAs

@Pii(description = "National ID number — sensitive personal identifier")
data class PiiConceptsNationalIdNumber(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}
```
