```kotlin
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.confidentiality.Encrypted
import io.cratis.chronicle.confidentiality.EncryptionScope

// One key per partner (EncryptionScope.Subject, the default).
@Encrypted
data class EncryptedAttrPartnerApiKeyScoped(override val value: String) : ConceptAs<String>

// One key for every partner in the namespace.
@Encrypted(scope = EncryptionScope.Namespace)
data class EncryptedAttrPartnerWebhookSecret(override val value: String) : ConceptAs<String>

// One key for the whole installation.
@Encrypted(scope = EncryptionScope.Global)
data class EncryptedAttrLicenseToken(override val value: String) : ConceptAs<String>
```
