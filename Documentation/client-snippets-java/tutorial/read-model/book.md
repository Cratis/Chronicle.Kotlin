```java
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetFromContext;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
@FromEvent(eventType = BookAdded.class)
record TutorialBookDetails(
    @SetFromContext(eventType = BookAdded.class, contextProperty = "eventSourceId")
    String id,
    String title,
    String isbn,
    @SetValue(eventType = BookAdded.class, value = "false")
    @SetValue(eventType = BookBorrowed.class, value = "true")
    @SetValue(eventType = BookReturned.class, value = "false")
    boolean onLoan,
    @SetFrom(propertyPath = "memberName", eventType = BookBorrowed.class)
    @SetValue(eventType = BookReturned.class, clear = true)
    String borrowedBy
) {}
```
