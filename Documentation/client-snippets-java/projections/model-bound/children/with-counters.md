```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.Decrement;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Collections;
import java.util.List;

@EventType
record MbChildrenCountersItemAddedToCart(
    String itemId,
    String productName,
    double price,
    int initialQuantity) {}

@EventType
record MbChildrenCountersQuantityIncreased(String itemId) {}

@EventType
record MbChildrenCountersQuantityDecreased(String itemId) {}

@ReadModel
@FromEvent(eventType = MbChildrenCountersItemAddedToCart.class)
class MbChildrenCountersShoppingCart {
    @ChildrenFrom(eventType = MbChildrenCountersItemAddedToCart.class, key = "itemId", identifiedBy = "itemId")
    @ChildrenFrom(eventType = MbChildrenCountersQuantityIncreased.class, key = "itemId", identifiedBy = "itemId")
    @ChildrenFrom(eventType = MbChildrenCountersQuantityDecreased.class, key = "itemId", identifiedBy = "itemId")
    public List<MbChildrenCountersCartItem> items = Collections.emptyList();
}

// Child type with its own projection attributes
class MbChildrenCountersCartItem {
    public String itemId = "";

    @SetFrom(propertyPath = "productName", eventType = MbChildrenCountersItemAddedToCart.class)
    public String productName = "";

    @SetFrom(propertyPath = "price", eventType = MbChildrenCountersItemAddedToCart.class)
    public double price = 0.0;

    @SetFrom(propertyPath = "initialQuantity", eventType = MbChildrenCountersItemAddedToCart.class)
    @Increment(eventType = MbChildrenCountersQuantityIncreased.class)
    @Decrement(eventType = MbChildrenCountersQuantityDecreased.class)
    public int quantity = 0;
}
```
