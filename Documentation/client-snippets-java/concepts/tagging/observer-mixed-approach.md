```java
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

// Java exposes only @Tag (repeatable) - combine a multi-value use with a stacked one however reads best
@Tag({"Notifications", "SMS"})
@Tag("Customer")
@Reactor
class TaggingSmsNotificationReactor {
}
```
