```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.observation.EventSourceType;
import io.cratis.chronicle.observation.Reducer;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record FilterBySourceTypeInvoiceIssued(double amount) {}

record FilterBySourceTypeCustomerInvoiceTotal(double amount) {}

@EventSourceType("customer")
@Reducer
class FilterBySourceTypeCustomerInvoiceTotalReducer {
    public FilterBySourceTypeCustomerInvoiceTotal invoiceIssued(FilterBySourceTypeInvoiceIssued event, FilterBySourceTypeCustomerInvoiceTotal current) {
        double currentAmount = current != null ? current.amount() : 0.0;
        return new FilterBySourceTypeCustomerInvoiceTotal(currentAmount + event.amount());
    }
}

class EventsFilteringByEventSourceTypeReducer {
    AppendResult issueCustomerInvoice(EventStore store, String eventSourceId, double amount) {
        AppendOptions options = new AppendOptionsBuilder().eventSourceType("customer").build();
        return EventLogJavaBridge.append(store.getEventLog(), eventSourceId, new FilterBySourceTypeInvoiceIssued(amount), options);
    }
}
```
