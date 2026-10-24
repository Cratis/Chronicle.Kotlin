```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.fundamentals.concepts.ConceptAs;

@Pii(description = "National ID number — sensitive personal identifier")
record PiiConceptsNationalIdNumber(String value) implements ConceptAs<String> {
}
```
