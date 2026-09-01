```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Collections;
import java.util.List;

// Events
@EventType
record MbChildrenFullOrderCreated(String customerName) {}

@EventType
record MbChildrenFullLineItemAdded(
    String itemId,
    String productName,
    int initialQuantity,
    double unitPrice) {}

@EventType
record MbChildrenFullQuantityAdjusted(String itemId, int newQuantity) {}

@EventType
record MbChildrenFullLineItemRemoved(String itemId) {}

// Read Models
@ReadModel
@FromEvent(eventType = MbChildrenFullOrderCreated.class)
class MbChildrenFullOrder {
    @SetFrom(propertyPath = "customerName", eventType = MbChildrenFullOrderCreated.class)
    public String customer = "";

    @ChildrenFrom(eventType = MbChildrenFullLineItemAdded.class, key = "itemId", identifiedBy = "itemId")
    @ChildrenFrom(eventType = MbChildrenFullQuantityAdjusted.class, key = "itemId", identifiedBy = "itemId")
    @RemovedWith(eventType = MbChildrenFullLineItemRemoved.class, key = "itemId")
    public List<MbChildrenFullOrderLine> lines = Collections.emptyList();
}

class MbChildrenFullOrderLine {
    public String itemId = "";

    @SetFrom(propertyPath = "productName", eventType = MbChildrenFullLineItemAdded.class)
    public String product = "";

    @SetFrom(propertyPath = "initialQuantity", eventType = MbChildrenFullLineItemAdded.class)
    @SetFrom(propertyPath = "newQuantity", eventType = MbChildrenFullQuantityAdjusted.class)
    public int quantity = 0;

    @SetFrom(propertyPath = "unitPrice", eventType = MbChildrenFullLineItemAdded.class)
    public double unitPrice = 0.0;
}
```
