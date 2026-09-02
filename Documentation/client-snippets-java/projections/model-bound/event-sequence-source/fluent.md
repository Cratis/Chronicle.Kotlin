```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.Projection;

@EventType
record MbEventSeqFluentOrderPlaced(double amount) {
}

class MbEventSeqFluentOrderSummary {
    public double totalAmount = 0;
}

@Projection(eventSequence = "custom-sequence")
class MbEventSeqFluentOrderProjection implements IProjectionFor<MbEventSeqFluentOrderSummary> {
    @Override
    public void define(IProjectionBuilderFor<MbEventSeqFluentOrderSummary> builder) {
        builder.from(MbEventSeqFluentOrderPlaced.class,
            from -> from.<Double>set("totalAmount").to(MbEventSeqFluentOrderPlaced::amount));
    }
}
```
