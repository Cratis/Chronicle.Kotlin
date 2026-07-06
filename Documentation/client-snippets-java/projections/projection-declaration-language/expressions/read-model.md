```java
import java.time.Instant;

class PdlExpressionsUserReadModel {
    public String name = "";               // Requires string
    public int loginCount = 0;              // Requires number
    public boolean isActive = false;        // Requires boolean
    public Instant createdAt = Instant.EPOCH; // Requires timestamp
}
```
