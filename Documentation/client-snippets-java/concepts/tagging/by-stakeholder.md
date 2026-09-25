```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@Tag("Customer")
@Tag("Operations")
@Tag("Finance")
@Tag("Support")
@Tag("Executive")
@Reactor
class TaggingByStakeholderExample {
    void on(TaggingByStakeholderReportRequested event) { }
}

@EventType
record TaggingByStakeholderReportRequested(String id) {}
```
