```kotlin title="Initialize collections"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

data class InitialValuesAddress(val street: String = "", val city: String = "")

@EventType(id = "initial-values-customer-registered")
data class InitialValuesCustomerRegistered(val name: String)

data class InitialValuesCustomerRecord(
    val name: String = "",
    val addresses: List<InitialValuesAddress> = emptyList(),
    val tags: List<String> = emptyList()
)

class InitialValuesCustomerRecordProjection : IProjectionFor<InitialValuesCustomerRecord> {
    override fun define(builder: IProjectionBuilderFor<InitialValuesCustomerRecord>) {
        builder.from(InitialValuesCustomerRegistered::class)
    }
}
```
