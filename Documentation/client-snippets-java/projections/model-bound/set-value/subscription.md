```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-set-value-subscription-started")
record MbSetValueSubscriptionStarted() {}

@EventType(id = "mb-set-value-subscription-paused")
record MbSetValueSubscriptionPaused() {}

@EventType(id = "mb-set-value-subscription-canceled")
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
