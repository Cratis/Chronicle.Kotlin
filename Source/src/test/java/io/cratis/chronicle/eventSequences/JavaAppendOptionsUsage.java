// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences;

import io.cratis.chronicle.auditing.Causation;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import java.util.List;
import java.util.UUID;

/**
 * Java construction of {@link AppendOptions}, exercised from Kotlin specs.
 *
 * Kotlin default arguments do not exist for Java, so adding a property to the data class silently
 * breaks every Java caller using a positional constructor. These usages fail to compile if that
 * happens again.
 */
public final class JavaAppendOptionsUsage {

    private JavaAppendOptionsUsage() {
    }

    /** The no-argument form. */
    public static AppendOptions empty() {
        return new AppendOptions();
    }

    /** The two-argument positional form Java callers already compile against. */
    public static AppendOptions withCorrelationAndScope(UUID correlationId, ConcurrencyScope scope) {
        return new AppendOptions(correlationId, scope);
    }

    /** The builder, which is what Java should use for anything beyond the first argument or two. */
    public static AppendOptions viaBuilder(String subject, String tag) {
        return new AppendOptionsBuilder().subject(subject).tag(tag).build();
    }

    /** Overriding the ambient causation chain from Java. */
    public static AppendOptions withCausation(Causation causation) {
        return new AppendOptionsBuilder().causation(causation).build();
    }

    /** An event that names both its own event source and the chain it belongs to. */
    public static EventForEventSourceId eventWithCausation(
            String eventSourceId,
            Object event,
            List<Causation> causation) {
        return new EventForEventSourceId(
            eventSourceId, event, null, null, null, List.of(), null, null, causation);
    }
}
