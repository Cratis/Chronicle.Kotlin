```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-constant-key-user-registered")
class MbConstantKeyUserRegistered {}

@EventType(id = "mb-constant-key-order-placed-global")
class MbConstantKeyOrderPlacedGlobal {}

@ReadModel
@FromEvent(eventType = MbConstantKeyUserRegistered.class)
@FromEvent(eventType = MbConstantKeyOrderPlacedGlobal.class)
class MbConstantKeyUserDashboard {
    public String name = "";

    // A per-instance property alongside a constant-keyed one on the same read model
    @Count(eventType = MbConstantKeyOrderPlacedGlobal.class, constantKey = "global-stats")
    public int platformTotalOrders = 0;
}
```
