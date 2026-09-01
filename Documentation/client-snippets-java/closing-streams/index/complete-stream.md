```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class ClosingStreamsIndexCompleteStream {
    // Closes the invoice stream so no further line items can be appended to it. Returns false
    // when the stream was already completed, or when it is the default stream (which can never
    // be completed).
    boolean closeInvoiceStream(EventStore store, String invoiceStreamId) {
        return EventLogJavaBridge.completeStream(store.getEventLog(), "invoices", invoiceStreamId);
    }
}
```
