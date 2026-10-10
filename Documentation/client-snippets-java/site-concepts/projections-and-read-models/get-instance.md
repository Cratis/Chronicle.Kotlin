```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;

public class BookStatusLookup {
    public void printStatus(IEventStore eventStore, String bookId) {
        var status = new BlockingEventStore(eventStore)
            .getReadModels()
            .getInstanceByKey(BookStatus.class, bookId);

        if (status != null) {
            System.out.println(status.title + ": " + (status.isBorrowed ? status.borrowedBy : "on the shelf"));
        }
    }
}
```
