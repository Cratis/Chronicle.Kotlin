```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.math.BigDecimal

@EventType
data class DecFunctionsTransaction(val amount: BigDecimal)

data class DecFunctionsTransactionSummary(
    val transactionCount: Int = 0,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val processedEvents: Int = 0
)

class DecFunctionsTransactionSummaryProjection : IProjectionFor<DecFunctionsTransactionSummary> {
    override fun define(builder: IProjectionBuilderFor<DecFunctionsTransactionSummary>) {
        builder
            .from(DecFunctionsTransaction::class) {
                it.count(DecFunctionsTransactionSummary::transactionCount)
                it.add(DecFunctionsTransactionSummary::totalAmount).with("amount")
                it.increment(DecFunctionsTransactionSummary::processedEvents)
            }
    }
}
```
