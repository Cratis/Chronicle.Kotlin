```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.ExternalServicesServiceJavaBridge;

class ExternalServicesIndexHttp {
    void registerPayrollHttpService(EventStore store) {
        ExternalServicesServiceJavaBridge.register(store.getExternalServices(), "payroll-provider", builder -> {
            builder
                .http("https://payroll.example.com/api")
                .withBearerToken("payroll-integration-token");
            return null; // Java lambda returning Unit
        });
    }
}
```
