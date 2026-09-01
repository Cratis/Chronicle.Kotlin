```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon

@EventType
data class GeospatialAssetLocationUpdated(val location: Point)

@EventType
data class GeospatialRouteCreated(val path: LineString)

@EventType
data class GeospatialZoneEstablished(val boundaries: Polygon)

data class GeospatialAssetReadModel(val id: String, val location: Point)
data class GeospatialRouteReadModel(val id: String, val path: LineString)
data class GeospatialZoneReadModel(val id: String, val boundaries: Polygon)
```
