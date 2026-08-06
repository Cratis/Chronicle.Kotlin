// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels;

/**
 * A read model written in Java, so the reactor fixtures below react to a Java type end to end.
 */
public class JavaEmployeeProfile {
    public String name;

    public JavaEmployeeProfile() {
    }

    public JavaEmployeeProfile(String name) {
        this.name = name;
    }
}
