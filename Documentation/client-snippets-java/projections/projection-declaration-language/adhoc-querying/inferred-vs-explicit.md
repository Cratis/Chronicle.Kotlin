```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.java.ProjectionsServiceJavaBridge;
import io.cratis.chronicle.projections.ProjectionQueryResult;

class InferredVsExplicitOrdersQuery {
    void compareInferredAndExplicit(EventStore store) {
        // Inferred — schema derived from OrderPlaced and OrderShipped event properties
        ProjectionQueryResult inferred = ProjectionsServiceJavaBridge.query(
            store.getProjections(),
            "projection Orders\n  from OrderPlaced\n  from OrderShipped"
        );

        // Explicit — schema comes from the registered 'PdlOrderReadModel' type
        ProjectionQueryResult explicitResult = ProjectionsServiceJavaBridge.query(
            store.getProjections(),
            "projection Orders => PdlOrderReadModel\n  from OrderPlaced\n  from OrderShipped"
        );
    }
}
```
