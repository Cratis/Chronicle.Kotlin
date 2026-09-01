```java title="Combine FromAll with event-specific mappings"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "order-created-declarative-all")
record OrderCreatedDeclarativeAll(String orderNumber) {}

@EventType(id = "order-shipped-declarative-all")
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
                return null; // Java lambda returning Unit
            })
            .from(OrderCreatedDeclarativeAll.class, fb -> {
                fb.<String>set("status").to(e -> "Placed");
                return null; // Java lambda returning Unit
            })
            .from(OrderShippedDeclarativeAll.class, fb -> {
                fb.<String>set("status").to(e -> "Shipped");
                return null; // Java lambda returning Unit
            });
    }
}
```
