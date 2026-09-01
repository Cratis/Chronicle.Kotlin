```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;

@Pii(description = "Full legal name — required for contract identification")
record PiiAttrLegalName(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
```
