```java title="Aggregating an event does not map its other properties"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "agg-only-arrangement-set")
record AggOnlyArrangementSet(String location) {}

@EventType(id = "agg-only-candidate-submitted")
record AggOnlyCandidateSubmitted(String name, String location) {}

@ReadModel
@FromEvent(eventType = AggOnlyArrangementSet.class)
record AggOnlyAssignmentSummary(
    // AggOnlyCandidateSubmitted is subscribed only to be counted, so its identically named
    // location is not auto-mapped over the value sourced from AggOnlyArrangementSet.
    @SetFrom(propertyPath = "location", eventType = AggOnlyArrangementSet.class)
    String location,

    @Count(eventType = AggOnlyCandidateSubmitted.class)
    int candidateCount
) {}
```
