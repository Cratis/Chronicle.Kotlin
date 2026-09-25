```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.ExternalServicesServiceJavaBridge;

class ExternalServicesIndexDatabase {
    void registerPayrollDatabase(EventStore store) {
        ExternalServicesServiceJavaBridge.register(store.getExternalServices(), "payroll-database", builder -> {
            builder.postgreSql("payroll-db.internal", "payroll", "chronicle", java.util.Objects.requireNonNull(System.getenv("CHRONICLE_PAYROLL_DB_PASSWORD"), "CHRONICLE_PAYROLL_DB_PASSWORD is required"), 0);
        });
    }
}
```
