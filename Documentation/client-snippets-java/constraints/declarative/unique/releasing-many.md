```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class ReleasingManyCustomerProfile {
    private String id = "";
    @Pii(description = "Customer email address")
    private String email = "";

    public ReleasingManyCustomerProfile() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

class ConstraintsUniqueReleasingMany {
    // Decrypts PII-annotated properties for a batch of read model instances in one call — the
    // compliance subject for each instance is derived from its own "id" property.
    List<ReleasingManyCustomerProfile> releaseCustomerEmails(EventStore store, List<ReleasingManyCustomerProfile> profiles) {
        return ReadModelsJavaBridge.releaseMany(store.getReadModels(), profiles);
    }
}
```
