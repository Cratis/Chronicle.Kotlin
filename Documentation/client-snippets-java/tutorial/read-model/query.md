```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;
import java.util.stream.Collectors;

@ReadModel
record TutorialBook(String title, boolean onLoan) {}

// Java has no raw MongoCollection to query directly - reads go through the read model service
// instead, which returns already-deserialized instances to filter in memory.
class Books {
    private final IEventStore store;

    Books(IEventStore store) {
        this.store = store;
    }

    List<TutorialBook> onLoan() {
        return ReadModelsJavaBridge.getInstances(store.getReadModels(), TutorialBook.class).stream()
            .filter(TutorialBook::onLoan)
            .collect(Collectors.toList());
    }
}
```
