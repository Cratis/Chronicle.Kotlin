```kotlin
import io.cratis.fundamentals.geospatial.LinearRing
import io.cratis.fundamentals.geospatial.LineString
import io.cratis.fundamentals.geospatial.Point
import io.cratis.fundamentals.geospatial.Polygon

object GeospatialTypeExamples {
    fun createPoint(): Point = Point(longitude = 10.456, latitude = 42.123)

    fun createPath(): LineString = LineString(
        Point(10.456, 42.123),
        Point(11.789, 43.456)
    )

    fun createBoundary(): Polygon = Polygon(
        shell = LinearRing(
            Point(0.0, 0.0),
            Point(10.0, 0.0),
            Point(10.0, 10.0),
            Point(0.0, 10.0),
            Point(0.0, 0.0)
        ),
        holes = emptyList()
    )
}
```
