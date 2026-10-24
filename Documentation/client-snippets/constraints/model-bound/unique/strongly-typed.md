```kotlin
import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

data class ConstraintsModelBoundUniqueEmailAddress(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}

@EventType
data class ConstraintsModelBoundUniqueAuthorRegistered(
    @Unique(id = "UniqueAuthorEmail") val email: ConstraintsModelBoundUniqueEmailAddress
)
```
