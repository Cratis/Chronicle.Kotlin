```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;

class GetStartedMaterializedBooks {
    private final IEventStore eventStore;

    GetStartedMaterializedBooks(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    List<GetStartedBookDetails> page(int skip, int take) {
        return ReadModelsJavaBridge.getMaterializedInstances(
            eventStore.getReadModels(), GetStartedBookDetails.class, skip, take);
    }
}
```
