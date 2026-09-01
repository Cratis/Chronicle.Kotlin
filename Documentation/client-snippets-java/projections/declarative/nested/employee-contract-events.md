```java title="Employee contract events"
import io.cratis.chronicle.events.EventType;

import java.time.LocalDate;

@EventType(id = "employee-hired-for-nested-contract-events")
record EmployeeHiredForNestedContractEvents(String name, String department) {}

@EventType(id = "contract-started-for-nested-contract-events")
record ContractStartedForNestedContractEvents(
    String contractId,
    LocalDate startDate,
    LocalDate endDate,
    String type
) {}

@EventType(id = "contract-extended-for-nested-contract-events")
record ContractExtendedForNestedContractEvents(LocalDate newEndDate) {}

@EventType(id = "contract-ended-for-nested-contract-events")
record ContractEndedForNestedContractEvents() {}
```
