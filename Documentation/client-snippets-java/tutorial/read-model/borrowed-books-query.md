```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

@ReadModel
record TutorialBorrowedBook(String title, String memberName) {}

// Java has no raw MongoCollection to query directly - reads go through the read model service
// instead.
class BorrowedBooks {
    private final IEventStore store;

    BorrowedBooks(IEventStore store) {
        this.store = store;
    }

    List<TutorialBorrowedBook> all() {
        return ReadModelsJavaBridge.getInstances(store.getReadModels(), TutorialBorrowedBook.class);
    }
}
```
