```java title="Employee contract projection"
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.time.LocalDate;

record EmployeeWithNestedContract(String name, String department, ContractForNestedEmployee activeContract) {}

record ContractForNestedEmployee(String contractId, LocalDate startDate, LocalDate endDate, String type) {}

class EmployeeProjectionWithNestedContract implements IProjectionFor<EmployeeWithNestedContract> {
    @Override
    public void define(IProjectionBuilderFor<EmployeeWithNestedContract> builder) {
        builder
            .from(EmployeeHiredForNestedContractEvents.class)
            .nested("activeContract", ContractForNestedEmployee.class, contract -> {
                contract
                    .from(ContractStartedForNestedContractEvents.class)
                    .from(ContractExtendedForNestedContractEvents.class, fb -> {
                        fb.<LocalDate>set("endDate").to(e -> e.newEndDate());
                        return null; // Java lambda returning Unit
                    })
                    .clearWith(ContractEndedForNestedContractEvents.class);
                return null; // Java lambda returning Unit
            });
    }
}
```
