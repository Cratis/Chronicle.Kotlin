```java title="Include child projection events"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.time.Instant;
import java.util.List;

@EventType
record OrderCreatedDeclarativeEveryChildren(String orderNumber) {}

@EventType
record ItemAddedDeclarativeEveryChildren(String orderId, String productId, String productName, int quantity) {}

@EventType
record ItemQuantityChangedDeclarativeEveryChildren(String orderId, String productId, int quantity) {}

class OrderDeclarativeEveryChildren {
    public String orderNumber = "";
    public Instant lastModified = Instant.EPOCH;
    public List<OrderItemDeclarativeEveryChildren> items = List.of();
}

record OrderItemDeclarativeEveryChildren(String productId, String productName, int quantity) {}

class OrderDeclarativeEveryChildrenProjection implements IProjectionFor<OrderDeclarativeEveryChildren> {
    @Override
    public void define(IProjectionBuilderFor<OrderDeclarativeEveryChildren> builder) {
        builder
            .from(OrderCreatedDeclarativeEveryChildren.class)
            .fromEvery(feb -> {
                feb.set("lastModified").toEventContextProperty("occurred");
                return null; // Java lambda returning Unit
            })
            .children("items", OrderItemDeclarativeEveryChildren.class, children -> {
                children
                    .identifiedBy("productId")
                    .from(ItemAddedDeclarativeEveryChildren.class, fb -> {
                        fb.usingKey("productId");
                        fb.usingParentKey("orderId");
                        return null; // Java lambda returning Unit
                    })
                    .from(ItemQuantityChangedDeclarativeEveryChildren.class, fb -> {
                        fb.usingKey("productId");
                        fb.usingParentKey("orderId");
                        return null; // Java lambda returning Unit
                    });
                return null; // Java lambda returning Unit
            });
    }
}
```
