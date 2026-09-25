```java title="Combine FromAll with event-specific mappings"
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record OrderCreatedDeclarativeAll(String orderNumber) {}

@EventType
record OrderShippedDeclarativeAll(String trackingNumber) {}

class OrderDeclarativeAll {
    public String orderNumber = "";
    public String status = "";
    public String lastModified = "";
}

class OrderDeclarativeAllProjection implements IProjectionFor<OrderDeclarativeAll> {
    @Override
    public void define(IProjectionBuilderFor<OrderDeclarativeAll> builder) {
        builder
            .fromAll(feb -> {
                feb.set("lastModified").toEventContextProperty("occurred");
            })
            .from(OrderCreatedDeclarativeAll.class, fb -> {
                fb.<String>set("status").toValue("Placed");
            })
            .from(OrderShippedDeclarativeAll.class, fb -> {
                fb.<String>set("status").toValue("Shipped");
            });
    }
}
```
