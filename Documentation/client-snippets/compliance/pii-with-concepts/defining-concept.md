```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs

@Pii
data class PiiConceptsPersonName(override val value: String) : ConceptAs<String>
```
