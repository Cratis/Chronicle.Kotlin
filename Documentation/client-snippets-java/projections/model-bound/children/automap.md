```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Collections;
import java.util.List;

@EventType(id = "mb-children-automap-line-item-added")
record MbChildrenAutoMapLineItemAdded(
    String itemId,
    String productName,
    int quantity,
    double price) {}

@ReadModel
@FromEvent(eventType = MbChildrenAutoMapLineItemAdded.class)
class MbChildrenAutoMapOrder {
    @ChildrenFrom(eventType = MbChildrenAutoMapLineItemAdded.class, key = "itemId", identifiedBy = "itemId")
    public List<MbChildrenAutoMapLineItem> items = Collections.emptyList();
}

class MbChildrenAutoMapLineItem {
    public String itemId = "";
    public String productName = "";  // Automatically mapped from MbChildrenAutoMapLineItemAdded.productName
    public int quantity = 0;          // Automatically mapped from MbChildrenAutoMapLineItemAdded.quantity
    public double price = 0.0;        // Automatically mapped from MbChildrenAutoMapLineItemAdded.price
}
```
