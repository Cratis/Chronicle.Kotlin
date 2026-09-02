```java title="Fluent FromAll mapping"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.time.Instant;

@EventType
record InventoryRegisteredFromAll(String productName) {
}

@EventType
record InventoryAdjustedFromAll(int quantity) {
}

class InventoryStatusFromAll {
    public String productName = "";
    public Instant lastUpdated = Instant.EPOCH;
}

class InventoryStatusFromAllProjection implements IProjectionFor<InventoryStatusFromAll> {
    @Override
    public void define(IProjectionBuilderFor<InventoryStatusFromAll> builder) {
        builder
            .from(InventoryRegisteredFromAll.class)
            .from(InventoryAdjustedFromAll.class)
            .fromAll(all -> {
                all.set("lastUpdated").toEventContextProperty("occurred");
            });
    }
}
```
