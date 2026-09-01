```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbClearingProjectNoted(String note) {}

@EventType
record MbClearingProjectNoteCleared() {}

@ReadModel
@FromEvent(eventType = MbClearingProjectNoted.class)
class MbClearingProjectNotes {
    @SetFrom(propertyPath = "note", eventType = MbClearingProjectNoted.class)
    @SetValue(eventType = MbClearingProjectNoteCleared.class, clear = true)
    public String note;
}
```
