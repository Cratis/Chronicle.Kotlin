```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.NoAutoMap;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

@EventType(id = "mb-children-no-automap-order-placed")
record MbChildrenNoAutoMapOrderPlaced(String orderId) {}

@EventType(id = "mb-children-no-automap-line-item-added")
record MbChildrenNoAutoMapLineItemAdded(
    String itemId,
    String productName,
    int quantity,
    double price
) {}

@ReadModel
@FromEvent(eventType = MbChildrenNoAutoMapOrderPlaced.class)
record MbChildrenNoAutoMapOrder(
    @ChildrenFrom(eventType = MbChildrenNoAutoMapLineItemAdded.class, key = "itemId")
    List<MbChildrenNoAutoMapLineItem> items
) {}

// Now you must use @SetFrom for each property
@NoAutoMap
record MbChildrenNoAutoMapLineItem(
    @SetFrom(propertyPath = "productName", eventType = MbChildrenNoAutoMapLineItemAdded.class)
    String productName,

    @SetFrom(propertyPath = "quantity", eventType = MbChildrenNoAutoMapLineItemAdded.class)
    int quantity,

    @SetFrom(propertyPath = "price", eventType = MbChildrenNoAutoMapLineItemAdded.class)
    double price
) {}
```
