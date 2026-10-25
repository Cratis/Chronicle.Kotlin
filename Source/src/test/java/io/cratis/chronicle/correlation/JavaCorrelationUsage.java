// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.correlation;

import java.util.UUID;

/** Compiled Java calls into the correlation manager, executed by the Kotlin specifications. */
public final class JavaCorrelationUsage {
    private JavaCorrelationUsage() {
    }

    public static CorrelationIdManager singleton() {
        return CorrelationIdManagerKt.getCorrelationIdManager();
    }

    public static UUID roundTrip(CorrelationIdManager manager, UUID id) {
        manager.set(id);
        var read = manager.getCurrent();
        manager.clear();
        return read;
    }

    public static UUID current(CorrelationIdManager manager) {
        return manager.getCurrent();
    }

    public static CorrelationIdManager create() {
        return new CorrelationIdManager();
    }
}
