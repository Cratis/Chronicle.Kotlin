```java
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.confidentiality.Encrypted;

@Encrypted
record SecurityOverviewPartnerApiKey(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

record SecurityOverviewPartnerIntegrationConfigured(SecurityOverviewPartnerApiKey apiKey) {
}
```
