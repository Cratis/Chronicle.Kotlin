```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.geospatial.LineString;
import io.cratis.chronicle.geospatial.Point;
import io.cratis.chronicle.geospatial.Polygon;

@EventType
record GeospatialAssetLocationUpdated(Point location) {}

@EventType
record GeospatialRouteCreated(LineString path) {}

@EventType
record GeospatialZoneEstablished(Polygon boundaries) {}

record GeospatialAssetReadModel(String id, Point location) {}
record GeospatialRouteReadModel(String id, LineString path) {}
record GeospatialZoneReadModel(String id, Polygon boundaries) {}
```
