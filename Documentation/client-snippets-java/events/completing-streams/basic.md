```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsCompletingStreamsBasic {
    // Completes a stream so no further events can be appended to it. Returns false when the
    // stream was already completed, or when it is the default stream (which can never be
    // completed).
    boolean completeOrderStream(EventStore store, String orderId) {
        return EventLogJavaBridge.completeStream(store.getEventLog(), "Order", orderId);
    }
}
```
