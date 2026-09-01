```java title="Nested property on the parent"
import io.cratis.chronicle.projections.Nested;

record ParentWithNestedProperty(
    @Nested
    NestedPropertyChild child
) {}

record NestedPropertyChild(String name, String description) {}
```
