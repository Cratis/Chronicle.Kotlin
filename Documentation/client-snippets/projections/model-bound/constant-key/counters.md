```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.readModels.ReadModel

@EventType
class MbConstantKeyOrderPlacedForMetrics

@EventType
class MbConstantKeyUserLoggedIn

@EventType
class MbConstantKeyUserLoggedOut

@EventType
class MbConstantKeyErrorOccurred

@ReadModel
@FromEvent(MbConstantKeyOrderPlacedForMetrics::class)
@FromEvent(MbConstantKeyUserLoggedIn::class)
@FromEvent(MbConstantKeyUserLoggedOut::class)
@FromEvent(MbConstantKeyErrorOccurred::class)
data class MbConstantKeySystemMetrics(
    @Count(MbConstantKeyOrderPlacedForMetrics::class, constantKey = "metrics")
    val totalOrders: Int = 0,

    @Increment(MbConstantKeyUserLoggedIn::class, constantKey = "metrics")
    @Decrement(MbConstantKeyUserLoggedOut::class, constantKey = "metrics")
    val activeSessions: Int = 0,

    @Count(MbConstantKeyErrorOccurred::class, constantKey = "metrics")
    val totalErrors: Int = 0
)
```
