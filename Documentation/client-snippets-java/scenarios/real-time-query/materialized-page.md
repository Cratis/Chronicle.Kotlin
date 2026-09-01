```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;

record ScenariosMaterializedBook(String title, boolean onLoan) {}

class ScenariosQueryBookPage {
    private final EventStore store;

    ScenariosQueryBookPage(EventStore store) {
        this.store = store;
    }

    List<ScenariosMaterializedBook> getPage() {
        return ReadModelsJavaBridge.getMaterializedInstances(store.getReadModels(), ScenariosMaterializedBook.class, 0, 20);
    }
}
```
