// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences;

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.java.AppendOptionsBuilder;

/** Java construction of {@link AppendOptions}, exercised from Kotlin specs. */
public final class JavaAppendOptionsUsage {
    private JavaAppendOptionsUsage() {
    }

    public static AppendOptions empty() {
        return new AppendOptions();
    }

    public static AppendOptions withScope(ConcurrencyScope scope) {
        return new AppendOptions(scope);
    }

    public static AppendOptions viaBuilder(String subject, String tag) {
        return new AppendOptionsBuilder().subject(subject).tag(tag).build();
    }
}
