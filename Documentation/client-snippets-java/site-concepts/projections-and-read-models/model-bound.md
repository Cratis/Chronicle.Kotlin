```java
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
@FromEvent(eventType = BookRegistered.class)
@FromEvent(eventType = BookBorrowed.class)
@FromEvent(eventType = BookReturned.class)
class BookStatus {
    public String id = "";

    @SetFrom(eventType = BookRegistered.class) public String title = "";
    @SetFrom(eventType = BookRegistered.class) public String isbn = "";

    @SetValue(eventType = BookBorrowed.class, value = "true")
    @SetValue(eventType = BookReturned.class, value = "false")
    public boolean isBorrowed = false;

    @SetFrom(propertyPath = "memberName", eventType = BookBorrowed.class)
    @SetValue(eventType = BookReturned.class, clear = true)
    public String borrowedBy;
}
```
