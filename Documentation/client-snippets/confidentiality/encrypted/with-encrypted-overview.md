```kotlin
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.confidentiality.Encrypted

@Encrypted
data class SecurityOverviewPartnerApiKey(override val value: String) : ConceptAs<String>

data class SecurityOverviewPartnerIntegrationConfigured(val apiKey: SecurityOverviewPartnerApiKey)
```
