```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

@EventType
record EmptyChildrenOrderPlaced(String customer) {}

@EventType
record EmptyChildrenLineItemAdded(String itemId, String productName, int quantity) {}

record EmptyChildrenLineItem(String id, String productName, int quantity) {}

// Non-nullable: an order with no line items reads back as an empty collection, so enumerating
// lines never needs a guard.
@ReadModel
@FromEvent(eventType = EmptyChildrenOrderPlaced.class)
class EmptyChildrenOrder {
    private String id = "";
    private String customer = "";
    @ChildrenFrom(eventType = EmptyChildrenLineItemAdded.class, key = "itemId")
    private List<EmptyChildrenLineItem> lines = List.of();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public List<EmptyChildrenLineItem> getLines() { return lines; }
    public void setLines(List<EmptyChildrenLineItem> lines) { this.lines = lines; }
}

// Nullable: "no line items yet" stays distinguishable from "an empty list".
@ReadModel
@FromEvent(eventType = EmptyChildrenOrderPlaced.class)
class EmptyChildrenDraftOrder {
    private String id = "";
    private String customer = "";
    @ChildrenFrom(eventType = EmptyChildrenLineItemAdded.class, key = "itemId")
    private List<EmptyChildrenLineItem> lines;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public List<EmptyChildrenLineItem> getLines() { return lines; }
    public void setLines(List<EmptyChildrenLineItem> lines) { this.lines = lines; }
}
```
