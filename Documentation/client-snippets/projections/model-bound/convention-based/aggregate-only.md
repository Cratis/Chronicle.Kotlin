```kotlin title="Aggregating an event does not map its other properties"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class AggOnlyArrangementSet(val location: String)

@EventType
data class AggOnlyCandidateSubmitted(val name: String, val location: String)

@ReadModel
@FromEvent(AggOnlyArrangementSet::class)
data class AggOnlyAssignmentSummary(
    // AggOnlyCandidateSubmitted is subscribed only to be counted, so its identically named
    // location is not auto-mapped over the value sourced from AggOnlyArrangementSet.
    @SetFrom("location", AggOnlyArrangementSet::class)
    val location: String = "",

    @Count(AggOnlyCandidateSubmitted::class)
    val candidateCount: Int = 0
)
```
