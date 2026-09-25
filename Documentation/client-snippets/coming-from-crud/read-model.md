```kotlin
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEvery
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(CrudComparisonCustomerRegistered::class)
@FromEvent(CrudComparisonAddressChanged::class)
data class CrudComparisonCustomerCard(
    @FromEvery(contextProperty = "eventSourceId")
    val id: String = "",
    val name: String = "",
    val address: String = "",
    @Count(CrudComparisonAddressChanged::class)
    val timesRelocated: Int = 0
)
```
