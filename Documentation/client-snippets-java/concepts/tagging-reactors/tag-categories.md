```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

// By integration type
@Tag({"Notifications", "ExternalAPI", "MessageQueue", "FileSystem"})
// By domain
@Tag({"Sales", "Inventory", "Customer", "Shipping"})
// By communication channel
@Tag({"Email", "SMS", "Push", "Webhook"})
// By purpose
@Tag({"Integration", "Alerting", "Monitoring", "Automation"})
// By stakeholder
@Tag({"Customer", "Operations", "Finance", "Support"})
@Reactor
class TaggingReactorsCategoryExamplesReactor {
    void on(TaggingReactorsCategoryOrderPlaced event) { }
}

@EventType
record TaggingReactorsCategoryOrderPlaced(String id) {}
```
