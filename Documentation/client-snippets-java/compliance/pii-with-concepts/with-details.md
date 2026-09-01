```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;

@Pii(
    description = "Collected under GDPR Art. 6(1)(b) — necessary for contract performance. "
        + "Retention: contract duration + 7 years."
)
record PiiConceptsLegalName(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
```
