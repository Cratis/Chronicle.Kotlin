```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.EventSequence;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbEventSeqOrderPlaced(double amount) {}

@ReadModel
@EventSequence("custom-sequence")
@FromEvent(eventType = MbEventSeqOrderPlaced.class)
record MbEventSeqOrderSummary(
    @SetFrom(propertyPath = "amount", eventType = MbEventSeqOrderPlaced.class)
    double totalAmount
) {}
```
