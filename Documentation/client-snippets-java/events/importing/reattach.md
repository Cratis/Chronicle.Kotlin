```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.imports.ImportId;
import io.cratis.chronicle.imports.ImportResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.java.JavaEventImport;

import java.util.List;

@EventType
record ImportingLegacyInvoiceIssued(String number) {}

record ImportingLegacyInvoice(String invoiceId, String number) {}

class EventsImportingReattach {
    ImportResult resume(EventStore store, ImportId importId, List<ImportingLegacyInvoice> source) {
        var eventLog = new BlockingEventSequence(store.getEventLog());
        JavaEventImport importSession = eventLog.beginImport(importId).join();
        try {
            // The kernel already holds the first eventsAcknowledged events; continue with the rest.
            var remaining = source.stream()
                .skip(importSession.getEventsAcknowledged())
                .map(invoice -> new EventForEventSourceId(invoice.invoiceId(), new ImportingLegacyInvoiceIssued(invoice.number())))
                .toList();
            importSession.add(remaining).join();
            return importSession.complete().join();
        } finally {
            importSession.close().join();
        }
    }
}
```
