```java
import java.util.Collections;
import java.util.List;

record DesigningReadModelsLineItem(String productName, int quantity, double price) {}

// A collection defaulted at the call site, not "fixed up" by logic in a constructor body -
// a document deserializer may build the instance without ever running one.
record DesigningReadModelsOrderSummary(String id, List<DesigningReadModelsLineItem> lines) {
    static DesigningReadModelsOrderSummary empty(String id) {
        return new DesigningReadModelsOrderSummary(id, Collections.emptyList());
    }
}
```
