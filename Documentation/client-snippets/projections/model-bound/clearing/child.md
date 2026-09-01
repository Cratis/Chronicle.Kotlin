```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbClearingTaskListStarted(val name: String)

@EventType
data class MbClearingTaskAdded(val listId: String, val taskId: String, val title: String, val due: String)

@EventType
data class MbClearingTaskDeferred(val listId: String, val taskId: String)

data class MbClearingTask(
    val taskId: String = "",

    @SetFrom("title", MbClearingTaskAdded::class)
    val title: String = "",

    @SetFrom("due", MbClearingTaskAdded::class)
    @SetValue(MbClearingTaskDeferred::class, clear = true)
    val due: String? = null
)

@ReadModel
@FromEvent(MbClearingTaskListStarted::class)
data class MbClearingTaskList(
    @ChildrenFrom(MbClearingTaskAdded::class, key = "taskId", identifiedBy = "taskId", parentKey = "listId")
    @ChildrenFrom(MbClearingTaskDeferred::class, key = "taskId", identifiedBy = "taskId", parentKey = "listId")
    val tasks: List<MbClearingTask> = emptyList()
)
```
