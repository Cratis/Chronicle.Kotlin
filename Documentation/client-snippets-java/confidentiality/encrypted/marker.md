```java
import io.cratis.chronicle.confidentiality.Encrypted;
import io.cratis.chronicle.confidentiality.EncryptionScope;
import io.cratis.chronicle.events.EventType;

// @interface Encrypted {
//     EncryptionScope scope() default EncryptionScope.Subject;
//     String description() default "";
// }
// Apply it to a class (a concept type), a record component, a field, or a constructor parameter.
// Both elements are optional; description records why the value needs encryption.
@EventType
record EncryptedMarkerPartnerIntegrationConfigured(
        @Encrypted(
            scope = EncryptionScope.Subject,
            description = "Partner API credential - an operational secret with no data subject")
        String apiKey) {
}
```
