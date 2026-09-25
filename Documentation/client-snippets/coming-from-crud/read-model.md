```kotlin
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEvery
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(CrudComparisonCustomerRegistered::class)
@FromEvent(CrudComparisonAddressChanged::class)
data class CrudComparisonCustomerCard(
    @FromEvery(contextProperty = "eventSourceId")
    val id: String = "",
    val name: String = "",
    // AutoMap covers the registration; a relocation also needs its address mapped explicitly,
    // because an event with only a count mapping is not auto-mapped.
    @SetFrom("address", CrudComparisonAddressChanged::class)
    val address: String = "",
    @Count(CrudComparisonAddressChanged::class)
    val timesRelocated: Int = 0
)
```
