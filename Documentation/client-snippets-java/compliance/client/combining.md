```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.fundamentals.concepts.ConceptAs;
import io.cratis.chronicle.events.EventType;

@Pii
record ComplianceClientEmailAddress(String value) implements ConceptAs<String> {
}

@EventType
record ComplianceClientCustomerRegistered(
        ComplianceClientPersonName name,       // encrypted via concept type
        ComplianceClientEmailAddress email,    // encrypted via concept type
        @Pii String phoneNumber,               // encrypted via property annotation
        String country) {                      // plaintext
}
```
