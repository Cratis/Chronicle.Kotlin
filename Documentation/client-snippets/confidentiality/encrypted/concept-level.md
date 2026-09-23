```kotlin
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.confidentiality.Encrypted

@Encrypted
data class EncryptedAttrPartnerApiKey(override val value: String) : ConceptAs<String>

data class EncryptedAttrPartnerIntegrationConfiguredWithKey(val apiKey: EncryptedAttrPartnerApiKey)
```
