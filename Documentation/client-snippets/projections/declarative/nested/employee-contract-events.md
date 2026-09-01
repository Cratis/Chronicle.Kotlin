```kotlin title="Employee contract events"
import io.cratis.chronicle.events.EventType
import java.time.LocalDate

@EventType
data class EmployeeHiredForNestedContractEvents(val name: String, val department: String)

@EventType
data class ContractStartedForNestedContractEvents(
    val contractId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: String
)

@EventType
data class ContractExtendedForNestedContractEvents(val newEndDate: LocalDate)

@EventType
data class ContractEndedForNestedContractEvents(val placeholder: Boolean = true)
```
