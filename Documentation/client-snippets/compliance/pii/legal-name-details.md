```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs

@Pii(description = "Full legal name — required for contract identification")
data class PiiAttrLegalName(override val value: String) : ConceptAs<String>
```
