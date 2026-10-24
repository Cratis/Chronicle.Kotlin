```java
import io.cratis.fundamentals.concepts.ConceptAs;
import io.cratis.chronicle.confidentiality.Encrypted;

@Encrypted
record EncryptedAttrPartnerApiKey(String value) implements ConceptAs<String> {
}

record EncryptedAttrPartnerIntegrationConfiguredWithKey(EncryptedAttrPartnerApiKey apiKey) {
}
```
