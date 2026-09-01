```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.AddFrom
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.projections.SubtractFrom
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

// Events
@EventType
data class MbCountersUserLoggedInFull(val timestamp: Instant)

@EventType
data class MbCountersUserLoggedOutFull(val timestamp: Instant)

@EventType
data class MbCountersPurchaseMade(val amount: Double)

@EventType
data class MbCountersRefundIssued(val amount: Double)

// Read Model
@ReadModel
@FromEvent(MbCountersUserLoggedInFull::class)
@FromEvent(MbCountersUserLoggedOutFull::class)
@FromEvent(MbCountersPurchaseMade::class)
@FromEvent(MbCountersRefundIssued::class)
data class MbCountersUserActivity(
    // Track login/logout counts
    @Count(MbCountersUserLoggedInFull::class)
    val totalLogins: Int = 0,

    @Count(MbCountersUserLoggedOutFull::class)
    val totalLogouts: Int = 0,

    // Track active sessions
    @Increment(MbCountersUserLoggedInFull::class)
    @Decrement(MbCountersUserLoggedOutFull::class)
    val activeSessions: Int = 0,

    // Track transaction counts
    @Count(MbCountersPurchaseMade::class)
    val purchaseCount: Int = 0,

    @Count(MbCountersRefundIssued::class)
    val refundCount: Int = 0,

    // Track transaction values
    @AddFrom(MbCountersPurchaseMade::class, "amount")
    @SubtractFrom(MbCountersRefundIssued::class, "amount")
    val netSpent: Double = 0.0
)
```
