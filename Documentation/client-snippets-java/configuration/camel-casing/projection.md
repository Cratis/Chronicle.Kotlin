```java
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.java.ProjectionBuilderJavaBridge;

// Java field names are already camelCase, and AutoMap matches read model properties to event
// properties of the same name — firstName, lastName, emailAddress, registrationDate — so no
// naming policy configuration is needed to have them come out as camelCase.
class CamelCasingUserProjection implements IProjectionFor<CamelCasingUserReadModel> {
    @Override
    public void define(IProjectionBuilderFor<CamelCasingUserReadModel> builder) {
        ProjectionBuilderJavaBridge.from(builder, CamelCasingUserRegistered.class);
    }
}
```
