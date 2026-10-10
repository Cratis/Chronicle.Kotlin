```java
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.EventLogJavaBridge;

import java.time.LocalDate;

@EventType
record BookBorrowed(String member, LocalDate dueDate) {}

class LendingService {
    private final IEventLog eventLog;

    LendingService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    AppendResult lend(String bookId, String member, LocalDate dueDate) {
        return EventLogJavaBridge.append(eventLog, bookId, new BookBorrowed(member, dueDate), null);
    }
}
```
