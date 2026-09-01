```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-clearing-project-noted")
record MbClearingProjectNoted(String note) {}

@EventType(id = "mb-clearing-project-note-cleared")
record MbClearingProjectNoteCleared() {}

@ReadModel
@FromEvent(eventType = MbClearingProjectNoted.class)
class MbClearingProjectNotes {
    @SetFrom(propertyPath = "note", eventType = MbClearingProjectNoted.class)
    @SetValue(eventType = MbClearingProjectNoteCleared.class, clear = true)
    public String note;
}
```
