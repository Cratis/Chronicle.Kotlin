```java
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;

@ReadModel
class CamelCasingUserReadModel {
    public String firstName = "";
    public String lastName = "";
    public String emailAddress = "";
    public Instant registrationDate = Instant.EPOCH;
}
```
