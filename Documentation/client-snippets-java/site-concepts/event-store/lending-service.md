```java
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.EventLogJavaBridge;

import java.time.LocalDate;

@EventType
public record BookBorrowed(String member, LocalDate dueDate) {}

public class LendingService {
    private final IEventLog eventLog;

    public LendingService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    public AppendResult lend(String bookId, String member, LocalDate dueDate) {
        return EventLogJavaBridge.append(eventLog, bookId, new BookBorrowed(member, dueDate), null);
    }
}
```
