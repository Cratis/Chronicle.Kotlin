```java title="Exclude a single property from convention mapping"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.NoAutoMap;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "no-auto-map-work-arrangement-set")
record NoAutoMapWorkArrangementSet(String location, int workMode) {}

@EventType(id = "no-auto-map-candidate-submitted")
record NoAutoMapCandidateSubmitted(String name, String location) {}

@ReadModel
@FromEvent(eventType = NoAutoMapWorkArrangementSet.class)
record NoAutoMapAssignmentSummary(
    // location is sourced only from NoAutoMapWorkArrangementSet. NoAutoMapCandidateSubmitted is
    // value-mapped (for candidateName) and also carries a location; @NoAutoMap stops that location
    // from being auto-mapped over the explicit value, while every other property keeps mapping.
    @SetFrom(propertyPath = "location", eventType = NoAutoMapWorkArrangementSet.class)
    @NoAutoMap
    String location,

    @SetFrom(propertyPath = "name", eventType = NoAutoMapCandidateSubmitted.class)
    String candidateName
) {}
```
