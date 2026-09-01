```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "dec-not-rewindable-payment-processed")
data class DecNotRewindablePaymentProcessed(val paymentId: String, val amount: Double)

data class DecNotRewindableLedgerEntry(
    val recordedAt: String = ""
)

class DecNotRewindableTransactionLedgerProjection : IProjectionFor<DecNotRewindableLedgerEntry> {
    override fun define(builder: IProjectionBuilderFor<DecNotRewindableLedgerEntry>) {
        builder
            .notRewindable()
            .fromEvery { it.set(DecNotRewindableLedgerEntry::recordedAt).toEventContextProperty("occurred") }
            .from(DecNotRewindablePaymentProcessed::class)
    }
}
```
