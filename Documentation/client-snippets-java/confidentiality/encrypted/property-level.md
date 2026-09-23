```java
import io.cratis.chronicle.confidentiality.Encrypted;
import io.cratis.chronicle.events.EventType;

@EventType
record EncryptedAttrPartnerIntegrationConfigured(
        @Encrypted String apiKey,
        String partnerName) {
}

// When this event is written, apiKey is encrypted. partnerName is stored as plaintext.
```
