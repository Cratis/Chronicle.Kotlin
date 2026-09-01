```java title="Convention-based FromAll attribute"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromAll;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "product-renamed-from-all-convention")
record ProductRenamedFromAllConvention(String name, int version) {}

@EventType(id = "product-price-changed-from-all-convention")
record ProductPriceChangedFromAllConvention(double price, int version) {}

@ReadModel
@FromEvent(eventType = ProductRenamedFromAllConvention.class)
@FromEvent(eventType = ProductPriceChangedFromAllConvention.class)
record ProductVersionFromAllConvention(
    String name,
    double price,

    @FromAll
    int version
) {}
```
