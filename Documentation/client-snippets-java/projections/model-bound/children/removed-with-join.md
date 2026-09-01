```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ChildrenFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWithJoin;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Collections;
import java.util.List;

@EventType
record MbChildrenRemovedFeatureActivated(String featureId, String name) {}

@EventType
record MbChildrenRemovedFeatureDeactivated(String featureId) {}

@ReadModel
@FromEvent(eventType = MbChildrenRemovedFeatureActivated.class)
class MbChildrenRemovedSubscription {
    @ChildrenFrom(eventType = MbChildrenRemovedFeatureActivated.class, key = "featureId", identifiedBy = "featureId")
    @RemovedWithJoin(eventType = MbChildrenRemovedFeatureDeactivated.class, key = "featureId")
    public List<MbChildrenRemovedFeature> features = Collections.emptyList();
}

class MbChildrenRemovedFeature {
    public String featureId = "";

    @SetFrom(propertyPath = "name", eventType = MbChildrenRemovedFeatureActivated.class)
    public String name = "";
}
```
