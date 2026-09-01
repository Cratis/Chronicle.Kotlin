```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.Decrement;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-constant-key-order-placed-for-metrics")
class MbConstantKeyOrderPlacedForMetrics {}

@EventType(id = "mb-constant-key-user-logged-in")
class MbConstantKeyUserLoggedIn {}

@EventType(id = "mb-constant-key-user-logged-out")
class MbConstantKeyUserLoggedOut {}

@EventType(id = "mb-constant-key-error-occurred")
class MbConstantKeyErrorOccurred {}

@ReadModel
@FromEvent(eventType = MbConstantKeyOrderPlacedForMetrics.class)
@FromEvent(eventType = MbConstantKeyUserLoggedIn.class)
@FromEvent(eventType = MbConstantKeyUserLoggedOut.class)
@FromEvent(eventType = MbConstantKeyErrorOccurred.class)
class MbConstantKeySystemMetrics {
    @Count(eventType = MbConstantKeyOrderPlacedForMetrics.class, constantKey = "metrics")
    public int totalOrders = 0;

    @Increment(eventType = MbConstantKeyUserLoggedIn.class, constantKey = "metrics")
    @Decrement(eventType = MbConstantKeyUserLoggedOut.class, constantKey = "metrics")
    public int activeSessions = 0;

    @Count(eventType = MbConstantKeyErrorOccurred.class, constantKey = "metrics")
    public int totalErrors = 0;
}
```
