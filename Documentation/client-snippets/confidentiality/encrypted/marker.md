```kotlin
import io.cratis.chronicle.confidentiality.Encrypted
import io.cratis.chronicle.confidentiality.EncryptionScope
import io.cratis.chronicle.events.EventType

// annotation class Encrypted(
//     val scope: EncryptionScope = EncryptionScope.Subject,
//     val description: String = ""
// )
// Apply it to a class (a concept type), a property, a field, or a constructor parameter.
// Both arguments are optional; description records why the value needs encryption.
@EventType
data class EncryptedMarkerPartnerIntegrationConfigured(
    @Encrypted(EncryptionScope.Subject, "Partner API credential - an operational secret with no data subject")
    val apiKey: String
)
```
