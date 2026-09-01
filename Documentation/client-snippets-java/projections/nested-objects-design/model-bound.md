```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Nested;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "nod-slice-created")
record NodSliceCreated(String name) {}

@EventType(id = "nod-command-set-for-slice")
record NodCommandSetForSlice(String name, String schema) {}

@EventType(id = "nod-command-cleared-for-slice")
class NodCommandClearedForSlice {}

@ReadModel
@FromEvent(eventType = NodSliceCreated.class)
class NodSlice {
    public String name = "";

    @Nested
    public NodCommandItem command = null;
}

@FromEvent(eventType = NodCommandSetForSlice.class)
@ClearWith(eventType = NodCommandClearedForSlice.class)
class NodCommandItem {
    public String name = "";
    public String schema = "";
}
```
