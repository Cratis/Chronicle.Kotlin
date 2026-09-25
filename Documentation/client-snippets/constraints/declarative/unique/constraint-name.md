```kotlin
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

// Model-bound equivalent: the explicit id names the constraint UniqueEmail.
@EventType
data class ConstraintsUniqueNamedUserRegistered(@Unique(id = "UniqueEmail") val email: String)
```
