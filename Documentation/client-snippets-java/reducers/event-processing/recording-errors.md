```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.ArrayList;
import java.util.List;

@EventType(id = "event-processing-invalid-data-detected")
record EventProcessingInvalidDataDetected(String reason) {}

@ReadModel
record EventProcessingValidationResult(boolean isValid, List<String> errors) {
    EventProcessingValidationResult() {
        this(true, List.of());
    }
}

@Reducer
class EventProcessingValidationResultReducer {
    EventProcessingValidationResult detected(EventProcessingInvalidDataDetected event, EventProcessingValidationResult current) {
        List<String> errors = new ArrayList<>(current == null ? List.of() : current.errors());
        errors.add(event.reason());

        return new EventProcessingValidationResult(false, errors);
    }
}
```
