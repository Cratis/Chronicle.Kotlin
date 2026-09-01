```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;
import java.util.stream.Collectors;

@ReadModel
record GetStartedBook(String title, boolean onLoan) {}

// Java has no raw MongoCollection to query directly - reads go through the read model service
// instead, which returns already-deserialized instances to filter in memory.
class GetStartedBooks {
    private final IEventStore store;

    GetStartedBooks(IEventStore store) {
        this.store = store;
    }

    List<GetStartedBook> onLoan() {
        return ReadModelsJavaBridge.getInstances(store.getReadModels(), GetStartedBook.class).stream()
            .filter(GetStartedBook::onLoan)
            .collect(Collectors.toList());
    }
}
```
