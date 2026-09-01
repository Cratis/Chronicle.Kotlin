```kotlin
import io.cratis.chronicle.events.EventType

// Generation 2 (current) - Name has been split into firstName and lastName
@EventType(id = "dotnet-client-author-registered", generation = 2)
data class MigrationsDotnetClientAuthorRegistered(val firstName: String, val lastName: String)

// Generation 1 (original) - same explicit id as the current generation, only the generation differs
@EventType(id = "dotnet-client-author-registered", generation = 1)
data class MigrationsDotnetClientAuthorRegisteredV1(val name: String)
```
