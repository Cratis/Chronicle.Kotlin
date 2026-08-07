```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;

record ScenariosQueryBook(String title, boolean onLoan) {}

class ScenariosQueryBookService {
    private final BlockingEventStore store;

    ScenariosQueryBookService(IEventStore store) {
        this.store = new BlockingEventStore(store);
    }

    ScenariosQueryBook getBook(String bookId) {
        return store.getReadModels().getInstanceByKey(ScenariosQueryBook.class, bookId);
    }
}
```
