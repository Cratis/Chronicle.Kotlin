// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections;

import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.readModels.ReadModel;

/**
 * A fluent projection written in Java, the way a reader would write one.
 *
 * Every configuration callback here used to be a Kotlin function type, which Java sees as a
 * {@code Function1} returning {@code Unit} - so each lambda had to end with a {@code return null;}
 * that meant nothing. These take a {@link java.util.function.Consumer} instead. Nothing here runs;
 * compiling it is the assertion.
 */
public final class JavaFluentProjectionUsage {

    private JavaFluentProjectionUsage() {
    }

    /** The event being projected from. */
    @EventType
    public record AccountOpened(String name, double initialBalance) {
    }

    /** The event that closes the account again. */
    @EventType
    public record AccountClosed(String reason) {
    }

    /** What the projection builds. */
    @ReadModel
    public static class AccountInfo {
        public String id = "";
        public String name = "";
        public double balance = 0;
        public String lastTouched = "";
    }

    /** Mapping per event, across every event, and a removal - none of them ending in `return null`. */
    public static class AccountProjection implements IProjectionFor<AccountInfo> {
        @Override
        public void define(IProjectionBuilderFor<AccountInfo> builder) {
            builder
                .from(AccountOpened.class, from -> {
                    from.set("name").toProperty("name");
                    from.set("balance").toProperty("initialBalance");
                })
                .fromEvery(every -> {
                    every.set("lastTouched").toEventContextProperty("occurred");
                })
                .removedWith(AccountClosed.class, key -> {
                    key.usingKey("reason");
                });
        }
    }
}
