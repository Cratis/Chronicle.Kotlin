```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.fundamentals.concepts.ConceptAs

@Pii(description = "Full legal name — required for contract identification")
data class PiiAttrLegalName(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}
```
