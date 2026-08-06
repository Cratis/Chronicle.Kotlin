// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

/**
 * A Java reactor pointed at a sequence by the standalone {@code @EventSequence} alone.
 *
 * <p>The annotation is written in Java's shorthand form, without naming an element. That only
 * compiles while the Kotlin annotation's parameter is called {@code value}, which is the whole
 * point of this fixture.
 */
@Reactor
@EventSequence("outbox")
public class JavaEventSequenceReactor {

    public void bookAdded(JavaBookAdded event) {
    }
}
