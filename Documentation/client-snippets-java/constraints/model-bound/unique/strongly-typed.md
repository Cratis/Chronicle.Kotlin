```java
import io.cratis.fundamentals.concepts.ConceptAs;
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

record ConstraintsModelBoundUniqueEmailAddress(String value) implements ConceptAs<String> {
}

@EventType
record ConstraintsModelBoundUniqueAuthorRegistered(
    @Unique(id = "UniqueAuthorEmail") ConstraintsModelBoundUniqueEmailAddress email) {}
```
