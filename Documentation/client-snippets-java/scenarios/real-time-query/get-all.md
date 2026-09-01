```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;

record ScenariosQueryAllBook(String title, boolean onLoan) {}

class ScenariosQueryOnLoanBooks {
    private final EventStore store;

    ScenariosQueryOnLoanBooks(EventStore store) {
        this.store = store;
    }

    List<ScenariosQueryAllBook> getOnLoan() {
        return ReadModelsJavaBridge.getInstances(store.getReadModels(), ScenariosQueryAllBook.class)
            .stream()
            .filter(ScenariosQueryAllBook::onLoan)
            .toList();
    }
}
```
