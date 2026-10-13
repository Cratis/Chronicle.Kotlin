```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetFromContext;
import io.cratis.chronicle.projections.SetValue;

@FromEvent(eventType = ChoosingStyleBookRegistered.class)
@FromEvent(eventType = ChoosingStyleBookBorrowed.class)
@FromEvent(eventType = ChoosingStyleBookReturned.class)
class ChoosingStyleBookStatusModelBound {
    @SetFromContext(eventType = ChoosingStyleBookRegistered.class, contextProperty = "eventSourceId")
    @SetFromContext(eventType = ChoosingStyleBookBorrowed.class, contextProperty = "eventSourceId")
    @SetFromContext(eventType = ChoosingStyleBookReturned.class, contextProperty = "eventSourceId")
    public String id = "";

    @SetFrom(propertyPath = "title", eventType = ChoosingStyleBookRegistered.class)
    public String title = null;

    @SetFrom(propertyPath = "isbn", eventType = ChoosingStyleBookRegistered.class)
    public String isbn = null;

    @SetValue(eventType = ChoosingStyleBookRegistered.class, value = "false")
    @SetValue(eventType = ChoosingStyleBookBorrowed.class, value = "true")
    @SetValue(eventType = ChoosingStyleBookReturned.class, value = "false")
    public boolean isBorrowed = false;

    @Pii
    @SetFrom(propertyPath = "memberName", eventType = ChoosingStyleBookBorrowed.class)
    @SetValue(eventType = ChoosingStyleBookRegistered.class, clear = true)
    @SetValue(eventType = ChoosingStyleBookReturned.class, clear = true)
    public String borrowedBy = null;
}
```
