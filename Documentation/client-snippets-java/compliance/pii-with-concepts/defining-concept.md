```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.fundamentals.concepts.ConceptAs;

@Pii
record PiiConceptsPersonName(String value) implements ConceptAs<String> {
}
```
