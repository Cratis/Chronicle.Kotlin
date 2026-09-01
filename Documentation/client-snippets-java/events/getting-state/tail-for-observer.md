```java
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.EventLogJavaBridge;

class TailForObserverExample {
    /**
     * Computes the tail sequence number relevant to a specific observer, based on the event types
     * it handles.
     */
    static long getRelevantTail(IEventLog eventLog, Class<?> observerType) {
        return EventLogJavaBridge.getTailSequenceNumberForObserver(eventLog, observerType);
    }
}
```
