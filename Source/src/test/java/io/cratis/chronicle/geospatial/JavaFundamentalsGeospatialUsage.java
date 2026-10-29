// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.geospatial;

import io.cratis.chronicle.json.ChronicleGson;
import io.cratis.fundamentals.geospatial.LineString;
import io.cratis.fundamentals.geospatial.LinearRing;
import io.cratis.fundamentals.geospatial.Point;
import io.cratis.fundamentals.geospatial.Polygon;
import java.util.List;

/** Java usage of the Fundamentals geospatial types with the client's serializer and the conversions. */
@SuppressWarnings("deprecation")
public final class JavaFundamentalsGeospatialUsage {

    private JavaFundamentalsGeospatialUsage() {
    }

    /** Constructs a point positionally. */
    public static Point point(double longitude, double latitude) {
        return new Point(longitude, latitude);
    }

    /** Constructs a line string from loose points. */
    public static LineString lineString(Point first, Point second) {
        return new LineString(first, second);
    }

    /** Constructs a solid polygon without naming the holes. */
    public static Polygon polygon(LinearRing shell) {
        return new Polygon(shell);
    }

    /** Constructs a polygon with holes. */
    public static Polygon polygon(LinearRing shell, List<LinearRing> holes) {
        return new Polygon(shell, holes);
    }

    /** Serializes through the client's Gson. */
    public static String toJson(Object value) {
        return ChronicleGson.chronicleGson.toJson(value);
    }

    /** Deserializes through the client's Gson. */
    public static <T> T fromJson(String json, Class<T> type) {
        return ChronicleGson.chronicleGson.fromJson(json, type);
    }

    /** Converts a legacy point to the Fundamentals point. */
    public static Point toFundamentals(io.cratis.chronicle.geospatial.Point legacy) {
        return legacy.toFundamentals();
    }

    /** Converts a Fundamentals polygon to the legacy polygon through the converting constructor. */
    public static io.cratis.chronicle.geospatial.Polygon toLegacy(Polygon value) {
        return new io.cratis.chronicle.geospatial.Polygon(value);
    }
}
