```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.IdentityManagerServiceJavaBridge;

class IdentitiesRenameBasic {
    // Renames the human-readable name the kernel has stored for an identity's subject.
    void renameIdentity(EventStore store, String subject, String newName) {
        IdentityManagerServiceJavaBridge.rename(store.getIdentities(), subject, newName);
    }
}
```
