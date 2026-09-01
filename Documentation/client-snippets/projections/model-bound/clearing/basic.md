```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbClearingProjectNoted(val note: String)

@EventType
data class MbClearingProjectNoteCleared(val placeholder: Boolean = true)

@ReadModel
@FromEvent(MbClearingProjectNoted::class)
data class MbClearingProjectNotes(
    @SetFrom("note", MbClearingProjectNoted::class)
    @SetValue(MbClearingProjectNoteCleared::class, clear = true)
    val note: String? = null
)
```
