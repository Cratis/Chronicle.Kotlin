```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.java.ProjectionsServiceJavaBridge;
import io.cratis.chronicle.projections.ProjectionDeclarationError;
import io.cratis.chronicle.projections.ProjectionQueryResult;

class ErrorHandlingOrdersQuery {
    // A declaration that fails to parse is not an exception — it comes back as a result you branch on.
    void queryOrders(EventStore store) {
        ProjectionQueryResult result = ProjectionsServiceJavaBridge.query(
            store.getProjections(),
            "projection Orders\n  from OrderPlaced"
        );

        if (result instanceof ProjectionQueryResult.Projected projected) {
            for (String entry : projected.getEntries()) {
                System.out.println(entry);
            }
        } else if (result instanceof ProjectionQueryResult.Invalid invalid) {
            for (ProjectionDeclarationError error : invalid.getErrors()) {
                System.out.println(error);
            }
        }
    }
}
```
