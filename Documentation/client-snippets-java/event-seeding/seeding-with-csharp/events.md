```java
import io.cratis.chronicle.events.EventType;

@EventType
record EvtSeedingUserRegistered(String email, String displayName) {}

@EventType
record EvtSeedingEmailVerified(String email) {}

@EventType
record EvtSeedingProfileUpdated(String displayName) {}

@EventType
record EvtSeedingOrderPlaced(String userId, double amount) {}
```
