// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison;

/** Field-only Java fixtures, including historical constructor shapes. */
public final class ComparisonPojo {
    private String name = "field-only";
    private int count = 4;
    private transient String ignored = "not-on-wire";

    public ComparisonPojo() {}

    /** A historical payload requiring access to a private no-argument constructor. */
    public static final class PrivateConstructor {
        private String name;

        private PrivateConstructor() {}
    }

    /** A historical payload for which Gson allocates without calling a constructor. */
    public static final class NoDefaultConstructor {
        private String name;

        public NoDefaultConstructor(String name) {
            this.name = name;
        }
    }
}
