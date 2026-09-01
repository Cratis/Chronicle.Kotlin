```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Collections;
import java.util.List;

@EventType
record MbClearingTaskListStarted(String name) {}

@EventType
record MbClearingTaskAdded(String listId, String taskId, String title, String due) {}

@EventType
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
