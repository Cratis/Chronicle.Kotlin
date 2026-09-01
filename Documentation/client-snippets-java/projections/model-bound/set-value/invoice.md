```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-set-value-invoice-issued")
record MbSetValueInvoiceIssued(double amount) {}

@EventType(id = "mb-set-value-invoice-paid")
record MbSetValueInvoicePaid() {}

@ReadModel
@FromEvent(eventType = MbSetValueInvoiceIssued.class)
@FromEvent(eventType = MbSetValueInvoicePaid.class)
class MbSetValueInvoice {
    @SetFrom(propertyPath = "amount", eventType = MbSetValueInvoiceIssued.class)
    public double amount = 0.0;

    @SetValue(eventType = MbSetValueInvoiceIssued.class, value = "issued")
    @SetValue(eventType = MbSetValueInvoicePaid.class, value = "paid")
    public String status = "";
}
```
