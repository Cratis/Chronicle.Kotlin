```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-set-value-thing-happened")
record MbSetValueThingHappened() {}

@ReadModel
@FromEvent(eventType = MbSetValueThingHappened.class)
class MbSetValueThing {
    @SetValue(eventType = MbSetValueThingHappened.class, value = "pending")
    public String statusLabel = "";

    // Kotlin annotation parameters can only be compile-time constants of a fixed set of types, so
    // SetValue always carries its constant as a string - a numeric or boolean value is written out
    // as its literal text and interpreted against the field's declared type.
    @SetValue(eventType = MbSetValueThingHappened.class, value = "42")
    public int priority = 0;

    @SetValue(eventType = MbSetValueThingHappened.class, value = "true")
    public boolean isActive = false;

    @SetValue(eventType = MbSetValueThingHappened.class, value = "3.14")
    public double score = 0.0;
}
```
