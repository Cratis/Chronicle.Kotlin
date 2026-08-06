// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels;

import java.util.ArrayList;
import java.util.List;

/**
 * A read model reactor written in Java, exercising every handler shape from the Java side: a plain
 * instance handler, one that also takes the {@link ReadModelChangeset}, and a removal handler.
 *
 * <p>Java parameters carry no nullability, so {@code removed} deliberately declares a plain
 * {@link JavaEmployeeProfile} — the rule that a Kotlin removal handler must be nullable cannot
 * apply here, and registering this class must not trip over it.
 */
public class JavaReadModelReactor implements IReadModelReactor {

    public final List<String> calls = new ArrayList<>();

    public void added(JavaEmployeeProfile employee) {
        calls.add("added:" + employee.name);
    }

    public void modified(JavaEmployeeProfile employee, ReadModelChangeset<JavaEmployeeProfile> changeset) {
        calls.add("modified:" + changeset.getModelKey());
    }

    public void removed(JavaEmployeeProfile employee) {
        calls.add("removed:" + employee);
    }
}
