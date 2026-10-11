```java
import io.cratis.chronicle.eventSequences.AppendedEvent;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.EventLogJavaBridge;

import java.util.List;

public class BookHistoryReader {
    private final IEventLog eventLog;

    public BookHistoryReader(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    public List<AppendedEvent> getBorrowings(String bookId) {
        return EventLogJavaBridge.getForEventSourceIdAndEventTypes(
            eventLog, bookId, List.of(BookBorrowed.class));
    }
}
```
