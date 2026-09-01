```java
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

record ConstraintsModelBoundUniqueEmailAddress(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}

@EventType
record ConstraintsModelBoundUniqueAuthorRegistered(
    @Unique(id = "UniqueAuthorEmail") ConstraintsModelBoundUniqueEmailAddress email) {}
```
