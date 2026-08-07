// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.java.BlockingReactorMiddleware;
import java.util.ArrayList;
import java.util.List;

/**
 * A reactor middleware written in Java, exercised from Kotlin specs.
 *
 * {@link io.cratis.chronicle.observation.IReactorMiddleware} is suspending and therefore not
 * implementable from Java at all. This fixture fails to compile if {@link BlockingReactorMiddleware}
 * ever grows a shape Java cannot express.
 */
public class JavaReactorMiddleware implements BlockingReactorMiddleware {

    private final List<String> log = new ArrayList<>();

    /** What this middleware saw, in order. */
    public List<String> getLog() {
        return log;
    }

    @Override
    public void beforeInvoke(EventContext context, Object event) {
        log.add("before:" + event.getClass().getSimpleName());
    }

    @Override
    public void afterInvoke(EventContext context, Object event) {
        log.add("after:" + event.getClass().getSimpleName());
    }
}
