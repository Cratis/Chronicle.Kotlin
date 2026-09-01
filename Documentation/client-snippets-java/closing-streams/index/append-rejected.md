```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.ConstraintViolation;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record ClosingStreamsInvoiceLineAdded(String description, double amount) {}

class ClosingStreamsIndexAppendRejected {
    // Appends a line item to an invoice stream. Once the stream has been closed, the append is
    // rejected with a "StreamClosed" constraint violation and no further lines can be added.
    boolean tryAppendLine(EventStore store, String invoiceId) {
        AppendOptions options = new AppendOptionsBuilder()
            .eventStreamType("invoices")
            .eventStreamId("invoice-42")
            .build();

        AppendResult result = EventLogJavaBridge.append(
            store.getEventLog(), invoiceId, new ClosingStreamsInvoiceLineAdded("Consulting", 500.0), options);

        if (!result.isSuccess()) {
            for (ConstraintViolation violation : result.getConstraintViolations()) {
                if (violation.getConstraintId().equals("StreamClosed")) {
                    return false;
                }
            }
        }

        return true;
    }
}
```
