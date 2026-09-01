```kotlin
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

data class ConstraintsModelBoundUniqueEmailAddress(override val value: String) : ConceptAs<String>

@EventType
data class ConstraintsModelBoundUniqueAuthorRegistered(
    @Unique(id = "UniqueAuthorEmail") val email: ConstraintsModelBoundUniqueEmailAddress
)
```
