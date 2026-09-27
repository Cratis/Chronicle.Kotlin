```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.FailedPartitionsJavaBridge;
import io.cratis.chronicle.observation.FailedPartition;

class ReactorPartitionRecovery {
    static void retryOneReactorPartition(IEventStore store, Class<?> reactor) {
        var failedPartitions = store.getFailedPartitions();
        var failures = FailedPartitionsJavaBridge.getFor(failedPartitions, reactor);
        if (failures.isEmpty()) return;

        FailedPartition failed = failures.get(0);
        FailedPartitionsJavaBridge.retry(failedPartitions, reactor, failed.getPartition());
        // The bridge returns void, so a refused retry of a quarantined partition is not signalled here.
        // Inspect partition state before reporting recovery.
    }
}
```
