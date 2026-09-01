```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.events.EventType;

// Property-level: requires repetition across every event
@EventType(id = "PiiConceptsComparisonEmployeeRegistered")
record PiiConceptsComparisonEmployeeRegistered(@Pii String name, String department) {
}

// must remember @Pii again
@EventType(id = "PiiConceptsComparisonEmployeeNameChanged")
record PiiConceptsComparisonEmployeeNameChanged(@Pii String newName) {
}

// Concept-level: declare once, apply everywhere automatically
@Pii
record PiiConceptsComparisonPersonName(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

// name is encrypted
@EventType(id = "PiiConceptsComparisonEmployeeRegisteredGood")
record PiiConceptsComparisonEmployeeRegisteredGood(PiiConceptsComparisonPersonName name, String department) {
}

// also encrypted, no extra annotation needed
@EventType(id = "PiiConceptsComparisonEmployeeNameChangedGood")
record PiiConceptsComparisonEmployeeNameChangedGood(PiiConceptsComparisonPersonName newName) {
}
```
