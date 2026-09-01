```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;

@Pii
record PiiAttrDateOfBirth(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

// The concept sits one level down, inside a value object.
record PiiAttrVerifiedDateOfBirth(PiiAttrDateOfBirth dateOfBirth, String verifiedBy) {
}

// Chronicle still finds it: dateOfBirth.dateOfBirth is encrypted, verifiedBy is not.
record PiiAttrExpressVerification(String name, PiiAttrVerifiedDateOfBirth dateOfBirth) {
}
```
