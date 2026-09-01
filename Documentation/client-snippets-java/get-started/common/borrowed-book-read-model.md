```java
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
@FromEvent(eventType = GetStartedBookBorrowed.class)
@RemovedWith(eventType = GetStartedBookReturned.class)
class GetStartedBorrowedBook {
    private String id = "";
    private String memberName = "";

    public GetStartedBorrowedBook() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
}
```
