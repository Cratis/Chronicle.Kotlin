```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.AddFrom;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.Decrement;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.projections.SubtractFrom;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;

// Events
@EventType(id = "mb-counters-user-logged-in-full")
record MbCountersUserLoggedInFull(Instant timestamp) {}

@EventType(id = "mb-counters-user-logged-out-full")
record MbCountersUserLoggedOutFull(Instant timestamp) {}

@EventType(id = "mb-counters-purchase-made")
record MbCountersPurchaseMade(double amount) {}

@EventType(id = "mb-counters-refund-issued")
record MbCountersRefundIssued(double amount) {}

// Read Model
@ReadModel
@FromEvent(eventType = MbCountersUserLoggedInFull.class)
@FromEvent(eventType = MbCountersUserLoggedOutFull.class)
@FromEvent(eventType = MbCountersPurchaseMade.class)
@FromEvent(eventType = MbCountersRefundIssued.class)
class MbCountersUserActivity {
    // Track login/logout counts
    @Count(eventType = MbCountersUserLoggedInFull.class)
    public int totalLogins = 0;

    @Count(eventType = MbCountersUserLoggedOutFull.class)
    public int totalLogouts = 0;

    // Track active sessions
    @Increment(eventType = MbCountersUserLoggedInFull.class)
    @Decrement(eventType = MbCountersUserLoggedOutFull.class)
    public int activeSessions = 0;

    // Track transaction counts
    @Count(eventType = MbCountersPurchaseMade.class)
    public int purchaseCount = 0;

    @Count(eventType = MbCountersRefundIssued.class)
    public int refundCount = 0;

    // Track transaction values
    @AddFrom(eventType = MbCountersPurchaseMade.class, eventPropertyName = "amount")
    @SubtractFrom(eventType = MbCountersRefundIssued.class, eventPropertyName = "amount")
    public double netSpent = 0.0;
}
```
