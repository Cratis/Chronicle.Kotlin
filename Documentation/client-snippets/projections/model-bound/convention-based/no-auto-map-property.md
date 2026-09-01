```kotlin title="Exclude a single property from convention mapping"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.NoAutoMap
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class NoAutoMapWorkArrangementSet(val location: String, val workMode: Int)

@EventType
data class NoAutoMapCandidateSubmitted(val name: String, val location: String)

@ReadModel
@FromEvent(NoAutoMapWorkArrangementSet::class)
data class NoAutoMapAssignmentSummary(
    // location is sourced only from NoAutoMapWorkArrangementSet. NoAutoMapCandidateSubmitted is
    // value-mapped (for candidateName) and also carries a location; @NoAutoMap stops that location
    // from being auto-mapped over the explicit value, while every other property keeps mapping.
    @SetFrom("location", NoAutoMapWorkArrangementSet::class)
    @NoAutoMap
    val location: String = "",

    @SetFrom("name", NoAutoMapCandidateSubmitted::class)
    val candidateName: String = ""
)
```
