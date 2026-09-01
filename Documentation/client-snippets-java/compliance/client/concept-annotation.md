```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;

@Pii
record ComplianceClientPersonName(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
```
