```java
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
@FromEvent(eventType = BookBorrowed.class)
@RemovedWith(eventType = BookReturned.class)
class BorrowedBook {
    private String id = "";
    private String memberName = "";

    public BorrowedBook() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
}
```
