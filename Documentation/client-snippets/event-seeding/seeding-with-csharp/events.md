```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class EvtSeedingUserRegistered(val email: String, val displayName: String)

@EventType
data class EvtSeedingEmailVerified(val email: String)

@EventType
data class EvtSeedingProfileUpdated(val displayName: String)

@EventType
data class EvtSeedingOrderPlaced(val userId: String, val amount: Double)
```
