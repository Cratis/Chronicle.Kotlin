```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.ConstraintViolation;
import io.cratis.chronicle.eventSequences.AppendError;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation;
import io.cratis.chronicle.transactions.UnitOfWork;

import java.util.stream.Collectors;

import io.cratis.chronicle.java.TransactionalEventSequenceJavaBridge;
import io.cratis.chronicle.java.UnitOfWorkJavaBridge;

@EventType
record InspectingResultOrderPlaced(String orderId, double totalAmount) {}

class EventsTransactionsInspectingResult {
    // Commits a unit of work and inspects the outcome — violations, errors, and whether it
    // succeeded overall are all available after commit() returns.
    void commitAndInspect(EventStore store, String orderId) {
        UnitOfWork unitOfWork = store.getUnitOfWorkManager().begin();
        TransactionalEventSequenceJavaBridge.append(
            store.getEventLog().getTransactional(), orderId, new InspectingResultOrderPlaced(orderId, 42.0), null);
        UnitOfWorkJavaBridge.commit(unitOfWork);

        if (unitOfWork.isSuccess()) {
            System.out.println("Committed successfully");
        } else {
            String violations = unitOfWork.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            String concurrencyViolations = unitOfWork.getConcurrencyViolations().stream()
                .map(ConcurrencyViolation::getEventSourceId).collect(Collectors.joining(", "));
            String errors = unitOfWork.getAppendErrors().stream()
                .map(AppendError::getMessage).collect(Collectors.joining(", "));
            System.out.println("Failed: violations=[" + violations + "] concurrency=[" + concurrencyViolations +
                "] errors=[" + errors + "]");
        }
    }
}
```
