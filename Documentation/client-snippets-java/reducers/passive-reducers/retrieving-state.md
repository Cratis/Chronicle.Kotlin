```java
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

class PassiveReducersReportingService {
    private final EventStore eventStore;

    PassiveReducersReportingService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    // This triggers the passive reducer to compute state from events
    PassiveReducersMonthlyRevenueReport generateReport(String reportId) {
        return ReadModelsJavaBridge.getInstanceByKey(
            eventStore.getReadModels(), PassiveReducersMonthlyRevenueReport.class, reportId);
    }
}
```
