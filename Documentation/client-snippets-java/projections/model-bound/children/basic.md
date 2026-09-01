```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Collections;
import java.util.List;

@EventType
record MbChildrenLineItemAdded(
    String itemId,
    String productName,
    int quantity,
    double price) {}

@ReadModel
@FromEvent(eventType = MbChildrenLineItemAdded.class)
class MbChildrenOrder {
    @ChildrenFrom(eventType = MbChildrenLineItemAdded.class, key = "itemId", identifiedBy = "itemId")
    public List<MbChildrenLineItem> items = Collections.emptyList();
}

class MbChildrenLineItem {
    public String itemId = "";  // Chronicle automatically discovers this as the key, via identifiedBy
    public String productName = "";
    public int quantity = 0;
    public double price = 0.0;
}
```
