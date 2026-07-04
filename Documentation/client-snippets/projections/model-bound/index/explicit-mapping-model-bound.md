```kotlin
import io.cratis.chronicle.projections.SetFrom

data class IndexExplicitMbAccountInfo(
    @SetFrom("name", IndexExplicitAccountOpened::class)
    val name: String = "",

    @SetFrom("initialBalance", IndexExplicitAccountOpened::class)
    val balance: Double = 0.0
)
```
