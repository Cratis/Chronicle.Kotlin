```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-clearing-invoice-issued")
data class MbClearingInvoiceIssued(val reference: String)

@EventType(id = "mb-clearing-invoice-voided")
data class MbClearingInvoiceVoided(val placeholder: Boolean = true)

@ReadModel
@FromEvent(MbClearingInvoiceIssued::class)
data class MbClearingInvoice(
    // AutoMap sets this from the matching "reference" property on MbClearingInvoiceIssued; SetValue
    // with clear = true is the only way to null it back out - @ClearWith targets CLASS only, so it
    // cannot sit on a plain scalar property the way the .NET client's [ClearWith<T>] can.
    @SetValue(MbClearingInvoiceVoided::class, clear = true)
    val reference: String? = null
)
```
