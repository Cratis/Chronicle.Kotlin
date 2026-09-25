```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record PassiveReducersSwitchableReadModel(int value) {
    PassiveReducersSwitchableReadModel() {
        this(0);
    }
}

// Was active, now passive
@Reducer(isActive = false)
class PassiveReducersSwitchableReducer {
    public PassiveReducersSwitchableReadModel on(PassiveReducersValueRecorded event) {
        return new PassiveReducersSwitchableReadModel(event.value());
    }
}

@EventType
record PassiveReducersValueRecorded(int value) {}
```
