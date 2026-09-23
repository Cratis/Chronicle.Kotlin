```java
import io.cratis.chronicle.concepts.EventSourceId;
import io.cratis.chronicle.confidentiality.Encrypted;

// This will throw EncryptedNotSupportedOnEventSourceId
@Encrypted
record EncryptedAttrPartnerId(String value) implements EventSourceId {
    @Override
    public String getValue() {
        return value;
    }
}
```
