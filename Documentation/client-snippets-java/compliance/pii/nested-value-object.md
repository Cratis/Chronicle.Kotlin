```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.fundamentals.concepts.ConceptAs;

@Pii
record PiiAttrDateOfBirth(String value) implements ConceptAs<String> {
}

// The concept sits one level down, inside a value object.
record PiiAttrVerifiedDateOfBirth(PiiAttrDateOfBirth dateOfBirth, String verifiedBy) {
}

// Chronicle still finds it: dateOfBirth.dateOfBirth is encrypted, verifiedBy is not.
record PiiAttrExpressVerification(String name, PiiAttrVerifiedDateOfBirth dateOfBirth) {
}
```
