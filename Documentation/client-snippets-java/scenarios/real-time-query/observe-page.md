```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;
import java.util.concurrent.Flow;

record ScenariosObserveBook(String title, boolean onLoan) {}

class ScenariosQueryLiveBookPage {
    private final IEventStore store;

    ScenariosQueryLiveBookPage(IEventStore store) {
        this.store = store;
    }

    // Frameworks such as Arc accept Flow.Publisher<List<T>> as an observable query.
    Flow.Publisher<List<ScenariosObserveBook>> observe() {
        return ReadModelsJavaBridge.observeMaterializedInstancesPublisher(
            store.getReadModels(), ScenariosObserveBook.class, 0, 50);
    }
}
```
