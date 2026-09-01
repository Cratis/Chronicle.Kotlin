```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.UUID;

@EventType(id = "event-processing-account-opened")
record EventProcessingAccountOpened(UUID accountId) {}

@EventType(id = "event-processing-deposit-made")
record EventProcessingDepositMade(double amount) {}

@EventType(id = "event-processing-account-closed")
record EventProcessingAccountClosed() {}

@ReadModel
record EventProcessingAccount(UUID accountId, double balance, boolean isActive) {
    EventProcessingAccount() {
        this(new UUID(0, 0), 0.0, false);
    }
}

@Reducer
class EventProcessingAccountReducer {
    EventProcessingAccount opened(EventProcessingAccountOpened event, EventProcessingAccount current, EventContext context) {
        return new EventProcessingAccount(event.accountId(), 0.0, true);
    }

    EventProcessingAccount depositMade(EventProcessingDepositMade event, EventProcessingAccount current, EventContext context) {
        // Skip if account doesn't exist or is not active
        if (current == null || !current.isActive()) return current;

        return new EventProcessingAccount(current.accountId(), current.balance() + event.amount(), current.isActive());
    }

    EventProcessingAccount closed(EventProcessingAccountClosed event, EventProcessingAccount current, EventContext context) {
        if (current == null) return null;

        return new EventProcessingAccount(current.accountId(), current.balance(), false);
    }
}
```
