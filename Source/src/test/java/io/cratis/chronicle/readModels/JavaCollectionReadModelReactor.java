// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels;

import java.util.ArrayList;
import java.util.List;

/**
 * A read model reactor written in Java whose handler takes a {@link List} of read models, so the
 * element type is what decides which read model is watched.
 */
public class JavaCollectionReadModelReactor implements IReadModelReactor {

    public final List<Integer> batchSizes = new ArrayList<>();

    public void added(List<JavaEmployeeProfile> employees) {
        batchSizes.add(employees.size());
    }
}
