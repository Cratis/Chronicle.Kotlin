```kotlin title="Aggregating an event does not map its other properties"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "decl-agg-arrangement-set")
data class DeclAggArrangementSet(val location: String)

@EventType(id = "decl-agg-candidate-submitted")
data class DeclAggCandidateSubmitted(val name: String, val location: String)

data class DeclAggAssignmentSummary(val location: String = "", val candidateCount: Int = 0)

class DeclAggAssignmentProjection : IProjectionFor<DeclAggAssignmentSummary> {
    override fun define(builder: IProjectionBuilderFor<DeclAggAssignmentSummary>) {
        builder
            .from(DeclAggArrangementSet::class)
            .from(DeclAggCandidateSubmitted::class) {
                it.count(DeclAggAssignmentSummary::candidateCount)
            }
    }
}
```
