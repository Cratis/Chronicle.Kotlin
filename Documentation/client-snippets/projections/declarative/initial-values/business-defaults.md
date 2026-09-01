```kotlin title="Business defaults"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

enum class InitialValuesOrderStatus {
    Draft,
    Submitted
}

@EventType
data class InitialValuesOrderSubmitted(val customerName: String, val totalAmount: Double)

data class InitialValuesOrderSummary(
    val customerName: String = "",
    val status: InitialValuesOrderStatus = InitialValuesOrderStatus.Draft,
    val totalAmount: Double = 0.0,
    val notes: String = "No notes"
)

class InitialValuesOrderSummaryProjection : IProjectionFor<InitialValuesOrderSummary> {
    override fun define(builder: IProjectionBuilderFor<InitialValuesOrderSummary>) {
        builder.from(InitialValuesOrderSubmitted::class)
    }
}
```
