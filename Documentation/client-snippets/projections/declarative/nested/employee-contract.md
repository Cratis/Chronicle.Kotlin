```kotlin title="Employee contract projection"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.time.LocalDate

@EventType
data class EmployeeHiredWithNestedContract(val name: String, val department: String)

@EventType
data class ContractStartedWithNestedContract(
    val contractId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: String
)

@EventType
data class ContractExtendedWithNestedContract(val newEndDate: LocalDate)

@EventType
data class ContractEndedWithNestedContract(val placeholder: Boolean = true)

data class EmployeeWithNestedContract(
    val name: String = "",
    val department: String = "",
    val activeContract: ContractForNestedEmployee? = null
)

data class ContractForNestedEmployee(
    val contractId: String = "",
    val startDate: LocalDate = LocalDate.MIN,
    val endDate: LocalDate = LocalDate.MIN,
    val type: String = ""
)

class EmployeeProjectionWithNestedContract : IProjectionFor<EmployeeWithNestedContract> {
    override fun define(builder: IProjectionBuilderFor<EmployeeWithNestedContract>) {
        builder
            .from(EmployeeHiredWithNestedContract::class)
            .nested(EmployeeWithNestedContract::activeContract, ContractForNestedEmployee::class) { contract ->
                contract
                    .from(ContractStartedWithNestedContract::class)
                    .from(ContractExtendedWithNestedContract::class) {
                        it.set(ContractForNestedEmployee::endDate).to { e -> e.newEndDate }
                    }
                    .clearWith(ContractEndedWithNestedContract::class)
            }
    }
}
```
