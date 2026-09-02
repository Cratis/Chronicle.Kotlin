```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record NodDeclarativeSliceCreated(String name) {}

@EventType
record NodDeclarativeCommandSet(String name, String schema) {}

@EventType
record NodDeclarativeCommandCleared() {}

record NodDeclarativeSlice(String name, NodDeclarativeCommandItem command) {}

record NodDeclarativeCommandItem(String name, String schema) {}

class NodDeclarativeSliceProjection implements IProjectionFor<NodDeclarativeSlice> {
    @Override
    public void define(IProjectionBuilderFor<NodDeclarativeSlice> builder) {
        builder
            .from(NodDeclarativeSliceCreated.class)
            .nested("command", NodDeclarativeCommandItem.class, nested -> {
                nested
                    .from(NodDeclarativeCommandSet.class)
                    .clearWith(NodDeclarativeCommandCleared.class);
            });
    }
}
```
