```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Nested
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class NodSliceCreated(val name: String)

@EventType
data class NodCommandSetForSlice(val name: String, val schema: String)

@EventType
class NodCommandClearedForSlice

@ReadModel
@FromEvent(NodSliceCreated::class)
data class NodSlice(
    val name: String = "",

    @Nested
    val command: NodCommandItem? = null
)

@FromEvent(NodCommandSetForSlice::class)
@ClearWith(NodCommandClearedForSlice::class)
data class NodCommandItem(
    val name: String = "",
    val schema: String = ""
)
```
