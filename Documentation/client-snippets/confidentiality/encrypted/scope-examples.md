```kotlin
import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.chronicle.confidentiality.Encrypted
import io.cratis.chronicle.confidentiality.EncryptionScope

// One key per partner (EncryptionScope.Subject, the default).
@Encrypted
data class EncryptedAttrPartnerApiKeyScoped(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}

// One key for every partner in the namespace.
@Encrypted(scope = EncryptionScope.Namespace)
data class EncryptedAttrPartnerWebhookSecret(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}

// One key for the whole installation.
@Encrypted(scope = EncryptionScope.Global)
data class EncryptedAttrLicenseToken(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}
```
