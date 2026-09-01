```java title="Use the read model property name by convention"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEvery;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ProductRenamedForEveryConvention(String name, int version) {}

@EventType
record ProductPriceChangedForEveryConvention(double price, int version) {}

@ReadModel
@FromEvent(eventType = ProductRenamedForEveryConvention.class)
@FromEvent(eventType = ProductPriceChangedForEveryConvention.class)
class ProductVersionFromEveryConvention {
    public String name = "";
    public double price = 0.0;

    @FromEvery
    public int version = 0;
}
```
