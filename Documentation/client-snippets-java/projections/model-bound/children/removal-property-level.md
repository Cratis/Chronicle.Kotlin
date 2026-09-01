```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Collections;
import java.util.List;

@EventType
record MbChildrenRemovalPropertyLineItemAdded(String itemId, String description) {}

@EventType
record MbChildrenRemovalPropertyLineItemRemoved(String itemId) {}

@ReadModel
@FromEvent(eventType = MbChildrenRemovalPropertyLineItemAdded.class)
class MbChildrenRemovalPropertyOrder {
    @ChildrenFrom(eventType = MbChildrenRemovalPropertyLineItemAdded.class, key = "itemId", identifiedBy = "itemId")
    @RemovedWith(eventType = MbChildrenRemovalPropertyLineItemRemoved.class, key = "itemId")
    public List<MbChildrenRemovalPropertyOrderLine> lines = Collections.emptyList();
}

class MbChildrenRemovalPropertyOrderLine {
    public String itemId = "";

    @SetFrom(propertyPath = "description", eventType = MbChildrenRemovalPropertyLineItemAdded.class)
    public String description = "";
}
```
