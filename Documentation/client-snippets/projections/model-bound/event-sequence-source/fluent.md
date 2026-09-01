```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.Projection

@EventType
data class MbEventSeqFluentOrderPlaced(val amount: Double)

data class MbEventSeqFluentOrderSummary(val totalAmount: Double = 0.0)

@Projection(eventSequence = "custom-sequence")
class MbEventSeqFluentOrderProjection : IProjectionFor<MbEventSeqFluentOrderSummary> {
    override fun define(builder: IProjectionBuilderFor<MbEventSeqFluentOrderSummary>) {
        builder.from(MbEventSeqFluentOrderPlaced::class) {
            it.set(MbEventSeqFluentOrderSummary::totalAmount).to { e -> e.amount }
        }
    }
}
```
