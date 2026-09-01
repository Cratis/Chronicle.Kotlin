```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;

@Pii
record PiiAttrConceptPersonName(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
```
