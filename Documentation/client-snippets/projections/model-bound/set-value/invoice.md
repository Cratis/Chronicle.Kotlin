```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-set-value-invoice-issued")
data class MbSetValueInvoiceIssued(val amount: Double)

@EventType(id = "mb-set-value-invoice-paid")
data class MbSetValueInvoicePaid(val placeholder: Boolean = true)

@ReadModel
@FromEvent(MbSetValueInvoiceIssued::class)
@FromEvent(MbSetValueInvoicePaid::class)
data class MbSetValueInvoice(
    @SetFrom("amount", MbSetValueInvoiceIssued::class)
    val amount: Double = 0.0,

    @SetValue(MbSetValueInvoiceIssued::class, value = "issued")
    @SetValue(MbSetValueInvoicePaid::class, value = "paid")
    val status: String = ""
)
```
