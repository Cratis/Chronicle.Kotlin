```java
import io.cratis.chronicle.IEventStore;

import io.cratis.chronicle.java.IdentityManagerServiceJavaBridge;

class CorrelationIdentityCausationRenamingAnIdentity {
    void rename(IEventStore eventStore) {
        IdentityManagerServiceJavaBridge.rename(eventStore.getIdentities(), "subject-42", "Jane Austen");
    }
}
```
