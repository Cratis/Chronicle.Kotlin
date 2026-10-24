```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.fundamentals.concepts.ConceptAs;
import io.cratis.chronicle.events.EventType;

// Property-level: requires repetition across every event
@EventType
record PiiConceptsComparisonEmployeeRegistered(@Pii String name, String department) {
}

// must remember @Pii again
@EventType
record PiiConceptsComparisonEmployeeNameChanged(@Pii String newName) {
}

// Concept-level: declare once, apply everywhere automatically
@Pii
record PiiConceptsComparisonPersonName(String value) implements ConceptAs<String> {
}

// name is encrypted
@EventType
record PiiConceptsComparisonEmployeeRegisteredGood(PiiConceptsComparisonPersonName name, String department) {
}

// also encrypted, no extra annotation needed
@EventType
record PiiConceptsComparisonEmployeeNameChangedGood(PiiConceptsComparisonPersonName newName) {
}
```
