```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@EventType
record ReactorsFilteringInvoiceIssued(double amount) {}

// @Tag labels the reactor itself and shows up in tooling - it changes nothing about
// what the reactor observes. @FilterEventsByTag is what narrows the event stream.
@Reactor
@Tag({"finance", "owned-by-billing"})
@FilterEventsByTag("audited")
class ReactorsFilteringInvoiceAuditor {
    void issued(ReactorsFilteringInvoiceIssued event, EventContext context) {
    }
}
```
