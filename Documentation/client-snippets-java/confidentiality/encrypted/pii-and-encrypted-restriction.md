```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.confidentiality.Encrypted;
import io.cratis.chronicle.events.EventType;

// Throws PiiAndEncryptedCombinedNotSupported at schema-generation time.
@EventType
record EncryptedAttrCustomerRegistered(@Pii @Encrypted String someValue) {
}
```
