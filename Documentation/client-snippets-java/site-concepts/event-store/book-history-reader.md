```java
import io.cratis.chronicle.eventSequences.AppendedEvent;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.EventLogJavaBridge;

import java.util.List;

class BookHistoryReader {
    private final IEventLog eventLog;

    BookHistoryReader(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    List<AppendedEvent> getBorrowings(String bookId) {
        return EventLogJavaBridge.getForEventSourceIdAndEventTypes(
            eventLog, bookId, List.of(BookBorrowed.class));
    }
}
```
