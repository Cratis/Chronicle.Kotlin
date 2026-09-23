```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.confidentiality.Encrypted
import io.cratis.chronicle.events.EventType

// Throws PiiAndEncryptedCombinedNotSupported at schema-generation time.
@EventType
data class EncryptedAttrCustomerRegistered(@Pii @Encrypted val someValue: String)
```
