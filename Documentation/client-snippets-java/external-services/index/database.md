```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.ExternalServicesServiceJavaBridge;

class ExternalServicesIndexDatabase {
    void registerPayrollDatabase(EventStore store) {
        ExternalServicesServiceJavaBridge.register(store.getExternalServices(), "payroll-database", builder -> {
            builder.postgreSql("payroll-db.internal", "payroll", "chronicle", "secret", 0);
            return null; // Java lambda returning Unit
        });
    }
}
```
