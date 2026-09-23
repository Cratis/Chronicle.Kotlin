```java
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.confidentiality.Encrypted;
import io.cratis.chronicle.confidentiality.EncryptionScope;

// One key per partner (EncryptionScope.Subject, the default).
@Encrypted
record EncryptedAttrPartnerApiKeyScoped(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

// One key for every partner in the namespace.
@Encrypted(scope = EncryptionScope.Namespace)
record EncryptedAttrPartnerWebhookSecret(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

// One key for the whole installation.
@Encrypted(scope = EncryptionScope.Global)
record EncryptedAttrLicenseToken(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
```
