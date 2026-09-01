```java
import io.cratis.chronicle.events.EventType;

import java.time.Instant;

@EventType
record CamelCasingUserRegistered(
    String firstName,
    String lastName,
    String emailAddress,
    Instant registrationDate) {
}
```
