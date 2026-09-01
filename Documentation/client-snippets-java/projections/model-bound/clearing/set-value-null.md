```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-clearing-invoice-issued")
record MbClearingInvoiceIssued(String reference) {}

@EventType(id = "mb-clearing-invoice-voided")
record MbClearingInvoiceVoided() {}

@ReadModel
@FromEvent(eventType = MbClearingInvoiceIssued.class)
class MbClearingInvoice {
    // AutoMap sets this from the matching "reference" field on MbClearingInvoiceIssued; SetValue with
    // clear = true is the only way to null it back out - @ClearWith targets CLASS only, so it cannot
    // sit on a plain scalar field the way the .NET client's [ClearWith<T>] can.
    @SetValue(eventType = MbClearingInvoiceVoided.class, clear = true)
    public String reference;
}
```
