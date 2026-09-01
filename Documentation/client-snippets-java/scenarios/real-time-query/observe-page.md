```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import kotlinx.coroutines.Job;

import java.util.List;
import java.util.function.Consumer;

record ScenariosObserveBook(String title, boolean onLoan) {
}

class ScenariosQueryLiveBookPage {
    private final IEventStore store;

    ScenariosQueryLiveBookPage(IEventStore store) {
        this.store = store;
    }

    /** Hands every new page to the subscriber, and returns the job to cancel when done. */
    Job subscribe(Consumer<List<ScenariosObserveBook>> onPage) {
        return ReadModelsJavaBridge.observeMaterializedInstances(
            store.getReadModels(),
            ScenariosObserveBook.class,
            0,
            50,
            onPage);
    }
}
```
