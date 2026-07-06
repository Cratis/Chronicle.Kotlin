```java
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
@FromEvent(eventType = IndexAutoMapAccountOpened.class)
class IndexAutoMapMbAccountInfo {
    public String name = "";    // Automatically mapped from IndexAutoMapAccountOpened.name
    public double balance = 0;  // Automatically mapped from IndexAutoMapAccountOpened.balance
}
```
