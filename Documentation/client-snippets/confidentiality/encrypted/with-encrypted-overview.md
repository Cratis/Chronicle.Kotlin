```kotlin
import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.chronicle.confidentiality.Encrypted

@Encrypted
data class SecurityOverviewPartnerApiKey(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}

data class SecurityOverviewPartnerIntegrationConfigured(val apiKey: SecurityOverviewPartnerApiKey)
```
