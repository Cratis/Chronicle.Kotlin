```java
import io.cratis.chronicle.geospatial.LinearRing;
import io.cratis.chronicle.geospatial.LineString;
import io.cratis.chronicle.geospatial.Point;
import io.cratis.chronicle.geospatial.Polygon;

import java.util.Collections;

class GeospatialTypeExamples {
    static Point createPoint() {
        return new Point(10.456, 42.123);
    }

    static LineString createPath() {
        return new LineString(
            new Point(10.456, 42.123),
            new Point(11.789, 43.456));
    }

    static Polygon createBoundary() {
        LinearRing shell = new LinearRing(
            new Point(0, 0),
            new Point(10, 0),
            new Point(10, 10),
            new Point(0, 10),
            new Point(0, 0));
        return new Polygon(shell, Collections.emptyList());
    }
}
```
