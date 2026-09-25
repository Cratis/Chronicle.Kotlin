```java
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEvery;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
@FromEvent(eventType = CrudComparisonCustomerRegistered.class)
@FromEvent(eventType = CrudComparisonAddressChanged.class)
class CrudComparisonCustomerCard {
    @FromEvery(contextProperty = "eventSourceId")
    public String id = "";
    public String name = "";

    // AutoMap covers the registration; a relocation also needs its address mapped explicitly,
    // because an event with only a count mapping is not auto-mapped.
    @SetFrom(propertyPath = "address", eventType = CrudComparisonAddressChanged.class)
    public String address = "";

    @Count(eventType = CrudComparisonAddressChanged.class)
    public int timesRelocated = 0;
}
```
