```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.geospatial.Point;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.Projection;

import io.cratis.chronicle.java.ProjectionBuilderJavaBridge;

@EventType
record GeospatialProjAssetLocationUpdated(Point location) {}

record GeospatialProjAssetReadModel(String id, Point location) {}

// AutoMap picks up the Point-typed property automatically - no manual mapping needed
@Projection
class GeospatialProjAssetProjection implements IProjectionFor<GeospatialProjAssetReadModel> {
    @Override
    public void define(IProjectionBuilderFor<GeospatialProjAssetReadModel> builder) {
        ProjectionBuilderJavaBridge.from(builder, GeospatialProjAssetLocationUpdated.class);
    }
}
```
