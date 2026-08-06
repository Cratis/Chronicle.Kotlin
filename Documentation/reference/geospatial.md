# Geospatial types

Chronicle recognizes a value as geospatial by its [GeoJSON](https://geojson.org)
shape. The client ships three types that serialize to that shape, so the sink
can index and query them geospatially. A hand-rolled coordinate class
serializes as an ordinary nested object and gets none of that.

They live in `io.cratis.chronicle.geospatial` and are usable on any event, read
model, or reducer state.

## Types

| Type | Constructor | GeoJSON `type` | Schema `format` |
| --- | --- | --- | --- |
| `Point` | `Point(longitude, latitude)` | `Point` | `point` |
| `LineString` | `LineString(coordinates)` | `LineString` | `linestring` |
| `Polygon` | `Polygon(shell, holes)` | `Polygon` | `polygon` |
| `LinearRing` | `LinearRing(coordinates)` | *(a polygon ring)* | — |

<!-- validate: skip -->

```kotlin
data class Point(val longitude: Double, val latitude: Double)

data class LineString(val coordinates: List<Point>)

data class LinearRing(val coordinates: List<Point>)

data class Polygon(
    val shell: LinearRing,
    val holes: List<LinearRing> = emptyList()
)
```

Longitude comes first, matching GeoJSON coordinate order — not the "latitude,
longitude" order used in everyday speech.

`LineString` and `LinearRing` also take loose points (`LineString(a, b, c)`),
which is what Java uses in place of a list literal. `Polygon` is
`@JvmOverloads`, so `new Polygon(shell)` works from Java for a polygon with no
holes.

## Using them on an event

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.geospatial.Point

@EventType
data class WarehouseInspected(
    val warehouseId: String = "",
    val inspectedAt: Point = Point(0.0, 0.0)
)
```

<!-- validate: declarations -->

```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.geospatial.Point;

@EventType
public record WarehouseInspected(String warehouseId, Point inspectedAt) {}
```

Appending is unchanged — the client serializes the point as GeoJSON on the way
out:

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.geospatial.Point

store.eventLog.append(
    eventSourceId = "warehouse-1",
    event = WarehouseInspected("warehouse-1", Point(10.75, 59.91))
)
```

The event content on the wire:

```json
{
  "warehouseId": "warehouse-1",
  "inspectedAt": { "type": "Point", "coordinates": [10.75, 59.91] }
}
```

## Building a polygon

A polygon's shell must be closed — its last point repeats its first — and needs
at least four points. Holes follow the same rule.

<!-- validate: body -->

```kotlin
import io.cratis.chronicle.geospatial.LinearRing
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon

val deliveryZone = Polygon(
    shell = LinearRing(
        Point(10.70, 59.90),
        Point(10.80, 59.90),
        Point(10.80, 59.95),
        Point(10.70, 59.90)
    )
)
```

GeoJSON stores the shell and the holes in one flat array of rings, with the
shell first. Reading a polygon back takes the first ring as the shell and the
rest as holes.

## Validation on read

Deserialization rejects geometries the sink would refuse, so a malformed value
fails at the client rather than deep in the kernel.

| Geometry | Rejected when |
| --- | --- |
| `Point` | `coordinates` is not `[longitude, latitude]` |
| `LineString` | fewer than 2 points |
| `Polygon` | it carries no rings at all |
| `LinearRing` | fewer than 4 points, or it does not close |
| all | the declared `type` is not the expected one |

Construction is not validated — only what comes off the wire is.

## Serializing yourself

`io.cratis.chronicle.json.ChronicleGson.chronicleGson` is the `Gson` instance
the client serializes event content and read model state with. Use it when you
need to produce the same JSON the client would.

<!-- validate: body -->

```kotlin
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.json.chronicleGson

val json = chronicleGson.toJson(Point(10.75, 59.91))
```

<!-- validate: body -->

```java
import io.cratis.chronicle.geospatial.Point;
import io.cratis.chronicle.json.ChronicleGson;

String json = ChronicleGson.chronicleGson.toJson(new Point(10.75, 59.91));
```
