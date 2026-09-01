```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.observation.EventSourceType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "reducers-filtering-invoice-issued")
record ReducersFilteringInvoiceIssued(double amount) {}

@ReadModel
record ReducersFilteringCustomerInvoiceTotal(double amount) {
    ReducersFilteringCustomerInvoiceTotal() {
        this(0.0);
    }
}

class ReducersFilteringInvoicingService {
    private final IEventLog eventLog;

    ReducersFilteringInvoicingService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void issueCustomerInvoice(String eventSourceId, double amount) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReducersFilteringInvoiceIssued(amount),
            new AppendOptionsBuilder().eventSourceType("customer").build());
    }
}

@Reducer
@EventSourceType("customer")
class ReducersFilteringCustomerInvoiceTotalReducer {
    ReducersFilteringCustomerInvoiceTotal issued(ReducersFilteringInvoiceIssued event, ReducersFilteringCustomerInvoiceTotal current, EventContext context) {
        double amount = current == null ? 0.0 : current.amount();
        return new ReducersFilteringCustomerInvoiceTotal(amount + event.amount());
    }
}
```
