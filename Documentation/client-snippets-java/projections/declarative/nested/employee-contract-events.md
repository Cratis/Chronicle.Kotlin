```java title="Employee contract events"
import io.cratis.chronicle.events.EventType;

import java.time.LocalDate;

@EventType
record EmployeeHiredForNestedContractEvents(String name, String department) {}

@EventType
record ContractStartedForNestedContractEvents(
    String contractId,
    LocalDate startDate,
    LocalDate endDate,
    String type
) {}

@EventType
record ContractExtendedForNestedContractEvents(LocalDate newEndDate) {}

@EventType
record ContractEndedForNestedContractEvents() {}
```
