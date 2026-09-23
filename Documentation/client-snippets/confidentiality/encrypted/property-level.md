```kotlin
import io.cratis.chronicle.confidentiality.Encrypted
import io.cratis.chronicle.events.EventType

@EventType
data class EncryptedAttrPartnerIntegrationConfigured(
    @Encrypted val apiKey: String,
    val partnerName: String
)

// When this event is written, apiKey is encrypted. partnerName is stored as plaintext.
```
