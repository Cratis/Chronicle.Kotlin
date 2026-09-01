```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs

@Pii
data class PiiAttrDateOfBirth(override val value: String) : ConceptAs<String>

// The concept sits one level down, inside a value object.
data class PiiAttrVerifiedDateOfBirth(val dateOfBirth: PiiAttrDateOfBirth, val verifiedBy: String)

// Chronicle still finds it: dateOfBirth.dateOfBirth is encrypted, verifiedBy is not.
data class PiiAttrExpressVerification(val name: String, val dateOfBirth: PiiAttrVerifiedDateOfBirth)
```
