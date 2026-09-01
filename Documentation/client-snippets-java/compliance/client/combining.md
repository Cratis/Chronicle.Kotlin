```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.events.EventType;

@Pii
record ComplianceClientEmailAddress(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

@EventType(id = "ComplianceClientCustomerRegistered")
record ComplianceClientCustomerRegistered(
        ComplianceClientPersonName name,       // encrypted via concept type
        ComplianceClientEmailAddress email,    // encrypted via concept type
        @Pii String phoneNumber,               // encrypted via property annotation
        String country) {                      // plaintext
}
```
