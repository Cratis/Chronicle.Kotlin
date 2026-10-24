```java
import io.cratis.fundamentals.concepts.ConceptAs;
import io.cratis.chronicle.confidentiality.Encrypted;

@Encrypted
record SecurityOverviewPartnerApiKey(String value) implements ConceptAs<String> {
}

record SecurityOverviewPartnerIntegrationConfigured(SecurityOverviewPartnerApiKey apiKey) {
}
```
