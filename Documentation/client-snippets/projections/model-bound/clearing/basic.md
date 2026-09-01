```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-clearing-project-noted")
data class MbClearingProjectNoted(val note: String)

@EventType(id = "mb-clearing-project-note-cleared")
data class MbClearingProjectNoteCleared(val placeholder: Boolean = true)

@ReadModel
@FromEvent(MbClearingProjectNoted::class)
data class MbClearingProjectNotes(
    @SetFrom("note", MbClearingProjectNoted::class)
    @SetValue(MbClearingProjectNoteCleared::class, clear = true)
    val note: String? = null
)
```
