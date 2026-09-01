```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

record ScenariosWatchBook(String title, boolean onLoan) {}

class ScenariosQueryBookWatcher {
    private final EventStore store;

    ScenariosQueryBookWatcher(EventStore store) {
        this.store = store;
    }

    void watch() {
        ReadModelsJavaBridge.watch(store.getReadModels(), ScenariosWatchBook.class, changeset -> {
            if (changeset.getRemoved() || changeset.getReadModel() == null) return;
            System.out.println(changeset.getModelKey() + ": on loan = " + changeset.getReadModel().onLoan());
        });
    }
}
```
