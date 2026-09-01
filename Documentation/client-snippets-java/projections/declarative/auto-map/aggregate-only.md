```java title="Aggregating an event does not map its other properties"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "decl-agg-arrangement-set")
record DeclAggArrangementSet(String location) {}

@EventType(id = "decl-agg-candidate-submitted")
record DeclAggCandidateSubmitted(String name, String location) {}

class DeclAggAssignmentSummary {
    public String location = "";
    public int candidateCount = 0;
}

class DeclAggAssignmentProjection implements IProjectionFor<DeclAggAssignmentSummary> {
    @Override
    public void define(IProjectionBuilderFor<DeclAggAssignmentSummary> builder) {
        builder
            .from(DeclAggArrangementSet.class)
            .from(DeclAggCandidateSubmitted.class, fb -> {
                fb.count("candidateCount");
                return null; // Java lambda returning Unit
            });
    }
}
```
