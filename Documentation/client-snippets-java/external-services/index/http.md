```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.ExternalServicesServiceJavaBridge;

class ExternalServicesIndexHttp {
    void registerPayrollHttpService(EventStore store) {
        ExternalServicesServiceJavaBridge.register(store.getExternalServices(), "payroll-provider", builder -> {
            builder
                .http("https://payroll.example.com/api")
                .withBearerToken(java.util.Objects.requireNonNull(System.getenv("CHRONICLE_PAYROLL_TOKEN"), "CHRONICLE_PAYROLL_TOKEN is required"));
        });
    }
}
```
