```java
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetFromContext;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
@FromEvent(eventType = GetStartedBookAdded.class)
record GetStartedBookDetails(
    @SetFromContext(eventType = GetStartedBookAdded.class, contextProperty = "eventSourceId")
    String id,
    String title,
    String isbn,
    @SetValue(eventType = GetStartedBookAdded.class, value = "false")
    @SetValue(eventType = GetStartedBookBorrowed.class, value = "true")
    @SetValue(eventType = GetStartedBookReturned.class, value = "false")
    boolean onLoan,
    @SetFrom(propertyPath = "memberName", eventType = GetStartedBookBorrowed.class)
    @SetValue(eventType = GetStartedBookReturned.class, clear = true)
    String borrowedBy
) {}
```
