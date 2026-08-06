// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.geospatial;

import io.cratis.chronicle.json.ChronicleGson;
import java.util.List;

/**
 * Java construction and serialization of the geospatial types, exercised from Kotlin specs.
 *
 * Kotlin default arguments and vararg-versus-list overloads do not carry over to Java, so a change
 * to any of these constructors silently breaks every Java caller. These usages fail to compile if
 * that happens.
 */
public final class JavaGeospatialUsage {

    private JavaGeospatialUsage() {
    }

    /** Java constructs a point positionally: longitude first, then latitude. */
    public static Point point(double longitude, double latitude) {
        return new Point(longitude, latitude);
    }

    /** Java constructs a line string from a list. */
    public static LineString lineStringFromList(List<Point> coordinates) {
        return new LineString(coordinates);
    }

    /** Java constructs a line string from loose points, without building a list first. */
    public static LineString lineStringFromPoints(Point first, Point second) {
        return new LineString(first, second);
    }

    /** Java constructs a solid polygon without naming the holes, which requires {@code @JvmOverloads}. */
    public static Polygon polygonWithoutHoles(LinearRing shell) {
        return new Polygon(shell);
    }

    /** Java constructs a polygon with holes. */
    public static Polygon polygonWithHoles(LinearRing shell, List<LinearRing> holes) {
        return new Polygon(shell, holes);
    }

    /** Java constructs a linear ring from loose points, without building a list first. */
    public static LinearRing ring(Point a, Point b, Point c, Point closing) {
        return new LinearRing(a, b, c, closing);
    }

    /** Java serializes a value through the client's own Gson, which is how event content is written. */
    public static String toJson(Object value) {
        return ChronicleGson.chronicleGson.toJson(value);
    }

    /** Java deserializes a value through the client's own Gson. */
    public static <T> T fromJson(String json, Class<T> type) {
        return ChronicleGson.chronicleGson.fromJson(json, type);
    }
}
