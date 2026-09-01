```java
import io.cratis.chronicle.events.EventType;

// Generation 2 (current) - name has been split into firstName and lastName
@EventType(id = "dotnet-client-author-registered", generation = 2)
record MigrationsDotnetClientAuthorRegistered(String firstName, String lastName) {}

// Generation 1 (original) - same explicit id as the current generation, only the generation differs
@EventType(id = "dotnet-client-author-registered", generation = 1)
record MigrationsDotnetClientAuthorRegisteredV1(String name) {}
```
