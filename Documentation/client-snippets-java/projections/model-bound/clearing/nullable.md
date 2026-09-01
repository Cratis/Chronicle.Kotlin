```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbClearingShiftPlanned(String assignee, int hours) {}

@EventType
record MbClearingShiftReleased() {}

@ReadModel
@FromEvent(eventType = MbClearingShiftPlanned.class)
class MbClearingShift {
    // A reference type, so "nobody is assigned" is a state the field can actually hold.
    @SetValue(eventType = MbClearingShiftReleased.class, clear = true)
    public String assignee;

    // Boxed Integer, for the same reason: 0 hours is a number of hours, not the absence of one. A
    // primitive int could never be cleared this way.
    @SetValue(eventType = MbClearingShiftReleased.class, clear = true)
    public Integer hours;
}
```
