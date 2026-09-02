// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences;

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;
import io.cratis.chronicle.java.ConcurrencyScopeBuilderJavaBridge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrencyScopeBuilderJavaTests {
    @Test
    void sequenceNumberCannotFollowNoMatchingEventExpectation() {
        var builder = new ConcurrencyScopeBuilder().expectsNoMatchingEvent();

        assertThrows(
            IllegalStateException.class,
            () -> ConcurrencyScopeBuilderJavaBridge.withSequenceNumber(builder, 4));
    }

    @Test
    void noMatchingEventExpectationCannotFollowSequenceNumber() {
        var builder = ConcurrencyScopeBuilderJavaBridge.withSequenceNumber(new ConcurrencyScopeBuilder(), 4);

        assertThrows(IllegalStateException.class, builder::expectsNoMatchingEvent);
    }
}
