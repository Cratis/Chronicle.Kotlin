```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.java.ProjectionsServiceJavaBridge;
import io.cratis.chronicle.projections.ProjectionDeclarationError;
import io.cratis.chronicle.projections.ProjectionQueryResult;

class BadQuery {
    // This declaration comes back as ProjectionQueryResult.Invalid:
    // OrderPlaced.value is a string, but OrderShipped.value is an int
    void queryBad(EventStore store) {
        ProjectionQueryResult result = ProjectionsServiceJavaBridge.query(
            store.getProjections(),
            "projection Bad\n"
                + "  from OrderPlaced   // value: string\n"
                + "  from OrderShipped  // value: int  -> incompatible types"
        );

        if (result instanceof ProjectionQueryResult.Invalid invalid) {
            for (ProjectionDeclarationError error : invalid.getErrors()) {
                System.out.println(error);
            }
        }
    }
}
```
