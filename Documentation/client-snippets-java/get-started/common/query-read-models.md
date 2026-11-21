```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;

class GetStartedReadModels {
    private final IEventStore eventStore;

    GetStartedReadModels(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    List<GetStartedBookDetails> books() {
        return ReadModelsJavaBridge.getInstances(eventStore.getReadModels(), GetStartedBookDetails.class);
    }

    List<GetStartedBorrowedBook> borrowedBooks() {
        return ReadModelsJavaBridge.getInstances(eventStore.getReadModels(), GetStartedBorrowedBook.class);
    }
}
```
