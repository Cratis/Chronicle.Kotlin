```java
import io.cratis.arc.artifacts.FromServices;
import io.cratis.arc.artifacts.ReadModel;
import io.cratis.arc.authorization.AllowAnonymous;
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;
import java.util.concurrent.Flow;

@ReadModel
@io.cratis.chronicle.readModels.ReadModel
@AllowAnonymous
public record ScenariosObserveBook(String title, boolean onLoan) {
    public static Flow.Publisher<List<ScenariosObserveBook>> observe(@FromServices IEventStore store) {
        return ReadModelsJavaBridge.observeMaterializedInstancesPublisher(
            store.getReadModels(), ScenariosObserveBook.class, 0, 50);
    }
}
```
