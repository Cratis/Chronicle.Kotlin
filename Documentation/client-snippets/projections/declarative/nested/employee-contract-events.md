```kotlin title="Employee contract events"
import io.cratis.chronicle.events.EventType
import java.time.LocalDate

@EventType(id = "employee-hired-for-nested-contract-events")
data class EmployeeHiredForNestedContractEvents(val name: String, val department: String)

@EventType(id = "contract-started-for-nested-contract-events")
data class ContractStartedForNestedContractEvents(
    val contractId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: String
)

@EventType(id = "contract-extended-for-nested-contract-events")
data class ContractExtendedForNestedContractEvents(val newEndDate: LocalDate)

@EventType(id = "contract-ended-for-nested-contract-events")
data class ContractEndedForNestedContractEvents(val placeholder: Boolean = true)
```
