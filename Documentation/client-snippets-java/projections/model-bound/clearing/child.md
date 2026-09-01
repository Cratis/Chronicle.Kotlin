```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Collections;
import java.util.List;

@EventType(id = "mb-clearing-task-list-started")
record MbClearingTaskListStarted(String name) {}

@EventType(id = "mb-clearing-task-added")
record MbClearingTaskAdded(String listId, String taskId, String title, String due) {}

@EventType(id = "mb-clearing-task-deferred")
record MbClearingTaskDeferred(String listId, String taskId) {}

class MbClearingTask {
    public String taskId = "";

    @SetFrom(propertyPath = "title", eventType = MbClearingTaskAdded.class)
    public String title = "";

    @SetFrom(propertyPath = "due", eventType = MbClearingTaskAdded.class)
    @SetValue(eventType = MbClearingTaskDeferred.class, clear = true)
    public String due;
}

@ReadModel
@FromEvent(eventType = MbClearingTaskListStarted.class)
class MbClearingTaskList {
    @ChildrenFrom(eventType = MbClearingTaskAdded.class, key = "taskId", identifiedBy = "taskId", parentKey = "listId")
    @ChildrenFrom(eventType = MbClearingTaskDeferred.class, key = "taskId", identifiedBy = "taskId", parentKey = "listId")
    public List<MbClearingTask> tasks = Collections.emptyList();
}
```
