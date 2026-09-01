```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.Projection

@EventType
data class GeospatialProjAssetLocationUpdated(val location: Point)

data class GeospatialProjAssetReadModel(val id: String = "", val location: Point? = null)

// AutoMap picks up the Point-typed property automatically - no manual mapping needed
@Projection
class GeospatialProjAssetProjection : IProjectionFor<GeospatialProjAssetReadModel> {
    override fun define(builder: IProjectionBuilderFor<GeospatialProjAssetReadModel>) {
        builder.from(GeospatialProjAssetLocationUpdated::class)
    }
}
```
