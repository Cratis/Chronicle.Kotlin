```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs

@Pii(description = "National ID number — sensitive personal identifier")
data class PiiConceptsNationalIdNumber(override val value: String) : ConceptAs<String>
```
