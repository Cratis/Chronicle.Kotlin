```java
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.confidentiality.Encrypted;

@Encrypted
record EncryptedAttrPartnerApiKey(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

record EncryptedAttrPartnerIntegrationConfiguredWithKey(EncryptedAttrPartnerApiKey apiKey) {
}
```
