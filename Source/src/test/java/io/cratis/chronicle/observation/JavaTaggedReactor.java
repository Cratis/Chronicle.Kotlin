// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

/**
 * A Java reactor using the tagging and filtering annotations, including repeating them.
 *
 * Kotlin's {@code @Repeatable} only works from Java when it emits a JVM container annotation, so
 * this pins that down rather than assuming it.
 */
@Reactor
@Tag({"analytics", "reporting"})
@Tag("owned-by-platform")
@FilterEventsByTag("critical")
@FilterEventsByTag("production")
@EventSourceType("Patient")
@EventStreamType("Onboarding")
public class JavaTaggedReactor {

    public void bookAdded(JavaBookAdded event) {
    }
}
