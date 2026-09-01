```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbSetValueSubscriptionStarted() {}

@EventType
record MbSetValueSubscriptionPaused() {}

@EventType
record MbSetValueSubscriptionCanceled() {}

@ReadModel
@FromEvent(eventType = MbSetValueSubscriptionStarted.class)
@FromEvent(eventType = MbSetValueSubscriptionPaused.class)
@FromEvent(eventType = MbSetValueSubscriptionCanceled.class)
class MbSetValueSubscription {
    @SetValue(eventType = MbSetValueSubscriptionStarted.class, value = "active")
    @SetValue(eventType = MbSetValueSubscriptionPaused.class, value = "paused")
    @SetValue(eventType = MbSetValueSubscriptionCanceled.class, value = "canceled")
    public String state = "";
}
```
